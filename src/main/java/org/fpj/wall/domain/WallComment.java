package org.fpj.wall.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.fpj.users.domain.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(
        name = "pinboard_comments",
        indexes = {
                @Index(name = "pb_owner_ctime", columnList = "wall_owner_id,created_at"),
                @Index(name = "pb_author_ctime", columnList = "author_id,created_at")
        }
)
@Comment("Comments on user pinboards (owner vs. author).")
public class WallComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wall_owner_id", nullable = false)
    private User wallOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
