package org.fpj.payments.domain;

import org.fpj.util.UiHelpers;

import java.math.BigDecimal;
import java.util.Objects;

public record TransactionLite(
        java.math.BigDecimal amount,
        TransactionType type,
        String senderUsername,
        String recipientUsername,
        String description
) {

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionLite other = (TransactionLite) o;
        return amount.compareTo(other.amount) == 0 && type.equals(other.type) && senderUsername.equals(other.senderUsername)
                && recipientUsername.equals(other.recipientUsername) && description.equals(other.description);
    }

    @Override
    public int hashCode(){
        return Objects.hash(amount == null ? null : amount.stripTrailingZeros(), type, senderUsername, recipientUsername, description);
    }

    public String amountString(String currentUsername) {
        boolean outgoing = this.senderUsername() ==null? false : this.senderUsername() == currentUsername;
        return UiHelpers.formatSignedEuro(!outgoing ? this.amount():new BigDecimal("0").subtract(this.amount()));
    }

    public boolean isOutgoing(String currentUsername){
        if(recipientUsername==null) return true;
        return !this.recipientUsername().equals(currentUsername);
    }

    public String amountStringUnsigned() {
        return UiHelpers.formatEuro( this.amount());
    }

    public static TransactionLite fromTransactionRow(TransactionRow row){
        return new TransactionLite(row.amount(), row.type(), row.senderUsername(), row.recipientUsername(), row.description());
    }
}