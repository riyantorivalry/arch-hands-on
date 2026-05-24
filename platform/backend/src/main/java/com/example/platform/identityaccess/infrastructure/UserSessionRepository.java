package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.UserSessionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    Optional<UserSessionEntity> findBySessionToken(String sessionToken);

    Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash);
}
