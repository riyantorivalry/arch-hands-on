package com.example.platform.identityaccess.domain;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;

@Table("authorization_policy_rules")
public class AuthorizationPolicyRuleEntity {

    @Id
    @Column("rule_id")
    private String ruleId;

    @Column("policy_id")
    private String policyId;

    @Column("effect")
    private String effect;

    @Column("role_name")
    private String roleName;

    @Column("action")
    private String action;

    @Column("resource_type")
    private String resourceType;

    @Column("condition_type")
    private String conditionType;

    @Column("priority")
    private int priority;

    @Column("enabled")
    private boolean enabled;

    @Column("created_at")
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
