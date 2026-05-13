package com.example.platform.identityaccess.application;

import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import org.springframework.transaction.annotation.Transactional;
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
                membership.getRole().name()
        );
    }

    @Transactional
    public MembershipAssignmentView assignMembership(
            String actorUserId,
            String workspaceId,
            String userId,
            String email,
            String displayName,
            String role
    ) {
        var actorMembership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found for actor in workspace " + workspaceId));
        if (actorMembership.getStatus() != com.example.platform.identityaccess.domain.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Actor membership is not active");
        }
        if (actorMembership.getRole() != com.example.platform.identityaccess.domain.MembershipRole.OWNER
                && actorMembership.getRole() != com.example.platform.identityaccess.domain.MembershipRole.ADMIN) {
            throw new com.example.platform.common.web.AuthorizationDeniedException("Workspace manager role is required");
        }

        var user = userRepository.findById(userId)
                .orElseGet(() -> userRepository.save(
                        new com.example.platform.identityaccess.domain.UserEntity(
                                userId,
                                email,
                                displayName,
                                com.example.platform.identityaccess.domain.UserStatus.ACTIVE
                        )
                ));
        var assignedRole = com.example.platform.identityaccess.domain.MembershipRole.valueOf(role);
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseGet(() -> membershipRepository.save(
                        new com.example.platform.identityaccess.domain.MembershipEntity(
                                actorMembership.getTenantId(),
                                workspaceId,
                                user.getUserId(),
                                assignedRole,
                                com.example.platform.identityaccess.domain.MembershipStatus.ACTIVE
                        )
                ));
        return new MembershipAssignmentView(
                membership.getTenantId(),
                membership.getWorkspaceId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getStatus().name()
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

    public record MembershipAssignmentView(
            String tenantId,
            String workspaceId,
            String userId,
            String role,
            String status
    ) {
    }
}
