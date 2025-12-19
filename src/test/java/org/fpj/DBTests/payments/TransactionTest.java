package org.fpj.DBTests.payments;

import org.fpj.exceptions.DataNotPresentException;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.*;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        txRepo.deleteAll();
        userRepo.deleteAll();

        userRepo.save(currentUser);
        userRepo.save(user2);

        userId1 = currentUser.getId();
        userId2 = user2.getId();
    }

    @Test
    public void testPayin(){
        BigDecimal amount = BigDecimal.valueOf(30);
        TransactionLite transactionLite1 = new TransactionLite(amount, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);

        BigDecimal balanceBeforePayin = txRepo.computeBalance(userId1);
        transactionService.sendTransfers(transactionLite1, currentUser);
        BigDecimal balanceAfterPayin = txRepo.computeBalance(userId1);

        BigDecimal expected1 = BigDecimal.ZERO;
        BigDecimal expected2 = BigDecimal.valueOf(30);

        assertEquals(0, balanceBeforePayin.compareTo(expected1));
        assertEquals(0, balanceAfterPayin.compareTo(expected2));
    }

    @Test
    public void testTransfer(){
        BigDecimal amount = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(10);
        TransactionLite transactionLite1 = new TransactionLite(amount, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        BigDecimal balanceBeforeTransferUser2 = txRepo.computeBalance(userId2);
        transactionService.sendTransfers(transactionLite2, currentUser);
        BigDecimal balanceAfterTransferUser1 = txRepo.computeBalance(userId1);
        BigDecimal balanceAfterTransferUser2 = txRepo.computeBalance(userId2);

        BigDecimal expected1 = BigDecimal.ZERO;
        BigDecimal expected2 = BigDecimal.valueOf(20);
        BigDecimal expected3 = BigDecimal.valueOf(10);

        assertEquals(0, balanceBeforeTransferUser2.compareTo(expected1));
        assertEquals(0, balanceAfterTransferUser1.compareTo(expected2));
        assertEquals(0, balanceAfterTransferUser2.compareTo(expected3));
    }

    @Test
    public void testPayout(){
        BigDecimal amount = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(10);
        TransactionLite transactionLite1 = new TransactionLite(amount, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.AUSZAHLUNG, USERNAME1, null, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        transactionService.sendTransfers(transactionLite2, currentUser);
        BigDecimal balanceAfterPayout = txRepo.computeBalance(userId1);
        BigDecimal expected = BigDecimal.valueOf(20);

        assertEquals(0, balanceAfterPayout.compareTo(expected));
    }

    @Test
    public void testTransferNoUserFound(){
        userRepo.delete(user2);

        BigDecimal amount = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(10);
        TransactionLite transactionLite1 = new TransactionLite(amount, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        BigDecimal balanceBeforeTransfer = txRepo.computeBalance(userId1);
        assertThrows(DataNotPresentException.class, () -> transactionService.sendTransfers(transactionLite2, currentUser));
        BigDecimal balanceAfterFailedTransfer = txRepo.computeBalance(userId1);

        assertEquals(0, balanceAfterFailedTransfer.compareTo(balanceBeforeTransfer));
    }

    @Test
    public void testFindLiteItemsForUser(){
        BigDecimal amount1 = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(40);
        TransactionLite transactionLite1 = new TransactionLite(amount1, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite3 = new TransactionLite(amount1, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        transactionService.sendTransfers(transactionLite2, currentUser);
        transactionService.sendTransfers(transactionLite3, currentUser);

        Page<TransactionRow> transactionRows = transactionService.findLiteItemsForUser(userId1, 0, 1);
        long elements = transactionRows.getTotalElements();
        List<TransactionRow> transactionRowList = transactionRows.getContent();

        //werden absteigend nach Datum zurückgegeben
        BigDecimal amount = transactionRowList.getFirst().amount();
        TransactionType type = transactionRowList.getFirst().type();
        Long senderId = transactionRowList.getFirst().senderId();
        Long recipientId = transactionRowList.getFirst().recipientId();

        assertEquals(3L, elements);
        assertEquals(0, amount.compareTo(amount1));
        assertEquals(TransactionType.UEBERWEISUNG, type);
        assertEquals(userId1, senderId);
        assertEquals(userId2, recipientId);
    }

    @Test
    public void testTransactionsForUserAsList(){
        BigDecimal amount1 = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(40);
        TransactionLite transactionLite1 = new TransactionLite(amount1, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite3 = new TransactionLite(amount1, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        transactionService.sendTransfers(transactionLite2, currentUser);
        transactionService.sendTransfers(transactionLite3, currentUser);

        List<TransactionRow> transactionRowList = transactionService.transactionsForUserAsList(userId1);
        int elements = transactionRowList.size();

        //werden absteigend nach Datum zurückgegeben
        BigDecimal amount = transactionRowList.getFirst().amount();
        TransactionType type = transactionRowList.getFirst().type();
        Long senderId = transactionRowList.getFirst().senderId();
        Long recipientId = transactionRowList.getFirst().recipientId();

        assertEquals(3, elements);
        assertEquals(0, amount.compareTo(amount1));
        assertEquals(TransactionType.UEBERWEISUNG, type);
        assertEquals(userId1, senderId);
        assertEquals(userId2, recipientId);
    }

    @Test
    public void testSearchTransactions(){
        BigDecimal amount1 = BigDecimal.valueOf(30);
        BigDecimal amount2 = BigDecimal.valueOf(40);
        TransactionLite transactionLite1 = new TransactionLite(amount1, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite2 = new TransactionLite(amount2, TransactionType.EINZAHLUNG, null, USERNAME1, SUBJECT);
        TransactionLite transactionLite3 = new TransactionLite(amount1, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);
        TransactionLite transactionLite4 = new TransactionLite(amount2, TransactionType.UEBERWEISUNG, USERNAME1, USERNAME2, SUBJECT);

        transactionService.sendTransfers(transactionLite1, currentUser);
        transactionService.sendTransfers(transactionLite2, currentUser);
        transactionService.sendTransfers(transactionLite3, currentUser);
        transactionService.sendTransfers(transactionLite4, currentUser);

        LocalDateTime localDateTime = LocalDateTime.of(2025, 12, 11, 13, 30);
        ZoneId zone = ZoneId.of("Europe/Berlin");
        Instant from = localDateTime.atZone(zone).toInstant();

        TransactionViewSearchParameter parameter = new TransactionViewSearchParameter(userId1, null, from, null, USERNAME1, BigDecimal.valueOf(35), BigDecimal.valueOf(60));
        Page<TransactionRow> transactionRows = transactionService.searchTransactions(parameter, 0, 1);
        TransactionRow transactionRow = transactionRows.getContent().getFirst();
        long size = transactionRows.getTotalElements();
        long senderId = transactionRow.senderId();
        long recipientId = transactionRow.recipientId();
        BigDecimal amount = transactionRow.amount();

        assertEquals(3, size);
        assertEquals(userId1, senderId);
        assertEquals(userId2, recipientId);
        assertEquals(amount2, amount);
    }

}
