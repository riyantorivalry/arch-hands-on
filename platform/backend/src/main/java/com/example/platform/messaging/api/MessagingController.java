package com.example.platform.messaging.api;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.messaging.application.MessagingFacade;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api")
public class MessagingController {

    private final MessagingFacade facade;

    public MessagingController(MessagingFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces/{workspaceId}/channels")
    public Mono<MessagingFacade.ChannelView> createChannel(
            @PathVariable String workspaceId,
            @RequestBody CreateChannelRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.createChannel(workspaceId, context.userId(), request.name()));
    }

    @GetMapping("/workspaces/{workspaceId}/channels")
    public Mono<List<MessagingFacade.ChannelView>> listChannels(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.listChannels(workspaceId, context.userId(), page, size));
    }

    @PostMapping("/channels/{channelId}/messages")
    public Mono<MessagingFacade.MessageView> postMessage(
            @PathVariable String channelId,
            @RequestBody PostMessageRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.postMessage(context.workspaceId(), channelId, context.userId(), request.body()));
    }

    @GetMapping("/channels/{channelId}/messages")
    public Mono<List<MessagingFacade.MessageView>> listMessages(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.listMessages(channelId, context.userId(), page, size));
    }

    @PostMapping("/messages/{messageId}/replies")
    public Mono<MessagingFacade.MessageView> reply(
            @PathVariable String messageId,
            @RequestBody PostMessageRequest request
    ) {
        return RequestContexts.currentReactive()
                .flatMap(context -> facade.replyToMessage(context.workspaceId(), messageId, context.userId(), request.body()));
    }

    public record CreateChannelRequest(@NotBlank String name) {
    }

    public record PostMessageRequest(@NotBlank String body) {
    }
}
