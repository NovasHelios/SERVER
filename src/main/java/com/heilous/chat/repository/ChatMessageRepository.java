package com.heilous.chat.repository;

import com.heilous.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m join fetch m.sender where m.room.id = :roomId order by m.id asc")
    List<ChatMessage> findAllByRoomIdOrderByIdAsc(@Param("roomId") Long roomId);

    // 커서 기반 페이지네이션: cursorId 이후 메시지를 오래된 순으로 조회
    @Query("select m from ChatMessage m join fetch m.sender where m.room.id = :roomId and m.id > :cursorId order by m.id asc")
    List<ChatMessage> findByRoomIdAfterCursor(@Param("roomId") Long roomId,
                                               @Param("cursorId") Long cursorId,
                                               Pageable pageable);

    // 최초 조회 (cursor 없이 최신 N개를 역순으로 가져온 뒤 뒤집어서 사용)
    @Query("select m from ChatMessage m join fetch m.sender where m.room.id = :roomId order by m.id desc")
    List<ChatMessage> findByRoomIdLatest(@Param("roomId") Long roomId, Pageable pageable);

    // bulk 읽음 처리: 내가 보낸 메시지가 아닌 미읽음 메시지 일괄 업데이트
    @Modifying
    @Query("update ChatMessage m set m.readAt = :now where m.room.id = :roomId and m.sender.email <> :email and m.readAt is null")
    int markAllAsRead(@Param("roomId") Long roomId,
                      @Param("email") String email,
                      @Param("now") LocalDateTime now);

    @Query("select count(m) from ChatMessage m where m.room.id = :roomId and m.sender.email <> :email and m.readAt is null")
    long countUnreadByRoomIdAndReceiverEmail(@Param("roomId") Long roomId, @Param("email") String email);

    Optional<ChatMessage> findTopByRoomIdOrderByIdDesc(Long roomId);

    // getRooms() N+1 해결용: 채팅방 ID 목록으로 각 방의 최신 메시지 한 번에 조회
    @Query("select m from ChatMessage m join fetch m.sender where m.id in " +
           "(select max(m2.id) from ChatMessage m2 where m2.room.id in :roomIds group by m2.room.id)")
    List<ChatMessage> findLatestMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);

    // getRooms() N+1 해결용: 채팅방 ID 목록으로 미읽음 수 한 번에 집계
    @Query("select m.room.id, count(m) from ChatMessage m " +
           "where m.room.id in :roomIds and m.sender.email <> :email and m.readAt is null " +
           "group by m.room.id")
    List<Object[]> countUnreadGroupByRoomIds(@Param("roomIds") List<Long> roomIds,
                                              @Param("email") String email);
}
