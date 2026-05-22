package com.example.platform.identityaccess.application;

import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.identityaccess.domain.WorkspaceMemberAddedEvent;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class IdentityAccessFacade {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final DomainEventPublisher domainEventPublisher;

    public IdentityAccessFacade(UserRepository userRepository, MembershipRepository membershipRepository, DomainEventPublisher domainEventPublisher) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<CurrentActorView> getCurrentActor(String workspaceId, String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
                .zipWith(membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Membership not found for workspace " + workspaceId))))
                .map(tuple -> {
                    var user = tuple.getT1();
                    var membership = tuple.getT2();
                    return new CurrentActorView(
                            user.getUserId(),
                            user.getDisplayName(),
                            user.getEmail(),
                            membership.getWorkspaceId(),
                            membership.getTenantId(),
                            membership.getRole().name()
                    );
                });
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<MembershipAssignmentView> assignMembership(
            String actorUserId,
            String workspaceId,
            String userId,
            String email,
            String displayName,
            String role
    ) {
        return membershipRepository.findByWorkspaceIdAndUserId(workspaceId, actorUserId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Membership not found for actor in workspace " + workspaceId)))
                .flatMap(actorMembership -> {
                    if (actorMembership.getStatus() != com.example.platform.identityaccess.domain.MembershipStatus.ACTIVE) {
                        return Mono.error(new IllegalStateException("Actor membership is not active"));
                    }
                    if (actorMembership.getRole() != com.example.platform.identityaccess.domain.MembershipRole.OWNER
                            && actorMembership.getRole() != com.example.platform.identityaccess.domain.MembershipRole.ADMIN) {
                        return Mono.error(new com.example.platform.common.web.AuthorizationDeniedException("Workspace manager role is required"));
                    }

                    var assignedRole = com.example.platform.identityaccess.domain.MembershipRole.valueOf(role);
                    return userRepository.findById(userId)
                            .switchIfEmpty(userRepository.save(
                                    new com.example.platform.identityaccess.domain.UserEntity(
                                            userId,
                                            email,
                                            displayName,
                                            com.example.platform.identityaccess.domain.UserStatus.ACTIVE
                                    )
                            ))
                            .flatMap(user -> membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                                    .switchIfEmpty(membershipRepository.save(
                                            new com.example.platform.identityaccess.domain.MembershipEntity(
                                                    actorMembership.getTenantId(),
                                                    workspaceId,
                                                    user.getUserId(),
                                                    assignedRole,
                                                    com.example.platform.identityaccess.domain.MembershipStatus.ACTIVE
                                            )
                                    )));
                })
                .flatMap(membership -> domainEventPublisher.publish(new WorkspaceMemberAddedEvent(
                                membership.getTenantId(),
                                membership.getWorkspaceId(),
                                membership.getUserId(),
                                membership.getRole().name()
                        ))
                        .thenReturn(membership))
                .map(membership -> new MembershipAssignmentView(
                        membership.getTenantId(),
                        membership.getWorkspaceId(),
                        membership.getUserId(),
                        membership.getRole().name(),
                        membership.getStatus().name()
                ));
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
