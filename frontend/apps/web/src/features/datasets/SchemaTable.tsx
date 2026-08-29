import type { EngineKind } from "@DBArena/api-client";
import { Badge } from "@DBArena/ui";
import { KeyRound, Link2 } from "lucide-react";
import type { DatasetEntity } from "@/lib/mock/types";

/**
 * One table/collection's column list + sample rows - the same CDM-shaped
 * entity is reused verbatim across all three engines (this product's whole
 * premise), so only the *labels* change per engine, not the data: a
 * PostgreSQL/MySQL table is a MongoDB collection, a column is a field.
 */
export function SchemaTable({ entity, engine, showSampleRows = true }: { entity: DatasetEntity; engine?: EngineKind; showSampleRows?: boolean }) {
  const entityLabel = engine === "MONGODB" ? "Collection" : "Table";
  const columnLabel = engine === "MONGODB" ? "Field" : "Column";

  return (
    <div className="overflow-hidden rounded-lg border border-border">
      <div className="flex items-center justify-between border-b border-border bg-bg-elevated px-3 py-2">
        <span className="font-mono text-sm font-semibold text-fg">{entity.name}</span>
        <Badge tone="neutral">{entityLabel}</Badge>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-border text-xs text-fg-muted">
              <th className="px-3 py-2 font-medium">{columnLabel}</th>
              <th className="px-3 py-2 font-medium">Type</th>
              <th className="px-3 py-2 font-medium">Nullable</th>
            </tr>
          </thead>
          <tbody>
            {entity.columns.map((col) => (
              <tr key={col.name} className="border-b border-border last:border-0">
                <td className="px-3 py-1.5 font-mono text-xs">
                  <span className="flex items-center gap-1.5">
                    {col.primaryKey && <KeyRound className="h-3 w-3 text-warning" aria-label="Primary key" />}
                    {col.foreignKey && <Link2 className="h-3 w-3 text-info" aria-label={`References ${col.foreignKey}`} />}
                    {col.name}
                  </span>
                </td>
                <td className="px-3 py-1.5 font-mono text-xs text-fg-muted">{col.type}</td>
                <td className="px-3 py-1.5 text-xs text-fg-muted">{col.nullable ? "yes" : "no"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showSampleRows && entity.sampleRows.length > 0 && (
        <div className="border-t border-border">
          <div className="px-3 py-1.5 text-xs font-medium text-fg-muted">Sample rows</div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border text-fg-muted">
                  {entity.columns.map((c) => (
                    <th key={c.name} className="px-3 py-1.5 font-mono font-medium">
                      {c.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {entity.sampleRows.map((row, i) => (
                  <tr key={i} className="border-b border-border last:border-0">
                    {entity.columns.map((c) => (
                      <td key={c.name} className="px-3 py-1.5 font-mono text-fg">
                        {row[c.name] === null || row[c.name] === undefined ? (
                          <span className="text-fg-muted">null</span>
                        ) : (
                          String(row[c.name])
                        )}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
