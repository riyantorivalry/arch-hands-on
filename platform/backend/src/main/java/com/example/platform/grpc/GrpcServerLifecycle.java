package com.example.platform.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final List<BindableService> services;
    private final boolean enabled;
    private final int port;
    private Server server;
    private boolean running;

    public GrpcServerLifecycle(
            List<BindableService> services,
            @Value("${platform.grpc.enabled:false}") boolean enabled,
            @Value("${platform.grpc.port:9090}") int port
    ) {
        this.services = services;
        this.enabled = enabled;
        this.port = port;
    }

    @Override
    public void start() {
        if (!enabled || running) {
            return;
        }

        NettyServerBuilder builder = NettyServerBuilder.forPort(port);
        services.forEach(builder::addService);
        try {
            server = builder.build().start();
            running = true;
            LOGGER.info("gRPC server started on port {} with {} service(s)", port, services.size());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start gRPC server on port " + port, exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
