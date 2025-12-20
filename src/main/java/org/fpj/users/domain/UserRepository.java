package org.fpj.users.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<UsernameOnly> findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String term);

    @Query("""
        select u
        from User u
        where u.id <> :a
          and exists (
                select 1
                from DirectMessage dm
                where (dm.sender.id = :a and dm.recipient.id = u.id)
                   or (dm.sender.id = u.id and dm.recipient.id = :a)
          )
        order by u.username asc
    """)
    Page<User> findContacts(@Param("a") Long userId, Pageable pageable);

    @Query(
            value = """
            select u.*
            from users u
            join (
                select contact_id, max(created_at) as last_at
                from (
                    select dm.recipient as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.sender = :a
                    union all
                    select dm.sender as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.recipient = :a
                ) x
                group by contact_id
            ) c on c.contact_id = u.id
            order by c.last_at desc
            """,
            countQuery = """
            select count(*) 
            from (
                select contact_id
                from (
                    select dm.recipient as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.sender = :a
                    union all
                    select dm.sender   as contact_id, dm.created_at
                    from direct_messages dm
                    where dm.recipient = :a
                ) x
                group by contact_id
            ) contacts
            """,
            nativeQuery = true
    )
    Page<User> findContactsOrderByLastMessageDesc(@Param("a") Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> lockById(@Param("id") Long userId);
}

