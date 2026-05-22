package com.example.platform.identityaccess.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class UserEntity extends AbstractAuditableEntity {

    @Id
    @Column("user_id")
    private String userId;

    @Column("email")
    private String email;

    @Column("display_name")
    private String displayName;

    @Column("status")
    private UserStatus status;

    protected UserEntity() {
    }

    public UserEntity(String userId, String email, String displayName, UserStatus status) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Object getId() {
        return userId;
    }
}
