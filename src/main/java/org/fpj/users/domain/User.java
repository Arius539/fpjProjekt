package org.fpj.users.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(
        name = "users",
        indexes = {
                @Index(name = "users_username_uq", columnList = "username", unique = true)
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq_gen")
    @SequenceGenerator(
            name = "users_id_seq_gen",
            sequenceName = "users_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, length = 320)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*
    sollten wir doch lieber dynamisch laden, immer 100 am Stück, erst wenn der Nutzer es will oder?
    @OneToMany(mappedBy = "wallComment", cascade = CascadeType.ALL)
    private List<WallComment> wallComments;*/
    //--> ja

    public User(final String username, final String passwordHash){
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (username != null) username = username.toLowerCase();
    }

}
