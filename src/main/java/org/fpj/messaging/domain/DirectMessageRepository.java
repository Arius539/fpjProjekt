package org.fpj.messaging.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long>{
    Page<DirectMessage> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Page<DirectMessage> findBySender_IdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    @Query("""
        select dm
        from DirectMessage dm
        where (dm.sender.id = :a and dm.recipient.id = :b)
           or (dm.sender.id = :b and dm.recipient.id = :a)
        order by dm.createdAt desc
    """)
    Page<DirectMessage> findConversation(@Param("a") Long userA, @Param("b") Long userB, Pageable pageable);

    @Query(value = """
    select *
    from direct_messages dm
    where (dm.sender = :a and dm.recipient = :b)
       or (dm.sender = :b and dm.recipient = :a)
    order by dm.created_at desc
    limit 1
    """, nativeQuery = true)
    Optional<DirectMessage> lastMessageNative(@Param("a") Long userA,
                                              @Param("b") Long userB);

    @Query(
            value = """
            INSERT INTO direct_messages (sender, recipient, content, created_at) 
            VALUES (:senderId, :recipientId, :content, NOW())
            RETURNING id 
        """,
            nativeQuery = true
    )
    Long add(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId, @Param("content") String content);

    @Query("SELECT d FROM DirectMessage d WHERE d.id = :id")
    Optional<DirectMessage> getDirectMessageById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sender", "recipient"})
    @Query("""
        select dm
        from DirectMessage dm
        where (dm.sender.id = :a and dm.recipient.id = :b)
           or (dm.sender.id = :b and dm.recipient.id = :a)
        order by dm.createdAt desc
    """)
    List<DirectMessage> findConversationAsList(@Param("a") Long userA, @Param("b") Long userB);
}
