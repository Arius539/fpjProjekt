package org.fpj.payments.domain;

import org.fpj.util.UiHelpers;

import java.math.BigDecimal;

public record TransactionRow(
        Long id,
        java.math.BigDecimal amount,
        java.time.Instant createdAt,
        TransactionType type,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String recipientUsername,
        String description
) {

    public String amountString(long currentUserId) {
       return UiHelpers.formatSignedEuro(!this.isOutgoing(currentUserId) ? this.amount():new BigDecimal("0").subtract(this.amount()));
    }

    public String amountStringUnsigned() {
        return UiHelpers.formatUnsignedEuro( this.amount());
    }

    public boolean isOutgoing(long currentUserId){
        if(recipientId==null) return true;
        return !this.recipientId().equals(currentUserId);
    }

    public static TransactionRow fromTransaction(Transaction t) {
        Long sId = t.getSender()== null ? null : t.getSender().getId();
        Long rId = t.getRecipient()== null ? null : t.getRecipient().getId();
        String sName = t.getSender()== null ? null : t.getSender().getUsername();
        String rName = t.getRecipient()== null ? null : t.getRecipient().getUsername();
        return new TransactionRow(t.getId(),t.getAmount(),t.getCreatedAt(), t.getTransactionType(),sId, sName, rId, rName, t.getDescription());
    }
}