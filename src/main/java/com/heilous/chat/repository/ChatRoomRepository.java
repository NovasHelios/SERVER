package com.heilous.chat.repository;

import com.heilous.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    boolean existsByCompanyIdAndLandId(Long companyId, Long landId);

    /** 토지 없는 1:1 채팅방 중복 방지 */
    boolean existsByCompanyIdAndOwnerIdAndLandIsNull(Long companyId, Long ownerId);

    @Query("select r from ChatRoom r join fetch r.company join fetch r.owner left join fetch r.land " +
           "where r.company.email = :email or r.owner.email = :email order by r.id desc")
    List<ChatRoom> findAllByParticipantEmail(@Param("email") String email);

    @Query("select r from ChatRoom r join fetch r.company join fetch r.owner left join fetch r.land " +
           "where r.id = :roomId")
    Optional<ChatRoom> findDetailById(@Param("roomId") Long roomId);
}
