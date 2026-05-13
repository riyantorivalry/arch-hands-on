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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class MessagingController {

    private final MessagingFacade facade;

    public MessagingController(MessagingFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces/{workspaceId}/channels")
    public MessagingFacade.ChannelView createChannel(
            @PathVariable String workspaceId,
            @RequestBody CreateChannelRequest request
    ) {
        return facade.createChannel(workspaceId, RequestContexts.current().userId(), request.name());
    }

    @GetMapping("/workspaces/{workspaceId}/channels")
    public List<MessagingFacade.ChannelView> listChannels(@PathVariable String workspaceId) {
        return facade.listChannels(workspaceId);
    }

    @PostMapping("/channels/{channelId}/messages")
    public MessagingFacade.MessageView postMessage(
            @PathVariable String channelId,
            @RequestBody PostMessageRequest request
    ) {
        return facade.postMessage(RequestContexts.current().workspaceId(), channelId, RequestContexts.current().userId(), request.body());
    }

    @GetMapping("/channels/{channelId}/messages")
    public List<MessagingFacade.MessageView> listMessages(@PathVariable String channelId) {
        return facade.listMessages(channelId);
    }

    @PostMapping("/messages/{messageId}/replies")
    public MessagingFacade.MessageView reply(
            @PathVariable String messageId,
            @RequestBody PostMessageRequest request
    ) {
        return facade.replyToMessage(RequestContexts.current().workspaceId(), messageId, RequestContexts.current().userId(), request.body());
    }

    public record CreateChannelRequest(@NotBlank String name) {
    }

    public record PostMessageRequest(@NotBlank String body) {
    }
}
