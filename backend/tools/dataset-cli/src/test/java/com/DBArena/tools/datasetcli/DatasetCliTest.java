package com.DBArena.tools.datasetcli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetCliTest {

    private static final String VALID_YAML = """
            datasetId: two-sum
            name: Two Sum
            schemaVersion: 1
            entities:
              - name: numbers
                columns:
                  - name: id
                    type: INTEGER
                    nullable: false
                    primaryKey: true
                  - name: value
                    type: INTEGER
                    nullable: false
                seedRows:
                  - id: 1
                    value: 2
                  - id: 2
                    value: 7
            """;

    // Valid YAML, but a real CDM problem: duplicate primary-key values.
    private static final String INVALID_YAML = """
            datasetId: broken
            name: Broken Dataset
            schemaVersion: 1
            entities:
              - name: numbers
                columns:
                  - name: id
                    type: INTEGER
                    nullable: false
                    primaryKey: true
                  - name: value
                    type: INTEGER
                    nullable: false
                seedRows:
                  - id: 1
                    value: 2
                  - id: 1
                    value: 99
            """;

    private static final String MALFORMED_YAML = """
            datasetId: t
            name: T
            schemaVersion: 1
            entities:
              - name: things
                columns:
                  - name: id
                    type: NOT_A_TYPE
                    nullable: false
                    primaryKey: true
            """;

    @Test
    void validDatasetExitsZero(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("dataset.yaml");
        Files.writeString(file, VALID_YAML);

        Result result = runCli("validate", file.toString());

        assertThat(result.exitCode).isZero();
        assertThat(result.stdout).contains("OK").contains("two-sum");
        assertThat(result.stderr).isEmpty();
    }

    @Test
    void datasetWithValidationProblemsExitsOneAndListsThem(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("dataset.yaml");
        Files.writeString(file, INVALID_YAML);

        Result result = runCli("validate", file.toString());

        assertThat(result.exitCode).isEqualTo(1);
        assertThat(result.stderr).contains("INVALID").contains("duplicate primary-key");
    }

    @Test
    void malformedYamlExitsTwo(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("dataset.yaml");
        Files.writeString(file, MALFORMED_YAML);

        Result result = runCli("validate", file.toString());

        assertThat(result.exitCode).isEqualTo(2);
        assertThat(result.stderr).contains("NOT_A_TYPE");
    }

    @Test
    void aMissingFileExitsTwoWithAClearMessage(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.yaml");

        Result result = runCli("validate", missing.toString());

        assertThat(result.exitCode).isEqualTo(2);
        assertThat(result.stderr).contains("could not read");
    }

    @Test
    void wrongUsageExitsTwo() {
        Result result = runCli("bogus-subcommand");

        assertThat(result.exitCode).isEqualTo(2);
        assertThat(result.stderr).contains("usage:");
    }

    private static Result runCli(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        int exitCode = DatasetCli.run(args, new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));
        return new Result(exitCode, outBytes.toString(StandardCharsets.UTF_8), errBytes.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) {
    }
}
