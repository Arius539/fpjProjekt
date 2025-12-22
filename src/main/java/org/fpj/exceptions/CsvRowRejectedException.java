package org.fpj.exceptions;

public class CsvRowRejectedException extends RuntimeException {
    public CsvRowRejectedException(String message) {
        super(message);
    }
}
