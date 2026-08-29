package com.DBArena.services.execution.validation;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.execution.engine.UnsupportedEngineException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SpringQueryValidatorRegistry implements QueryValidatorRegistry {

    private final Map<EngineType, QueryValidator> validatorsByType;

    public SpringQueryValidatorRegistry(List<QueryValidator> validators) {
        this.validatorsByType = validators.stream()
                .collect(Collectors.toMap(QueryValidator::engineType, Function.identity()));
    }

    @Override
    public QueryValidator resolve(EngineType type) {
        QueryValidator validator = validatorsByType.get(type);
        if (validator == null) {
            throw new UnsupportedEngineException(type);
        }
        return validator;
    }
}
