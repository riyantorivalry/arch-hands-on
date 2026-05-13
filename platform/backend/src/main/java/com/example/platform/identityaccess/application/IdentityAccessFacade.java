package com.example.platform.identityaccess.application;

import org.springframework.stereotype.Service;

@Service
public class IdentityAccessFacade {

    public CurrentActorView getCurrentActor(String workspaceId, String userId) {
        return new CurrentActorView(userId, workspaceId, "MEMBER");
    }

    public record CurrentActorView(String userId, String workspaceId, String workspaceRole) {
    }
}
