package com.DBArena.services.execution.validation;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * "Safe practice SQL only; destructive/admin operations disabled" (the
 * task brief), enforced entirely on the parsed AST, never the raw text
 * (hard rule #4):
 *
 * <ol>
 *   <li>length cap on the raw text (a cheap, pre-parse rejection - not
 *       itself the security boundary, just resource hygiene)</li>
 *   <li>must parse as exactly one statement - blocks stacked-query
 *       injection ({@code SELECT 1; DROP TABLE x;})</li>
 *   <li>that one statement must be a {@link Select} with no
 *       {@code INTO} target - blocks every data-modifying/DDL statement
 *       type (INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/GRANT/CREATE/COPY/...)
 *       in one check, and blocks {@code SELECT ... INTO new_table}, which
 *       would otherwise sneak table creation in through a Select</li>
 *   <li>no reference to a denied schema ({@link DeniedSqlSurface#DENIED_SCHEMAS})
 *       - blocks catalog/metadata reconnaissance</li>
 *   <li>no call to a denied function ({@link DeniedSqlSurface#DENIED_FUNCTIONS})
 *       - blocks artificial delay/DoS, file/process/network-egress
 *       functions, found via {@link FunctionCallCollector}, not string
 *       matching</li>
 * </ol>
 *
 * <p>A statement that fails to parse at all is rejected, not treated as an
 * error to surface differently - a syntax error a learner needs to see is
 * exactly what {@link com.DBArena.engine.spi.DatabaseEngineAdapter#execute}
 * would normally report, but this validator runs first and would parse it
 * successfully too if it were valid SQL; if JSqlParser can't parse it,
 * Postgres would almost certainly reject it too, so failing closed here
 * costs nothing real.
 */
@Component
public class PostgresSqlQueryValidator implements QueryValidator {

    @Override
    public EngineType engineType() {
        return EngineType.POSTGRES;
    }

    @Override
    public ValidationResult validate(String statementText, ExecutionPolicy policy) {
        if (statementText == null || statementText.isBlank()) {
            return ValidationResult.reject("statement must not be blank");
        }
        if (statementText.length() > policy.maxStatementLength()) {
            return ValidationResult.reject(
                    "statement exceeds the maximum allowed length of " + policy.maxStatementLength() + " characters");
        }

        Statements parsed;
        try {
            parsed = CCJSqlParserUtil.parseStatements(statementText);
        } catch (JSQLParserException e) {
            return ValidationResult.reject("statement is not valid SQL");
        }

        List<Statement> statements = parsed.getStatements();
        if (statements.size() != 1) {
            return ValidationResult.reject("only a single statement is allowed - no stacked/multiple statements");
        }

        Statement statement = statements.get(0);
        if (!(statement instanceof Select select)) {
            return ValidationResult.reject(
                    "only SELECT statements are allowed - destructive/admin operations are disabled");
        }
        if (select instanceof PlainSelect plainSelect
                && plainSelect.getIntoTables() != null && !plainSelect.getIntoTables().isEmpty()) {
            return ValidationResult.reject("SELECT ... INTO is not allowed - it creates a table");
        }

        ValidationResult schemaResult = rejectDeniedSchemas(select);
        if (!schemaResult.allowed()) {
            return schemaResult;
        }

        return rejectDeniedFunctions(select);
    }

    private static ValidationResult rejectDeniedSchemas(Select select) {
        // getTableList is overloaded for Statement and Expression, both of which Select implements -
        // the (Statement) cast is required to disambiguate, not decorative.
        List<String> tables = new net.sf.jsqlparser.util.TablesNamesFinder().getTableList((Statement) select);
        for (String table : tables) {
            String schemaCandidate = table.contains(".")
                    ? table.substring(0, table.indexOf('.')).toLowerCase(Locale.ROOT)
                    : "";
            if (DeniedSqlSurface.DENIED_SCHEMAS.contains(schemaCandidate)) {
                return ValidationResult.reject("access to schema '" + schemaCandidate + "' is not allowed");
            }
        }
        return ValidationResult.allow();
    }

    private static ValidationResult rejectDeniedFunctions(Select select) {
        for (String function : new FunctionCallCollector().collect(select)) {
            if (DeniedSqlSurface.DENIED_FUNCTIONS.contains(function)) {
                return ValidationResult.reject("function '" + function + "' is not allowed");
            }
        }
        return ValidationResult.allow();
    }
}
