package org.fpj.DBTests.wall;

import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.fpj.wall.application.WallCommentService;
import org.fpj.wall.domain.WallComment;
import org.fpj.wall.domain.WallCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class WallTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16").withDatabaseName("testdb")
                    .withUsername("test").withPassword("test").withInitScript("db/init.sql");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r){
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    WallCommentRepository wallCommentRepo;
    @Autowired
    UserRepository userRepo;
    @Autowired
    WallCommentService wallCommentService;

    private static final String USERNAME1 = "testuser1@web.de";
    private static final String USERNAME2 = "testuser2@gmx.de";
    private static final String USERNAME3 = "creativename@t-online.de";

    private User u1;
    private User u2;
    private User u3;

    @BeforeEach
    public void setUp(){
        wallCommentRepo.deleteAll();
        userRepo.deleteAll();

        User user1 = new User(USERNAME1, "password123");
        User user2 = new User(USERNAME2, "password456");
        User user3 = new User(USERNAME3, "password789");

        u1 = userRepo.save(user1);
        u2 = userRepo.save(user2);
        u3 = userRepo.save(user3);
    }

    @Test
    public void testToListByAuthor(){
        WallComment comment1 = new WallComment(null, u1, u2, "Schönes T-Shirt", null);
        WallComment comment2 = new WallComment(null, u3, u2, "Abzocke", null);
        WallComment comment3 = new WallComment(null, u2, u3, "Irgendwas", null);

        wallCommentService.add(comment1);
        wallCommentService.add(comment2);
        wallCommentService.add(comment3);

        List<WallComment> wallComments = wallCommentService.toListByAuthor(u2.getId());

        assertEquals(2, wallComments.size());
        assertEquals(USERNAME2, wallComments.getFirst().getAuthor().getUsername());
        assertEquals(USERNAME3, wallComments.getFirst().getWallOwner().getUsername());
        assertEquals("Abzocke", wallComments.getFirst().getContent());
        assertEquals(USERNAME2, wallComments.get(1).getAuthor().getUsername());
        assertEquals(USERNAME1, wallComments.get(1).getWallOwner().getUsername());
        assertEquals("Schönes T-Shirt", wallComments.get(1).getContent());
    }

    @Test
    public void testToListByWallOwner(){
        WallComment comment1 = new WallComment(null, u1, u2, "Schönes T-Shirt", null);
        WallComment comment2 = new WallComment(null, u3, u2, "Abzocke", null);
        WallComment comment3 = new WallComment(null, u1, u3, "Irgendwas", null);

        wallCommentService.add(comment1);
        wallCommentService.add(comment2);
        wallCommentService.add(comment3);

        List<WallComment> wallComments = wallCommentService.toListByWallOwner(u1.getId());

        assertEquals(2, wallComments.size());
        assertEquals(USERNAME1, wallComments.getFirst().getWallOwner().getUsername());
        assertEquals(USERNAME3, wallComments.getFirst().getAuthor().getUsername());
        assertEquals("Irgendwas", wallComments.getFirst().getContent());
        assertEquals(USERNAME1, wallComments.get(1).getWallOwner().getUsername());
        assertEquals(USERNAME2, wallComments.get(1).getAuthor().getUsername());
        assertEquals("Schönes T-Shirt", wallComments.get(1).getContent());
    }

}