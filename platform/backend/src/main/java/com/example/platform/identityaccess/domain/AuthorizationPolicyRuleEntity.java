package com.example.platform.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "authorization_policy_rules", indexes = {
        @Index(name = "idx_authorization_policy_rules_enabled_priority", columnList = "enabled,priority")
})
public class AuthorizationPolicyRuleEntity {

    @Id
    @Column(name = "rule_id", nullable = false, length = 96)
    private String ruleId;

    @Column(name = "policy_id", nullable = false, length = 96)
    private String policyId;

    @Column(name = "effect", nullable = false, length = 16)
    private String effect;

    @Column(name = "role_name", nullable = false, length = 32)
    private String roleName;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "condition_type", nullable = false, length = 64)
    private String conditionType;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthorizationPolicyRuleEntity() {
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getEffect() {
        return effect;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getConditionType() {
        return conditionType;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
