package org.fpj.payments.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record MassTransfer(
        String empfaenger,
        BigDecimal betrag,
        String beschreibung
) {
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof MassTransfer(String empfaenger1, BigDecimal betrag1, String beschreibung1))) return false;

        return Objects.equals(empfaenger, empfaenger1)
                && Objects.equals(beschreibung, beschreibung1)
                && (betrag == null ? betrag1 == null : betrag.compareTo(betrag1) == 0);
    }

    @Override
    public int hashCode(){
        return Objects.hash(empfaenger, beschreibung, betrag == null ? null : betrag.stripTrailingZeros());
    }

}
