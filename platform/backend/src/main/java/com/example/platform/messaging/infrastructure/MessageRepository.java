package com.example.platform.messaging.infrastructure;

import com.example.platform.messaging.domain.MessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MessageRepository extends ReactiveCrudRepository<MessageEntity, String> {

    Flux<MessageEntity> findByChannelIdOrderByCreatedAtAsc(String channelId);

    @Query("""
            select *
            from messages
            where channel_id = :channelId
            order by created_at asc
            limit :limit
            offset :offset
            """)
    Flux<MessageEntity> findByChannelIdOrderByCreatedAtAsc(
            @Param("channelId") String channelId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );
}
