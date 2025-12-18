package org.fpj.DBTests.payments;

import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionRepository;
import org.fpj.payments.domain.TransactionType;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class TransactionTest {

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
    TransactionRepository txRepo;
    @Autowired
    TransactionService transactionService;
    @Autowired
    UserRepository userRepo;

    private Long userId1;
    private Long userId2;

    private static final String USERNAME1 = "testuser1@web.de";
    private static final String USERNAME2 = "testuser2@gmx.de";
    private static final String SUBJECT = "Beschreibung";

    private final User currentUser = new User(USERNAME1, "password123");
    private final User user2 = new User(USERNAME2, "password456");

    @BeforeEach
    public void setUp(){
        userRepo.deleteAll();

        userRepo.saveAndFlush(currentUser);
        userRepo.saveAndFlush(user2);

        userId1 = currentUser.getId();
        userId2 = user2.getId();
    }

    @Test
    public void testTransfer(){
        BigDecimal amount = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(10);
        TransactionLite transactionLite1 = new TransactionLite(amount, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        BigDecimal balanceAfterPayinUser1 = txRepo.computeBalance(userId1);
        BigDecimal balanceAfterPayinUser2 = txRepo.computeBalance(userId2);
        transactionService.sendTransfers(transactionLite2, currentUser);
        BigDecimal balanceAfterTransferUser1 = txRepo.computeBalance(userId1);
        BigDecimal balanceAfterTransferUser2 = txRepo.computeBalance(userId2);

        BigDecimal expected1 = BigDecimal.valueOf(30);
        BigDecimal expected2 = BigDecimal.ZERO;
        BigDecimal expected3 = BigDecimal.valueOf(20);
        BigDecimal expected4 = BigDecimal.valueOf(10);

        assertEquals(0, balanceAfterPayinUser1.compareTo(expected1));
        assertEquals(0, balanceAfterPayinUser2.compareTo(expected2));
        assertEquals(0, balanceAfterTransferUser1.compareTo(expected3));
        assertEquals(0, balanceAfterTransferUser2.compareTo(expected4));
    }

}
