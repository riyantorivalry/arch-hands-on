package com.example.platform.identityaccess.application;

import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class IdentityAccessFacade {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public IdentityAccessFacade(UserRepository userRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    public CurrentActorView getCurrentActor(String workspaceId, String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found for workspace " + workspaceId));
        return new CurrentActorView(
                user.getUserId(),
                user.getDisplayName(),
                user.getEmail(),
                membership.getWorkspaceId(),
                membership.getTenantId(),
                membership.getRole()
        );
    }

    public record CurrentActorView(
            String userId,
            String displayName,
            String email,
            String workspaceId,
            String tenantId,
            String workspaceRole
    ) {
    }
}
