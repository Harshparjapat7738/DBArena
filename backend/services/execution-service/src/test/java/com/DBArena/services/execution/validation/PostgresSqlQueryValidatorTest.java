package com.DBArena.services.execution.validation;

import com.DBArena.services.execution.domain.ExecutionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The malicious-query suite the B04 task brief asks for. Every rejection
 * case here is a real attack shape a learner (or an adversarial script)
 * could actually submit; every allow case is real, useful practice SQL a
 * false-positive here would break.
 */
class PostgresSqlQueryValidatorTest {

    private final PostgresSqlQueryValidator validator = new PostgresSqlQueryValidator();
    private final ExecutionPolicy policy = new ExecutionPolicy(
            5000, 500, 1_048_576, Duration.ofSeconds(5), Duration.ofSeconds(3), 2, 8, 3);

    // --- allowed: real practice SQL --------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1",
            "SELECT * FROM orders",
            "SELECT id, name FROM customers WHERE age > 18",
            "SELECT o.id, c.name FROM orders o JOIN customers c ON o.customer_id = c.id",
            "SELECT customer_id, SUM(amount) FROM orders GROUP BY customer_id HAVING SUM(amount) > 100",
            "SELECT * FROM orders ORDER BY created_at DESC LIMIT 10",
            "SELECT * FROM (SELECT id FROM customers WHERE age > 18) AS adults",
            "WITH totals AS (SELECT customer_id, SUM(amount) AS total FROM orders GROUP BY customer_id) SELECT * FROM totals WHERE total > 50",
            "SELECT * FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE country = 'US')",
            "SELECT * FROM orders UNION SELECT * FROM archived_orders",
            "SELECT UPPER(name), COUNT(*) FROM customers GROUP BY UPPER(name)",
    })
    void allowsRealPracticeSql(String sql) {
        ValidationResult result = validator.validate(sql, policy);
        assertThat(result.allowed()).as("expected '%s' to be allowed, reason: %s", sql, result.rejectionReason()).isTrue();
    }

    // --- rejected: data modification / DDL / admin -----------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO orders (id) VALUES (1)",
            "UPDATE orders SET amount = 0",
            "DELETE FROM orders",
            "DROP TABLE orders",
            "ALTER TABLE orders ADD COLUMN hacked TEXT",
            "TRUNCATE orders",
            "CREATE TABLE evil (id INT)",
            "GRANT ALL ON orders TO PUBLIC",
            "COPY orders TO '/tmp/dump.csv'",
    })
    void rejectsDestructiveAndAdminStatements(String sql) {
        ValidationResult result = validator.validate(sql, policy);
        assertThat(result.allowed()).as("expected '%s' to be rejected", sql).isFalse();
        assertThat(result.rejectionReason()).isPresent();
    }

    @Test
    void rejectsSelectInto() {
        ValidationResult result = validator.validate("SELECT * INTO new_table FROM orders", policy);
        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectionReason().orElseThrow()).containsIgnoringCase("into");
    }

    @Test
    void rejectsStackedQueries() {
        ValidationResult result = validator.validate("SELECT 1; DROP TABLE orders;", policy);
        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectionReason().orElseThrow()).containsIgnoringCase("single statement");
    }

    @Test
    void rejectsStackedQueriesEvenWhenBothHalvesAreSelects() {
        ValidationResult result = validator.validate("SELECT 1; SELECT 2;", policy);
        assertThat(result.allowed()).isFalse();
    }

    // --- rejected: denied functions (AST-based, not string matching) -----------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT pg_sleep(999999)",
            "SELECT * FROM orders WHERE pg_sleep(5) IS NOT NULL",
            "SELECT (SELECT pg_sleep(5))",
            "SELECT PG_SLEEP(5)", // case-insensitivity
            "SELECT pg_catalog.pg_sleep(5)", // schema-qualified evasion attempt
            "SELECT pg_read_file('/etc/passwd')",
            "SELECT lo_import('/etc/passwd')",
            "SELECT dblink_connect('host=evil.example.com')",
            "SELECT pg_terminate_backend(1)",
    })
    void rejectsDeniedFunctionCallsWhereverTheyAppearInTheAst(String sql) {
        ValidationResult result = validator.validate(sql, policy);
        assertThat(result.allowed()).as("expected '%s' to be rejected", sql).isFalse();
    }

    // --- rejected: denied schemas -----------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM information_schema.tables",
            "SELECT * FROM pg_catalog.pg_class",
            "SELECT relname FROM pg_catalog.pg_class WHERE relkind = 'r'",
    })
    void rejectsAccessToDeniedSchemas(String sql) {
        ValidationResult result = validator.validate(sql, policy);
        assertThat(result.allowed()).as("expected '%s' to be rejected", sql).isFalse();
    }

    // --- rejected: malformed / oversized -----------------------------------------------------

    @Test
    void rejectsBlankStatement() {
        assertThat(validator.validate("   ", policy).allowed()).isFalse();
    }

    @Test
    void rejectsInvalidSyntax() {
        ValidationResult result = validator.validate("SELECT FROM WHERE ;;; garbage", policy);
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void rejectsStatementsLongerThanThePolicyLimit() {
        String longStatement = "SELECT '" + "a".repeat(policy.maxStatementLength()) + "'";
        ValidationResult result = validator.validate(longStatement, policy);
        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectionReason().orElseThrow()).containsIgnoringCase("length");
    }

    @Test
    void engineTypeIsPostgres() {
        assertThat(validator.engineType()).isEqualTo(com.DBArena.engine.spi.EngineType.POSTGRES);
    }
}
