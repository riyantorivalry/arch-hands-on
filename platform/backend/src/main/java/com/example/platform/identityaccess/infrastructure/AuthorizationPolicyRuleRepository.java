package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.AuthorizationPolicyRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationPolicyRuleRepository extends JpaRepository<AuthorizationPolicyRuleEntity, String> {

    List<AuthorizationPolicyRuleEntity> findByEnabledTrueOrderByPriorityAscRuleIdAsc();
}
