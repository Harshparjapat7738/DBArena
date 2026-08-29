"use client";

import { EmptyState, Tabs } from "@DBArena/ui";
import { AlertTriangle, PlayCircle, Table2 } from "lucide-react";
import { useState } from "react";
import type { MockQueryResult } from "@/lib/mock/types";

export function ResultsPanel({ result, running }: { result: MockQueryResult | null; running: boolean }) {
  const [tab, setTab] = useState<"result" | "messages" | "explain">("result");

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b border-border px-2">
        <Tabs
          items={[
            { value: "result", label: "Result" },
            { value: "messages", label: "Messages", badge: result?.warnings.length ? <span className="text-warning">{result.warnings.length}</span> : undefined },
            { value: "explain", label: "Explain" },
          ]}
          value={tab}
          onChange={(v) => setTab(v as typeof tab)}
        />
        {result && !result.error && (
          <span className="shrink-0 pl-3 text-xs text-fg-muted">
            {result.rowCount} row{result.rowCount === 1 ? "" : "s"} · {result.executionMs}ms
          </span>
        )}
      </div>

      <div className="flex-1 overflow-auto">
        {running ? (
          <div className="flex h-full items-center justify-center gap-2 text-sm text-fg-muted">
            <PlayCircle className="h-4 w-4 animate-pulse" aria-hidden />
            Running…
          </div>
        ) : !result ? (
          <EmptyState
            icon={Table2}
            title="No results yet"
            description="Run a query (Ctrl/Cmd+Enter) to see results here."
            className="h-full justify-center border-none"
          />
        ) : tab === "result" ? (
          result.error ? (
            <div className="flex items-center gap-2 p-4 text-sm text-danger">
              <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden />
              {result.error}
            </div>
          ) : result.rows.length === 0 ? (
            <p className="p-4 text-sm text-fg-muted">Query succeeded, but returned no rows.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="sticky top-0 bg-bg-elevated">
                <tr className="border-b border-border text-xs text-fg-muted">
                  {result.columns.map((c) => (
                    <th key={c} className="whitespace-nowrap px-3 py-1.5 font-mono font-medium">
                      {c}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {result.rows.map((row, i) => (
                  <tr key={i} className="border-b border-border last:border-0 hover:bg-bg-elevated/60">
                    {row.map((cell, j) => (
                      <td key={j} className="whitespace-nowrap px-3 py-1.5 font-mono">
                        {cell === null ? <span className="text-fg-muted">null</span> : String(cell)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          )
        ) : tab === "messages" ? (
          <div className="flex flex-col gap-1.5 p-3 text-xs">
            <p className="text-fg-muted">
              {result.error ? "1 error." : `Query OK, ${result.rowCount} row(s) returned in ${result.executionMs}ms.`}
            </p>
            {result.warnings.map((w, i) => (
              <p key={i} className="text-warning">
                Warning: {w}
              </p>
            ))}
          </div>
        ) : (
          <pre className="overflow-x-auto p-3 font-mono text-xs text-fg-muted">{result.explainPlan || "No plan available."}</pre>
        )}
      </div>
    </div>
  );
}
