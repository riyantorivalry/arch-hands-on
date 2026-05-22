package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.UserSessionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserSessionRepository extends ReactiveCrudRepository<UserSessionEntity, String> {

    Mono<UserSessionEntity> findBySessionToken(String sessionToken);
}
