package com.example.platform.identityaccess.application;

import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public void requireWorkspaceManager(MembershipEntity membership) {
        if (membership.getRole() != MembershipRole.OWNER && membership.getRole() != MembershipRole.ADMIN) {
            throw new AuthorizationDeniedException("Workspace manager role is required");
        }
    }

    public void requireOwnerOrAdminOrResourceOwner(MembershipEntity membership, String resourceOwnerUserId) {
        if (membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN) {
            return;
        }
        if (!membership.getUserId().equals(resourceOwnerUserId)) {
            throw new AuthorizationDeniedException("Actor is not allowed to modify this resource");
        }
    }

    public void requireOwnerOrAdminOrAssignee(
            MembershipEntity membership,
            String resourceOwnerUserId,
            String assigneeUserId
    ) {
        if (membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN) {
            return;
        }
        if (membership.getUserId().equals(resourceOwnerUserId)) {
            return;
        }
        if (assigneeUserId != null && membership.getUserId().equals(assigneeUserId)) {
            return;
        }
        throw new AuthorizationDeniedException("Actor is not allowed to modify this task");
    }
}
