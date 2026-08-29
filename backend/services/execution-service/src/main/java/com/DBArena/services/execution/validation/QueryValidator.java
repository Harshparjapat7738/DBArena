package com.DBArena.services.execution.validation;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.execution.domain.ExecutionPolicy;

/**
 * Hard rule #4: statement validation is AST-based - JSqlParser for SQL, a
 * restricted command AST for MongoDB (not built here; Mongo has no adapter
 * yet - B05). Regex-based filtering of user SQL is forbidden. This is the
 * single choke point every statement passes through before a
 * {@link com.DBArena.engine.spi.model.StatementRequest} is ever built - see
 * that record's own Javadoc, which says exactly this.
 */
public interface QueryValidator {

    EngineType engineType();

    ValidationResult validate(String statementText, ExecutionPolicy policy);
}
