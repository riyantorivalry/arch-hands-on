package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, String> {

    Mono<UserEntity> findByEmail(String email);
}
