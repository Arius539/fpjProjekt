package org.fpj.payments.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;

@Getter
@Setter
public class TransactionViewSearchParameter {

    private Long currentUserID;
    private String description;
    private Instant createdFrom;
    private Instant createdTo;
    private String senderRecipientUsername;
    private BigDecimal amountFrom;
    private BigDecimal amountTo;

    public TransactionViewSearchParameter(
            Long currentUserID,
            String description,
            Instant createdFrom,
            Instant createdTo,
            String senderRecipientUsername,
            BigDecimal amountFrom,
            BigDecimal amountTo) {
        this.currentUserID = currentUserID;
        this.description = description;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.senderRecipientUsername = senderRecipientUsername;
        this.amountFrom = amountFrom;
        this.amountTo = amountTo;
    }

    public static TransactionViewSearchParameter fromLocalDate(
            Long currentUserID,
            String description,
            LocalDate createdFromDate,
            LocalDate createdToDate,
            String senderRecipientUsername,
            BigDecimal amountFrom,
            BigDecimal amountTo) {
        Instant createdFrom = createdFromDate != null
                ? createdFromDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;

        Instant createdTo = createdToDate != null
                ? createdToDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant()
                : null;

        return new TransactionViewSearchParameter(
                currentUserID,
                description,
                createdFrom,
                createdTo,
                senderRecipientUsername,
                amountFrom,
                amountTo
        );
    }
    public void setCreatedFrom(LocalDate date) {
        this.createdFrom = date != null
                ? date.atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;
    }

    public void setCreatedTo(LocalDate date) {
        this.createdTo = date != null
                ? date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant()
                : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionViewSearchParameter that = (TransactionViewSearchParameter) o;
        return Objects.equals(currentUserID, that.currentUserID)
                && Objects.equals(description, that.description)
                && Objects.equals(createdFrom, that.createdFrom)
                && Objects.equals(createdTo, that.createdTo)
                && Objects.equals(senderRecipientUsername, that.senderRecipientUsername)
                && Objects.equals(amountFrom, that.amountFrom)
                && Objects.equals(amountTo, that.amountTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                currentUserID,
                description,
                createdFrom,
                createdTo,
                senderRecipientUsername,
                amountFrom,
                amountTo
        );
    }

    public TransactionViewSearchParameter copy() {
        return new TransactionViewSearchParameter(
                this.currentUserID,
                this.description,
                this.createdFrom,
                this.createdTo,
                this.senderRecipientUsername,
                this.amountFrom,
                this.amountTo
        );
    }

}

