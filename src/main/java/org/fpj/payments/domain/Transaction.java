package org.fpj.payments.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;
import org.fpj.util.UiHelpers;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.fpj.users.domain.User;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "trx_sender_idx", columnList = "sender_id"),
                @Index(name = "trx_recipient_idx", columnList = "recipient_id"),
                @Index(name = "trx_sender_ctime", columnList = "sender_id,created_at"),
                @Index(name = "trx_recipient_ctime", columnList = "recipient_id,created_at")
        }
)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, columnDefinition = "transaction_type_enum")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TransactionType transactionType;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sender", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "recipient", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
    private User recipient;

    @Column(nullable = false)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public String amountString(long currentUserId) {
        boolean outgoing = this.sender != null && this.sender.getId() == currentUserId;
        return UiHelpers.formatSignedEuro(!outgoing ? this.amount : new BigDecimal("0").subtract(this.amount));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction transaction = (Transaction) obj;
        boolean basic = transactionType.equals(transaction.getTransactionType()) && amount.equals(transaction.getAmount())
                && description.equals(transaction.getDescription());
        boolean equalUsers;
        if (transactionType == TransactionType.EINZAHLUNG){
            equalUsers = recipient.equals(transaction.recipient);
        } else if (transactionType == TransactionType.AUSZAHLUNG){
            equalUsers = sender.equals(transaction.sender);
        } else {
            equalUsers = recipient.equals(transaction.recipient) && sender.equals(transaction.sender);
        }
        return basic && equalUsers;
    }

    @Override
    public String toString(){
        String string = "Transaktion{TransactionType= " + transactionType.toString() + ", Amount= " + amount.toString();
        if (transactionType == TransactionType.EINZAHLUNG){
            string = string + ", Recipient= " + recipient.toString() + ", Descripion= " + description;
        } else if (transactionType == TransactionType.AUSZAHLUNG){
            string = string + ", Sender= " + sender.toString() + ", Descripion= " + description;
        } else {
            string = string + ", Sender= " + sender.toString() + ", Recipient= " + recipient.toString() + ", Descripion= " + description;
        }

        if (null != createdAt){
            string = string + ", CreatedAt= " + createdAt;
        }
        return string;
    }

}
