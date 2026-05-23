package com.example.platform.common.infrastructure.observability;

import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "platform.observability.profiling", name = "enabled", havingValue = "true")
public class PyroscopeProfilingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PyroscopeProfilingConfiguration.class);

    @Value("${platform.observability.profiling.application-name:${spring.application.name}}")
    private String applicationName;

    @Value("${platform.observability.profiling.server-address:http://localhost:4040}")
    private String serverAddress;

    @Value("${platform.observability.profiling.environment:local}")
    private String environment;

    @Value("${platform.observability.profiling.region:local}")
    private String region;

    @Value("${platform.observability.profiling.alloc:}")
    private String allocationThreshold;

    @Value("${platform.observability.profiling.lock:10ms}")
    private String lockThreshold;

    @PostConstruct
    public void startProfiler() {
        try {
            Config.Builder builder = new Config.Builder()
                    .setApplicationName(applicationName)
                    .setServerAddress(serverAddress)
                    .setProfilingEvent(EventType.ITIMER)
                    .setFormat(Format.JFR)
                    .setLabels(staticLabels());

            if (StringUtils.hasText(allocationThreshold)) {
                builder.setProfilingAlloc(allocationThreshold);
            }
            if (StringUtils.hasText(lockThreshold)) {
                builder.setProfilingLock(lockThreshold);
            }

            PyroscopeAgent.start(builder.build());
            logger.info("Pyroscope profiler started - application: {}, server: {}", applicationName, serverAddress);
        } catch (RuntimeException | Error ex) {
            logger.warn("Pyroscope profiler could not start - server: {}, error: {}", serverAddress, ex.getMessage());
        }
    }

    private Map<String, String> staticLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("service_name", applicationName);
        labels.put("environment", environment);
        labels.put("region", region);
        return labels;
    }
}
