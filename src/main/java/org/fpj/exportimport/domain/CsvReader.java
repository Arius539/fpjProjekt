package org.fpj.exportimport.domain;

import java.io.InputStream;

public interface CsvReader<T> {
    CsvImportResult<T> parse(InputStream in) throws Exception;

    boolean isRunning();
}
