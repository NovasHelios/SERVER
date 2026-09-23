package com.heilous.chat.repository;

import com.heilous.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    boolean existsByCompanyIdAndLandId(Long companyId, Long landId);
    @Query("select r from ChatRoom r join fetch r.land join fetch r.company join fetch r.owner where r.company.email = :email or r.owner.email = :email order by r.id desc")
    List<ChatRoom> findAllByParticipantEmail(@Param("email") String email);
    @Query("select r from ChatRoom r join fetch r.land join fetch r.company join fetch r.owner where r.id = :roomId")
    Optional<ChatRoom> findDetailById(@Param("roomId") Long roomId);
}
