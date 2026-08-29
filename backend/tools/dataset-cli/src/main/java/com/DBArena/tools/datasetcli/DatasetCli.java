package com.dbforge.tools.datasetcli;

import com.dbforge.common.core.error.FieldViolation;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmDatasetValidator;
import com.dbforge.engine.spi.cdm.CdmValidationResult;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * {@code mvn -pl backend/tools/dataset-cli exec:java -Dexec.args="validate <path>"}
 * per backend/CLAUDE.md's Commands section. Only a {@code validate}
 * subcommand exists in this milestone (B02's name is "CDM model +
 * validator") - author/generate/materialize subcommands mentioned in root
 * CLAUDE.md's repository-layout one-liner belong to later milestones (an
 * author currently just hand-writes YAML; generate/materialize are
 * B04/B05's engine adapters).
 *
 * <p>Exit codes: {@code 0} the dataset is valid; {@code 1} it parsed but
 * {@link CdmDatasetValidator} found real problems with it; {@code 2} usage
 * error, missing file, or the YAML itself couldn't become a well-formed
 * dataset at all.
 */
public final class DatasetCli {

    private DatasetCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Package-visible so tests can capture output and an exit code without terminating the JVM. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 2 || !"validate".equals(args[0])) {
            err.println("usage: dataset-cli validate <path-to-dataset.yaml>");
            return 2;
        }

        Path path = Path.of(args[1]);
        CdmDataset dataset;
        try {
            dataset = CdmDatasetLoader.load(path);
        } catch (DatasetYamlException e) {
            err.println("error: " + e.getMessage());
            return 2;
        } catch (IOException e) {
            err.println("error: could not read '" + path + "': " + e.getMessage());
            return 2;
        }

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);
        if (result.valid()) {
            out.println("OK: '" + dataset.datasetId() + "' is valid ("
                    + dataset.entities().size() + " entit" + (dataset.entities().size() == 1 ? "y" : "ies") + ").");
            return 0;
        }

        err.println("INVALID: '" + dataset.datasetId() + "' has " + result.violations().size() + " problem(s):");
        for (FieldViolation violation : result.violations()) {
            err.println("  - " + violation.field() + ": " + violation.message());
        }
        return 1;
    }
}
