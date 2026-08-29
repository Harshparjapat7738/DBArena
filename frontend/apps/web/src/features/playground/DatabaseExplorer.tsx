"use client";

import type { EngineKind } from "@DBArena/api-client";
import {
  ChevronDown,
  ChevronRight,
  Columns3,
  Database as DatabaseIcon,
  KeyRound,
  Layers,
  Table2,
} from "lucide-react";
import { useState } from "react";
import type { Dataset } from "@/lib/mock/types";

/**
 * DataGrip-style schema tree: Database > Schema > Tables/Collections >
 * Columns/Fields, all collapsible. Read-only (browsing, not DDL editing) -
 * this platform's workbench is for querying a materialized practice
 * dataset, not administering a real server.
 */
export function DatabaseExplorer({ dataset, engine }: { dataset: Dataset; engine: EngineKind }) {
  const entityLabel = engine === "MONGODB" ? "Collections" : "Tables";

  return (
    <div className="flex h-full flex-col overflow-y-auto">
      <TreeNode label={dataset.name} icon={DatabaseIcon} defaultOpen depth={0}>
        <TreeNode label="public" icon={Layers} defaultOpen depth={1}>
          <TreeNode label={entityLabel} icon={Table2} defaultOpen depth={2}>
            {dataset.entities.map((entity) => (
              <TreeNode key={entity.name} label={entity.name} icon={Table2} depth={3}>
                {entity.columns.map((col) => (
                  <div key={col.name} className="flex items-center gap-1.5 py-1 pl-12 pr-2 text-xs text-fg-muted">
                    {col.primaryKey ? (
                      <KeyRound className="h-3 w-3 shrink-0 text-warning" aria-hidden />
                    ) : (
                      <Columns3 className="h-3 w-3 shrink-0" aria-hidden />
                    )}
                    <span className="truncate font-mono">{col.name}</span>
                    <span className="ml-auto shrink-0 text-[10px] uppercase text-fg-muted/70">{col.type}</span>
                  </div>
                ))}
              </TreeNode>
            ))}
          </TreeNode>
        </TreeNode>
      </TreeNode>
    </div>
  );
}

function TreeNode({
  label,
  icon: Icon,
  depth,
  defaultOpen = false,
  children,
}: {
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  depth: number;
  defaultOpen?: boolean;
  children?: React.ReactNode;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const hasChildren = Boolean(children);

  return (
    <div>
      <button
        type="button"
        onClick={() => hasChildren && setOpen((o) => !o)}
        className="flex w-full items-center gap-1.5 py-1 pr-2 text-left text-sm text-fg hover:bg-bg-elevated"
        style={{ paddingLeft: `${depth * 14 + 8}px` }}
      >
        {hasChildren ? (
          open ? (
            <ChevronDown className="h-3.5 w-3.5 shrink-0 text-fg-muted" aria-hidden />
          ) : (
            <ChevronRight className="h-3.5 w-3.5 shrink-0 text-fg-muted" aria-hidden />
          )
        ) : (
          <span className="w-3.5 shrink-0" />
        )}
        <Icon className="h-3.5 w-3.5 shrink-0 text-accent" aria-hidden />
        <span className="truncate font-mono text-xs">{label}</span>
      </button>
      {hasChildren && open && <div>{children}</div>}
    </div>
  );
}
