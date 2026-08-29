"use client";

import type { EngineKind } from "@DBArena/api-client";
import { Tabs } from "@DBArena/ui";
import { useState } from "react";
import { EngineBadge } from "@/features/catalog/badges";
import type { DatasetEntity } from "@/lib/mock/types";
import { SchemaTable } from "./SchemaTable";

/**
 * The "same dataset, three engines" story made visible: switching the tab
 * re-labels the exact same entities (table -> collection, column -> field
 * for MongoDB) rather than fetching or rendering different data - because
 * there isn't different data, that's the whole point of the CDM.
 */
export function DatasetEngineTabs({ entities, engines }: { entities: DatasetEntity[]; engines: EngineKind[] }) {
  const [engine, setEngine] = useState<EngineKind>(engines[0] ?? "POSTGRES");

  return (
    <div className="flex flex-col gap-4">
      <Tabs
        items={engines.map((e) => ({ value: e, label: <EngineBadge engine={e} /> }))}
        value={engine}
        onChange={(v) => setEngine(v as EngineKind)}
      />
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {entities.map((entity) => (
          <SchemaTable key={entity.name} entity={entity} engine={engine} />
        ))}
      </div>
    </div>
  );
}
