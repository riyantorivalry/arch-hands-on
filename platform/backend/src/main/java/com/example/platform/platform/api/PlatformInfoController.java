package com.example.platform.platform.api;

import com.example.platform.common.web.RequestContextResponse;
import com.example.platform.common.web.RequestContexts;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformInfoController {

    @GetMapping("/info")
    public PlatformInfoResponse info() {
        var context = RequestContexts.current();
        return new PlatformInfoResponse(
                "platform-backend",
                "0.0.1-SNAPSHOT",
                "modular-monolith",
                List.of("identity-access", "tenant-management", "messaging", "documents", "tasks"),
                context.isAuthenticated() ? RequestContextResponse.from(context) : null
        );
    }

    public record PlatformInfoResponse(
            String service,
            String version,
            String architectureStyle,
            List<String> modules,
            RequestContextResponse requestContext
    ) {
    }
}
