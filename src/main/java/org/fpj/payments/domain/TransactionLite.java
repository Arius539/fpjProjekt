package org.fpj.payments.domain;

import org.fpj.util.UiHelpers;

import java.math.BigDecimal;

public record TransactionLite(
        java.math.BigDecimal amount,
        TransactionType type,
        String senderUsername,
        String recipientUsername,
        String description
) {

    public String amountString(String currentUsername) {
        boolean outgoing = this.senderUsername() ==null? false : this.senderUsername() == currentUsername;
        return UiHelpers.formatSignedEuro(!outgoing ? this.amount():new BigDecimal("0").subtract(this.amount()));
    }

    public boolean isOutgoing(String currentUsername){
        if(recipientUsername==null) return true;
        return !this.recipientUsername().equals(currentUsername);
    }

    public String amountStringUnsigned() {
        return UiHelpers.formatUnsignedEuro( this.amount());
    }

    public static TransactionLite fromTransactionRow(TransactionRow row){
        return new TransactionLite(row.amount(), row.type(), row.senderUsername(), row.recipientUsername(), row.description());
    }
}