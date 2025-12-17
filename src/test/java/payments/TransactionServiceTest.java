package payments;

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
    @Mock
    TransactionLite transactionLite;


    @InjectMocks
    private TransactionService underTest = new TransactionService();


    private static final String RECIPIENT_USERNAME = "testusername2@web.de";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(120);
    private static final String SUBJECT = "Subject";
    private static final Long SENDER_ID = 582173450L;

    @Test
    public void testSendTransfersPayin(){
        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(200));
        when(transactionLite.type()).thenReturn(TransactionType.EINZAHLUNG);
        when(transactionLite.amount()).thenReturn(AMOUNT);
        when(transactionLite.description()).thenReturn(SUBJECT);

        Transaction expectedTransaction = new Transaction(null, TransactionType.EINZAHLUNG, AMOUNT, null, sender, SUBJECT, null);
        BigDecimal expectedCurrentBalance = BigDecimal.valueOf(200).add(AMOUNT);

        TransactionResult actual = underTest.sendTransfers(transactionLite, sender);

        assertEquals(expectedTransaction, actual.transaction());
        assertEquals(expectedCurrentBalance, actual.newBalance());
    }

    @Test
    public void testSendTransfersPayout(){
        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(200));
        when(transactionLite.type()).thenReturn(TransactionType.AUSZAHLUNG);
        when(transactionLite.amount()).thenReturn(AMOUNT);
        when(transactionLite.description()).thenReturn(SUBJECT);

        Transaction expectedTransaction = new Transaction(null, TransactionType.AUSZAHLUNG, AMOUNT, sender, null, SUBJECT, null);
        BigDecimal expectedCurrentBalance = BigDecimal.valueOf(200).subtract(AMOUNT);

        TransactionResult actual = underTest.sendTransfers(transactionLite, sender);

        assertEquals(expectedTransaction, actual.transaction());
        assertEquals(expectedCurrentBalance, actual.newBalance());
    }

    @Test
    public void testSendTransfersPayoutFailed(){
        final int senderBalanceDeficient = 100;

        when(sender.getId()).thenReturn(SENDER_ID);
        when(userRepo.lockById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(txRepo.computeBalance(SENDER_ID)).thenReturn(BigDecimal.valueOf(senderBalanceDeficient));
        when(transactionLite.type()).thenReturn(TransactionType.AUSZAHLUNG);
        when(transactionLite.amount()).thenReturn(AMOUNT);

        assertThrows(TransactionException.class, () -> underTest.sendTransfers(transactionLite, sender));
    }






}
