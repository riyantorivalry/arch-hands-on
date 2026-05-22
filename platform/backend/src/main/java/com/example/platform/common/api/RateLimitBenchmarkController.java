package com.example.platform.common.api;

import com.example.platform.common.infrastructure.RateLimitService;
import com.example.platform.common.infrastructure.ratelimit.RateLimitDecision;
import com.example.platform.common.web.RequestContexts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/benchmarks/rate-limit")
public class RateLimitBenchmarkController {

    private final RateLimitService rateLimitService;

    public RateLimitBenchmarkController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/algorithm")
    public Mono<RateLimitAlgorithmResponse> selectedAlgorithm() {
        return RequestContexts.authenticatedReactive()
                .thenReturn(new RateLimitAlgorithmResponse(rateLimitService.mode()));
    }

    @PostMapping("/{algorithm}/decisions")
    public Mono<RateLimitDecision> check(
            @PathVariable String algorithm,
            @Valid @RequestBody RateLimitDecisionRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .then(Mono.defer(() -> rateLimitService.check(algorithm, request.key(), request.limit(), request.windowSeconds())));
    }

    public record RateLimitAlgorithmResponse(String selectedAlgorithm) {
    }

    public record RateLimitDecisionRequest(
            @NotBlank String key,
            @Min(1) int limit,
            @Min(1) int windowSeconds
    ) {
    }
}
