package com.heilous.chat.repository;

import com.heilous.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("select m from ChatMessage m join fetch m.sender where m.room.id = :roomId order by m.id asc")
    List<ChatMessage> findAllByRoomIdOrderByIdAsc(@Param("roomId") Long roomId);

    @Query("select count(m) from ChatMessage m where m.room.id = :roomId and m.sender.email <> :email and m.readAt is null")
    long countUnreadByRoomIdAndReceiverEmail(@Param("roomId") Long roomId, @Param("email") String email);

    Optional<ChatMessage> findTopByRoomIdOrderByIdDesc(Long roomId);
}
