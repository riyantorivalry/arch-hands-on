package com.example.platform.common.infrastructure.observability;

import com.example.platform.documents.infrastructure.DocumentRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import com.example.platform.messaging.infrastructure.MessageRepository;
import com.example.platform.tasks.infrastructure.TaskRepository;
import com.example.platform.tenantmanagement.infrastructure.TenantRepository;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserFacingOverviewMetrics {

    private static final Logger logger = LoggerFactory.getLogger(UserFacingOverviewMetrics.class);

    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;

    private final AtomicLong tenants = new AtomicLong();
    private final AtomicLong workspaces = new AtomicLong();
    private final AtomicLong users = new AtomicLong();
    private final AtomicLong tasks = new AtomicLong();
    private final AtomicLong documents = new AtomicLong();
    private final AtomicLong messages = new AtomicLong();

    public UserFacingOverviewMetrics(
            MeterRegistry meterRegistry,
            TenantRepository tenantRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            DocumentRepository documentRepository,
            MessageRepository messageRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.messageRepository = messageRepository;

        registerGauge(meterRegistry, "tenants", tenants);
        registerGauge(meterRegistry, "workspaces", workspaces);
        registerGauge(meterRegistry, "users", users);
        registerGauge(meterRegistry, "tasks", tasks);
        registerGauge(meterRegistry, "documents", documents);
        registerGauge(meterRegistry, "messages", messages);
    }

    @PostConstruct
    public void initialize() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${platform.observability.user-facing-metrics-refresh:60000}")
    public void refresh() {
        tenants.set(countSafely("tenants", tenantRepository::count));
        workspaces.set(countSafely("workspaces", workspaceRepository::count));
        users.set(countSafely("users", userRepository::count));
        tasks.set(countSafely("tasks", taskRepository::count));
        documents.set(countSafely("documents", documentRepository::count));
        messages.set(countSafely("messages", messageRepository::count));
    }

    private void registerGauge(MeterRegistry meterRegistry, String entity, AtomicLong value) {
        Gauge.builder("platform.user_facing.total", value, AtomicLong::get)
                .description("Cached total count for product-facing platform entities")
                .tag("entity", entity)
                .register(meterRegistry);
    }

    private long countSafely(String entity, Supplier<Long> countSupplier) {
        try {
            return countSupplier.get();
        } catch (RuntimeException exception) {
            logger.debug("Could not refresh {} user-facing count: {}", entity, exception.getMessage());
            return 0L;
        }
    }
}
