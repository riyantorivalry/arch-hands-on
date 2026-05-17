package com.example.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.platform.grpc.GrpcServerLifecycle;
import com.example.platform.grpc.PlatformQueryGrpcService;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GrpcServiceInterfaceTests {

    @Autowired
    private PlatformQueryGrpcService platformQueryGrpcService;

    @Autowired
    private GrpcServerLifecycle grpcServerLifecycle;

    @Test
    void platformQueryServiceExposesInternalGrpcMethodsButServerIsDisabledByDefault() {
        ServerServiceDefinition definition = platformQueryGrpcService.bindService();
        Set<String> methods = definition.getMethods().stream()
                .map(ServerMethodDefinition::getMethodDescriptor)
                .map(descriptor -> descriptor.getFullMethodName())
                .collect(Collectors.toSet());

        assertThat(definition.getServiceDescriptor().getName()).isEqualTo("platform.v1.PlatformQueryService");
        assertThat(methods).containsExactlyInAnyOrder(
                "platform.v1.PlatformQueryService/ListTasks",
                "platform.v1.PlatformQueryService/ListDocuments"
        );
        assertThat(grpcServerLifecycle.isRunning()).isFalse();
    }
}
