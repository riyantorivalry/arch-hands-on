package com.example.platform.common.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseQueryMetricsListener implements QueryExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseQueryMetricsListener.class);
    private static final int MAX_SQL_PREVIEW_LENGTH = 220;

    private final MeterRegistry meterRegistry;
    private final long slowQueryThresholdMs;
    private final Tracer tracer;
    private final ThreadLocal<Deque<QuerySpanContext>> querySpans = ThreadLocal.withInitial(ArrayDeque::new);

    public DatabaseQueryMetricsListener(MeterRegistry meterRegistry, long slowQueryThresholdMs, Tracer tracer) {
        this.meterRegistry = meterRegistry;
        this.slowQueryThresholdMs = slowQueryThresholdMs;
        this.tracer = tracer;
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (tracer == null) {
            return;
        }

        String operation = queryOperation(queryInfoList);
        String datasource = safeTag(execInfo.getDataSourceName(), "unknown");
        String statementType = statementType(execInfo);
        Span span = tracer.nextSpan()
                .name("db." + operation)
                .tag("db.system", "jdbc")
                .tag("db.operation", operation)
                .tag("db.statement", sqlPreview(queryInfoList))
                .tag("db.datasource", datasource)
                .tag("db.statement_type", statementType)
                .tag("db.batch", String.valueOf(execInfo.isBatch()));

        span.start();
        Tracer.SpanInScope scope = tracer.withSpan(span);
        querySpans.get().push(new QuerySpanContext(span, scope));
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        long elapsedMs = Math.max(execInfo.getElapsedTime(), 0L);
        String operation = queryOperation(queryInfoList);
        String datasource = safeTag(execInfo.getDataSourceName(), "unknown");
        String statementType = statementType(execInfo);
        String success = String.valueOf(execInfo.getThrowable() == null);

        Timer.builder("database.query.time")
                .description("JDBC query execution time")
                .tag("datasource", datasource)
                .tag("operation", operation)
                .tag("statement_type", statementType)
                .tag("batch", String.valueOf(execInfo.isBatch()))
                .tag("success", success)
                .register(meterRegistry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);

        if (elapsedMs >= slowQueryThresholdMs || execInfo.getThrowable() != null) {
            logSlowOrFailedQuery(execInfo, queryInfoList, elapsedMs, operation, datasource);
        }

        finishQuerySpan(execInfo, queryInfoList, elapsedMs, operation, datasource, statementType, success);
    }

    private void finishQuerySpan(
            ExecutionInfo execInfo,
            List<QueryInfo> queryInfoList,
            long elapsedMs,
            String operation,
            String datasource,
            String statementType,
            String success
    ) {
        if (tracer == null) {
            return;
        }

        QuerySpanContext spanContext = pollQuerySpan();
        Span span;
        Tracer.SpanInScope scope;
        if (spanContext == null) {
            span = tracer.nextSpan().name("db." + operation).start();
            scope = tracer.withSpan(span);
        } else {
            span = spanContext.span();
            scope = spanContext.scope();
        }

        try {
            span.tag("db.system", "jdbc");
            span.tag("db.operation", operation);
            span.tag("db.statement", sqlPreview(queryInfoList));
            span.tag("db.datasource", datasource);
            span.tag("db.statement_type", statementType);
            span.tag("db.batch", String.valueOf(execInfo.isBatch()));
            span.tag("db.success", success);
            span.tag("platform.duration_ms", String.valueOf(elapsedMs));
            if (execInfo.getThrowable() != null) {
                span.error(execInfo.getThrowable());
            }
        } finally {
            scope.close();
            span.end();
        }
    }

    private void logSlowOrFailedQuery(
            ExecutionInfo execInfo,
            List<QueryInfo> queryInfoList,
            long elapsedMs,
            String operation,
            String datasource
    ) {
        String sqlPreview = sqlPreview(queryInfoList);
        Throwable throwable = execInfo.getThrowable();

        if (throwable == null) {
            logger.warn(
                    "Slow database query - datasource: {}, operation: {}, duration: {}ms, batch: {}, sql: {}",
                    datasource,
                    operation,
                    elapsedMs,
                    execInfo.isBatch(),
                    sqlPreview
            );
            return;
        }

        logger.warn(
                "Failed database query - datasource: {}, operation: {}, duration: {}ms, batch: {}, error: {}, sql: {}",
                datasource,
                operation,
                elapsedMs,
                execInfo.isBatch(),
                throwable.getMessage(),
                sqlPreview
        );
    }

    private String queryOperation(List<QueryInfo> queryInfoList) {
        String query = firstQuery(queryInfoList);
        if (query == null) {
            return "unknown";
        }

        String normalized = query.stripLeading().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "unknown";
        }

        int end = 0;
        while (end < normalized.length() && Character.isLetter(normalized.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return "other";
        }

        String operation = normalized.substring(0, end);
        return switch (operation) {
            case "SELECT", "INSERT", "UPDATE", "DELETE", "MERGE", "CALL", "CREATE", "ALTER", "DROP" ->
                    operation.toLowerCase(Locale.ROOT);
            default -> "other";
        };
    }

    private String sqlPreview(List<QueryInfo> queryInfoList) {
        String query = firstQuery(queryInfoList);
        if (query == null || query.isBlank()) {
            return "<empty>";
        }

        String compact = query.replaceAll("\\s+", " ").trim();
        if (compact.length() <= MAX_SQL_PREVIEW_LENGTH) {
            return compact;
        }
        return compact.substring(0, MAX_SQL_PREVIEW_LENGTH) + "...";
    }

    private String firstQuery(List<QueryInfo> queryInfoList) {
        if (queryInfoList == null || queryInfoList.isEmpty()) {
            return null;
        }
        return queryInfoList.get(0).getQuery();
    }

    private String statementType(ExecutionInfo execInfo) {
        return execInfo.getStatementType() == null
                ? "unknown"
                : execInfo.getStatementType().name().toLowerCase(Locale.ROOT);
    }

    private String safeTag(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private QuerySpanContext pollQuerySpan() {
        Deque<QuerySpanContext> spans = querySpans.get();
        QuerySpanContext spanContext = spans.poll();
        if (spans.isEmpty()) {
            querySpans.remove();
        }
        return spanContext;
    }

    private record QuerySpanContext(Span span, Tracer.SpanInScope scope) {
    }
}
