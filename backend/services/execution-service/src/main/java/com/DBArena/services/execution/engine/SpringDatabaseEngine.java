package com.DBArena.services.execution.engine;

import com.DBArena.engine.spi.DatabaseEngineAdapter;
import com.DBArena.engine.spi.EngineType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Wires every {@link DatabaseEngineAdapter} bean Spring knows about into a lookup-by-{@link EngineType} map - registering a new adapter (e.g. a future adapter-mongo) needs nothing here, just a new bean. */
@Component
public class SpringDatabaseEngine implements DatabaseEngine {

    private final Map<EngineType, DatabaseEngineAdapter> adaptersByType;

    public SpringDatabaseEngine(List<DatabaseEngineAdapter> adapters) {
        this.adaptersByType = adapters.stream()
                .collect(Collectors.toMap(DatabaseEngineAdapter::engineType, Function.identity()));
    }

    @Override
    public DatabaseEngineAdapter resolve(EngineType type) {
        DatabaseEngineAdapter adapter = adaptersByType.get(type);
        if (adapter == null) {
            throw new UnsupportedEngineException(type);
        }
        return adapter;
    }
}
