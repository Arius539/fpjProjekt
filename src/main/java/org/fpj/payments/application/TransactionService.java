package org.fpj.payments.application;

import org.fpj.exceptions.TransactionException;
import org.fpj.payments.domain.*;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.fpj.users.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.fpj.util.UiHelpers.parseAmountTolerant;
import static org.fpj.util.UiHelpers.safe;
import static org.fpj.payments.domain.TransactionType.*;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepo;

    @Transactional(readOnly = true)
    public Page<TransactionRow> findLiteItemsForUser(long userId, int page, int size) {
        Pageable pr = PageRequest.of(page, size);
        return txRepo.findRowsForUser(userId, pr);
    }

    @Transactional(readOnly = true)
    public BigDecimal computeBalance(long userId) {
        BigDecimal b = txRepo.computeBalance(userId);
        return b != null ? b : BigDecimal.ZERO;
    }

    public List<TransactionRow> transactionsForUserAsList(long userId) {
        return this.txRepo.findRowsForUserList(userId);
    }

    private Transaction deposit(User user, BigDecimal amount, String subject) {
        Transaction tx = new Transaction();
        tx.setTransactionType(EINZAHLUNG);
        tx.setRecipient(user);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
        return tx;
    }

    private Transaction withdraw(User user, BigDecimal amount, String subject) {
        Transaction tx = new Transaction();
        tx.setTransactionType(AUSZAHLUNG);
        tx.setSender(user);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
        return tx;
    }

    private Transaction transfer(User sender, String recipientUsername, BigDecimal amount, String subject) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new TransactionException("Empfänger ist erforderlich.");
        }
        User recipient = userService.findByUsername(recipientUsername);

        if (recipient.getId().equals(sender.getId())) {
            throw new TransactionException("Überweisung an sich selbst ist nicht erlaubt.");
        }

        requirePositive(amount, "Der Betrag muss größer als 0 sein.");

        Transaction tx = new Transaction();
        tx.setTransactionType(UEBERWEISUNG);
        tx.setSender(sender);
        tx.setRecipient(recipient);
        tx.setAmount(amount);
        tx.setDescription(subject);
        tx.setCreatedAt(java.time.Instant.now());
        txRepo.save(tx);
        return tx;
    }

    public Page<TransactionRow> searchTransactions(TransactionViewSearchParameter parameter, int page, int size) {
        String senderRecipientPattern = parameter.getSenderRecipientUsername() == null ? null : "%" + parameter.getSenderRecipientUsername().toLowerCase() + "%";
        String descriptionPattern = parameter.getDescription() == null ? null : "%" + parameter.getDescription().toLowerCase() + "%";
        Pageable pr = PageRequest.of(page, size);
        return txRepo.searchTransactions(
                parameter.getCurrentUserID(),
                parameter.getCreatedFrom(),
                parameter.getCreatedTo(),
                senderRecipientPattern,
                parameter.getAmountFrom(),
                parameter.getAmountTo(),
                descriptionPattern,
                pr
        );
    }

    public TransactionLite transactionInfosToTransactionLite(String amountIn, String senderUsername, String recipientUsername, String description, TransactionType type) {
        BigDecimal amount = parseAmountTolerant(amountIn);
        String recipient = safe(recipientUsername);
        if (type == TransactionType.UEBERWEISUNG) {
            if (recipient.equals(senderUsername)) throw new TransactionException("Du kannst keine Überweisungen an dich selbst ausführen.");
            userService.findByUsername(recipient);
        } else if (type != AUSZAHLUNG && type != EINZAHLUNG){
            throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
        }
        return new TransactionLite(amount, type, senderUsername, recipientUsername, description);
    }

    @Transactional
    public TransactionResult sendTransfers(TransactionLite transactionLite, User currentUser) {
        userRepo.lockById(currentUser.getId()).orElseThrow();

        BigDecimal currentBalance = this.computeBalance(currentUser.getId());
        Transaction transaction;
        if (transactionLite.type() == TransactionType.EINZAHLUNG) {
            transaction = this.deposit(currentUser, transactionLite.amount(), transactionLite.description());
            currentBalance = currentBalance.add(transactionLite.amount());
        } else if (transactionLite.type() == TransactionType.AUSZAHLUNG) {
            if (currentBalance.compareTo(transactionLite.amount()) < 0) throw new TransactionException("Nicht genügend Guthaben für die Auszahlung.");
            currentBalance = currentBalance.subtract(transactionLite.amount());
            transaction = this.withdraw(currentUser, transactionLite.amount(), transactionLite.description());
        } else {
            if (currentBalance.compareTo(transactionLite.amount()) < 0) throw new TransactionException("Nicht genügend Guthaben für die Überweisung.");
            currentBalance = currentBalance.subtract(transactionLite.amount());

            if (transactionLite.recipientUsername() == null || transactionLite.recipientUsername().isBlank()) {
                throw new TransactionException("Empfänger ist erforderlich.");
            }
            transaction = this.transfer(currentUser, transactionLite.recipientUsername(), transactionLite.amount(), transactionLite.description());
        }
        return new TransactionResult(transaction, currentBalance);
    }


    @Transactional
    public ArrayList<TransactionResult> sendBulkTransfers(List<TransactionLite> transactionsLite, User currentUser) {
        if(transactionsLite.isEmpty()) throw new IllegalArgumentException("Die Transaktionsliste ist leer, es können keine Transaktionen ausgeführt werden");
        ArrayList<TransactionResult> results =  new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (TransactionLite lite : transactionsLite) {
            if (lite.type() == TransactionType.EINZAHLUNG) {
                sum = sum.subtract(lite.amount());
            } else {
                sum = sum.add(lite.amount());
            }
        }
        userRepo.lockById(currentUser.getId()).orElseThrow();
        BigDecimal balance = this.computeBalance(currentUser.getId());
        if(balance.compareTo(sum) < 0) throw new TransactionException( "Dein Kontostand ist zu gering um die Transaktionen auszuführen");

        balance = balance.subtract(sum);
        for (TransactionLite lite : transactionsLite) {
            Transaction transaction = sendTransfersWithoutBalanceCheck(lite, currentUser);
            results.add(new TransactionResult(transaction, balance));
        }
        if(results.isEmpty()) throw new IllegalArgumentException("Es gab ein Problem beim Ausführen deiner Transaktionen");
        return results;
    }

    private Transaction sendTransfersWithoutBalanceCheck(TransactionLite transactionLite,User currentUser) {
        Transaction transaction;
        if (transactionLite.type() == TransactionType.EINZAHLUNG) {
            transaction = this.deposit(currentUser, transactionLite.amount(), transactionLite.description());
        } else if (transactionLite.type()== TransactionType.AUSZAHLUNG) {
            transaction = this.withdraw(currentUser, transactionLite.amount(), transactionLite.description());
        } else {
            transaction = this.transfer(currentUser, transactionLite.recipientUsername(), transactionLite.amount(), transactionLite.description());
        }
        return transaction;
    }

    public BigDecimal findUserBalanceAfterTransaction(long userId, long transactionId){
       return txRepo.findUserBalanceAfterTransaction(userId, transactionId);
    }

    private void requirePositive(BigDecimal amt, String msg) {
        if (amt.signum() <= 0) throw new TransactionException(msg);
    }

}
