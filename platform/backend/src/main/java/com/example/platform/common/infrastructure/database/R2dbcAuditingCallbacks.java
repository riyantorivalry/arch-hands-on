package com.example.platform.common.infrastructure.database;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class R2dbcAuditingCallbacks implements BeforeConvertCallback<AbstractAuditableEntity> {

    @Override
    public Publisher<AbstractAuditableEntity> onBeforeConvert(AbstractAuditableEntity entity, SqlIdentifier table) {
        if (entity.getCreatedAt() == null) {
            entity.touchForCreate();
        } else {
            entity.touchForUpdate();
        }
        return Mono.just(entity);
    }
}
