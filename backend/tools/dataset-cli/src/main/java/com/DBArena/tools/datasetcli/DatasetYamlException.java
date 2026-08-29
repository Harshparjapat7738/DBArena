package com.dbforge.tools.datasetcli;

/**
 * A dataset.yaml file could not be parsed into the CDM model at all - a
 * missing required field, an unknown {@code type} name, a malformed
 * timestamp string. Distinct from a {@code CdmValidationResult} failure:
 * this means the file couldn't even become a well-formed {@code CdmDataset}
 * to validate, so {@link DatasetCli} reports it with a different exit code.
 */
public class DatasetYamlException extends RuntimeException {

    public DatasetYamlException(String message) {
        super(message);
    }

    public DatasetYamlException(String message, Throwable cause) {
        super(message, cause);
    }
}
