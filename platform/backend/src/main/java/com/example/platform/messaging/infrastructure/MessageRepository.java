package com.example.platform.messaging.infrastructure;

import com.example.platform.messaging.domain.MessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    List<MessageEntity> findByChannelIdOrderByCreatedAtAsc(String channelId);
}
