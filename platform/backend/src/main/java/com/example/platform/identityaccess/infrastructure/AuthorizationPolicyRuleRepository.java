package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.AuthorizationPolicyRuleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AuthorizationPolicyRuleRepository extends ReactiveCrudRepository<AuthorizationPolicyRuleEntity, String> {

    Flux<AuthorizationPolicyRuleEntity> findByEnabledTrueOrderByPriorityAscRuleIdAsc();
}
