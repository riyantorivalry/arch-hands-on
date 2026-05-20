package com.example.platform.realtime.api;

import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.common.web.RequestContexts;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.realtime.application.RealtimeEventService;
import com.example.platform.realtime.application.RealtimeEventService.RealtimeEvent;
import com.example.platform.realtime.application.RealtimeEventService.RealtimePollResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Validated
@RestController
@RequestMapping("/api")
public class RealtimeEventsController {

    private final RealtimeEventService realtimeEventService;
    private final MembershipRepository membershipRepository;

    public RealtimeEventsController(
            RealtimeEventService realtimeEventService,
            MembershipRepository membershipRepository
    ) {
        this.realtimeEventService = realtimeEventService;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/v1/workspaces/{workspaceId}/events")
    public RealtimePollResponse pollEvents(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") long since
    ) {
        MembershipEntity membership = requireActiveMembership(workspaceId);
        return realtimeEventService.poll(membership.getTenantId(), workspaceId, since);
    }

    @GetMapping(path = "/v2/workspaces/{workspaceId}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeEvent>> streamEvents(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") long since
    ) {
        MembershipEntity membership = requireActiveMembership(workspaceId);
        return realtimeEventService.stream(membership.getTenantId(), workspaceId, since);
    }

    private MembershipEntity requireActiveMembership(String workspaceId) {
        String userId = RequestContexts.authenticated().userId();
        MembershipEntity membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AuthorizationDeniedException("User is not a member of workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }
        return membership;
    }
}
