package org.fpj.UnitTests.payments;

import org.fpj.exceptions.TransactionException;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.*;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    TransactionRepository txRepo;
    @Mock
    UserService userService;
    @Mock
    UserRepository userRepo;

    @Mock
    User sender;
    @Mock
    User recipient;


    @InjectMocks
    private TransactionService underTest = new TransactionService();

    private static final String SENDER_USERNAME = "testusername1@web.de";
    private static final String RECIPIENT_USERNAME = "testusername2@web.de";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(120);
    private static final int STANDARD_BALANCE = 200;
    private static final String SUBJECT = "Subject";
    private static final Long SENDER_ID = 582173450L;
    private static final Long RECIPIENT_ID = 43178506L;

    @Test
    public void testPayin(){
        TransactionLite transactionLite = new TransactionLite(AMOUNT, TransactionType.EINZAHLUNG, null, RECIPIENT_USERNAME, SUBJECT);

        when(recipient.getId()).thenReturn(RECIPIENT_ID);
        when(userRepo.lockById(RECIPIENT_ID)).thenReturn(Optional.of(recipient));
        when(txRepo.computeBalance(RECIPIENT_ID)).thenReturn(BigDecimal.valueOf(STANDARD_BALANCE));

        Transaction expectedTransaction = new Transaction(null, TransactionType.EINZAHLUNG, AMOUNT, null, recipient, SUBJECT, null);
        BigDecimal expectedCurrentBalance = BigDecimal.valueOf(STANDARD_BALANCE).add(AMOUNT);

        TransactionResult actual = underTest.sendTransfers(transactionLite, recipient);

        assertEquals(expectedTransaction, actual.transaction());
        assertEquals(expectedCurrentBalance, actual.newBalance());
    }

    @Test
    public void testPayout(){
        TransactionLite transactionLite = new TransactionLite(AMOUNT, TransactionType.AUSZAHLUNG, SENDER_USERNAME, null, SUBJECT);

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(STANDARD_BALANCE));

        Transaction expectedTransaction = new Transaction(null, TransactionType.AUSZAHLUNG, AMOUNT, sender, null, SUBJECT, null);
        BigDecimal expectedCurrentBalance = BigDecimal.valueOf(STANDARD_BALANCE).subtract(AMOUNT);

        TransactionResult actual = underTest.sendTransfers(transactionLite, sender);

        assertEquals(expectedTransaction, actual.transaction());
        assertEquals(expectedCurrentBalance, actual.newBalance());
    }

    @Test
    public void testPayoutDeficientBalance(){
        TransactionLite transactionLite = new TransactionLite(AMOUNT, TransactionType.AUSZAHLUNG, SENDER_USERNAME, null, SUBJECT);
        final int senderBalanceDeficient = 100;

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(senderBalanceDeficient));

        assertThrows(TransactionException.class, () -> underTest.sendTransfers(transactionLite, sender));
    }

    @Test
    public void testTransfer(){
        TransactionLite transactionLite = new TransactionLite(AMOUNT, TransactionType.UEBERWEISUNG, SENDER_USERNAME, RECIPIENT_USERNAME, SUBJECT);

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(STANDARD_BALANCE));
        when(userService.findByUsername(RECIPIENT_USERNAME)).thenReturn(recipient);
        when(recipient.getId()).thenReturn(RECIPIENT_ID);

        Transaction expectedTransaction = new Transaction(null, TransactionType.UEBERWEISUNG, AMOUNT, sender, recipient, SUBJECT, null);
        BigDecimal expectedCurrentBalance = BigDecimal.valueOf(STANDARD_BALANCE).subtract(AMOUNT);

        TransactionResult actual = underTest.sendTransfers(transactionLite, sender);

        assertEquals(expectedTransaction, actual.transaction());
        assertEquals(expectedCurrentBalance, actual.newBalance());
    }

    @Test
    public void testTransferDeficientBalance(){
        TransactionLite transactionLite = new TransactionLite(AMOUNT, TransactionType.UEBERWEISUNG, SENDER_USERNAME, RECIPIENT_USERNAME, SUBJECT);
        final int senderBalanceDeficient = 100;

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(senderBalanceDeficient));

        assertThrows(TransactionException.class, () -> underTest.sendTransfers(transactionLite, sender));
    }

    @Test
    public void testTransferNoRecipientUsername(){
        final String recipientUsername = "";
        TransactionLite transactionLite = new TransactionLite(AMOUNT, TransactionType.UEBERWEISUNG, SENDER_USERNAME, recipientUsername, SUBJECT);

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(STANDARD_BALANCE));

        assertThrows(TransactionException.class, () -> underTest.sendTransfers(transactionLite, sender));
    }

    @Test
    public void testSendBulkTransfers(){
        TransactionLite payin = new TransactionLite(BigDecimal.valueOf(100), TransactionType.EINZAHLUNG, null, SENDER_USERNAME, SUBJECT);
        TransactionLite payout = new TransactionLite(BigDecimal.valueOf(20), TransactionType.AUSZAHLUNG, SENDER_USERNAME, null, SUBJECT);
        TransactionLite transfer = new TransactionLite(BigDecimal.valueOf(50), TransactionType.UEBERWEISUNG, SENDER_USERNAME, RECIPIENT_USERNAME, SUBJECT);
        final List<TransactionLite> transactionsLite = List.of(payin, payout, transfer, transfer, payin);
        final int balance = 0;

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(balance));
        when(userService.findByUsername(RECIPIENT_USERNAME)).thenReturn(recipient);
        when(recipient.getId()).thenReturn(RECIPIENT_ID);

        ArrayList<TransactionResult> results = underTest.sendBulkTransfers(transactionsLite, sender);
        BigDecimal newBalance = results.get(0).newBalance();
        BigDecimal expected = BigDecimal.valueOf(80);

        assertEquals(expected, newBalance);
    }

    @Test
    public void testSendBulkTransfersDeficientBalance(){
        TransactionLite payin = new TransactionLite(BigDecimal.valueOf(30), TransactionType.EINZAHLUNG, null, SENDER_USERNAME, SUBJECT);
        TransactionLite payout = new TransactionLite(BigDecimal.valueOf(20), TransactionType.AUSZAHLUNG, SENDER_USERNAME, null, SUBJECT);
        TransactionLite transfer = new TransactionLite(BigDecimal.valueOf(50), TransactionType.UEBERWEISUNG, SENDER_USERNAME, RECIPIENT_USERNAME, SUBJECT);
        final List<TransactionLite> transactionsLite = List.of(payin, payout, transfer, transfer, payin);
        final int balance = 0;

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(balance));

        assertThrows(TransactionException.class, () -> underTest.sendBulkTransfers(transactionsLite, sender));
    }


}
