package org.fpj.UnitTests.wall;

import org.fpj.users.domain.User;
import org.fpj.wall.application.WallCommentService;
import org.fpj.wall.domain.WallComment;
import org.fpj.wall.domain.WallCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class WallCommentServiceTest {

    @Mock
    User currentUser;
    @Mock
    User user2;
    @Mock
    WallComment commentReturned;

    @Mock
    WallCommentRepository wallCommentRepository;

    @InjectMocks
    WallCommentService underTest;

    private static final String USERNAME1 = "testuser1@web.de";
    private static final String USERNAME2 = "testuser2@gmx.de";

    private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2025, 12, 11, 13, 30);
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final Instant TIME = LOCAL_DATE_TIME.atZone(ZONE).toInstant();

    @Test
    public void testAdd(){
        WallComment comment = new WallComment(1L, currentUser, user2, "Schönes T-Shirt", TIME);

        when(currentUser.getUsername()).thenReturn(USERNAME1);
        when(user2.getUsername()).thenReturn(USERNAME2);
        when(wallCommentRepository.save(comment)).thenReturn(commentReturned);

        underTest.add(comment);

        Mockito.verify(wallCommentRepository, Mockito.times(1)).save(comment);
    }

    @Test
    public void testAddNoWallOwner(){
        WallComment comment = new WallComment(1L, null, user2, "Schönes T-Shirt", TIME);

        assertThrows(IllegalArgumentException.class, () -> underTest.add(comment));
    }

    @Test
    public void testAddNoAuthor(){
        WallComment comment = new WallComment(1L, currentUser, null, "Schönes T-Shirt", TIME);

        assertThrows(IllegalArgumentException.class, () -> underTest.add(comment));
    }

    @Test
    public void testAddAuthorSameAsOwner(){
        WallComment comment = new WallComment(1L, currentUser, currentUser, "Schönes T-Shirt", TIME);

        when(currentUser.getUsername()).thenReturn(USERNAME1);

        assertThrows(IllegalArgumentException.class, () -> underTest.add(comment));
    }
}
