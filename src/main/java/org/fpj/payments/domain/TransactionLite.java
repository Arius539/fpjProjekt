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

    public String amountStringSigned(String currentUsername) {
        boolean outgoing = isOutgoing(currentUsername);
        return UiHelpers.formatAmount(!outgoing ? this.amount():new BigDecimal("0").subtract(this.amount()), true,true, true, ',', true, '\0', false);
    }

    public boolean isOutgoing(String currentUsername){
        if(recipientUsername==null) return true;
        return !this.recipientUsername().equals(currentUsername);
    }

    public String amountStringUnsigned() {
        return UiHelpers.formatAmount(this.amount(), false,false, true, ',', true, '\0', false);
    }

    public static TransactionLite fromTransactionRow(TransactionRow row){
        return new TransactionLite(row.amount(), row.type(), row.senderUsername(), row.recipientUsername(), row.description());
    }
}