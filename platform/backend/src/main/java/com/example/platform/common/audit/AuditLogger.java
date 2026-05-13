package com.example.platform.common.audit;

import com.example.platform.common.web.RequestContext;
import com.example.platform.common.web.RequestContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogger.class);

    public void logWrite(String module, String action, String resourceType, String resourceId, String outcome) {
        RequestContext context = RequestContexts.current();
        LOGGER.info(
                "audit module={} action={} resourceType={} resourceId={} outcome={} correlationId={} tenantId={} workspaceId={} userId={}",
                module,
                action,
                resourceType,
                resourceId,
                outcome,
                context.correlationId(),
                context.tenantId(),
                context.workspaceId(),
                context.userId()
        );
    }
}
