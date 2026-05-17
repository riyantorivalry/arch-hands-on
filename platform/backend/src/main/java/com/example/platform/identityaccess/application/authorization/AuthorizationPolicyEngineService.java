package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationPolicyEngineService {

    private final List<AuthorizationPolicyEngine> engines;
    private final String mode;

    public AuthorizationPolicyEngineService(
            List<AuthorizationPolicyEngine> engines,
            @Value("${platform.feature.authorization.engine:in-code}") String mode
    ) {
        this.engines = engines;
        this.mode = normalize(mode);
    }

    public String mode() {
        return mode;
    }

    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        return engine(mode).decide(membership, request);
    }

    public AuthorizationDecision decide(String engine, MembershipEntity membership, AuthorizationDecisionRequest request) {
        return engine(engine).decide(membership, request);
    }

    public AuthorizationPolicyEngine engine(String engine) {
        String normalized = normalize(engine);
        return engines.stream()
                .filter(candidate -> candidate.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported authorization policy engine: " + engine));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "in-code";
        }
        return value.trim().toLowerCase();
    }
}
