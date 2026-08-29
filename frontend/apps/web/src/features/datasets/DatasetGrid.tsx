"use client";

import { Badge, Card, EmptyState, Input } from "@DBArena/ui";
import { Database, Search } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import type { Dataset } from "@/lib/mock/types";

export function DatasetGrid({ datasets }: { datasets: Dataset[] }) {
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");

  const categories = useMemo(() => Array.from(new Set(datasets.map((d) => d.category))), [datasets]);

  const filtered = datasets.filter((d) => {
    if (category && d.category !== category) return false;
    if (q && !d.name.toLowerCase().includes(q.toLowerCase()) && !d.description.toLowerCase().includes(q.toLowerCase())) return false;
    return true;
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        <div className="relative min-w-[220px] flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-fg-muted" aria-hidden />
          <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search datasets…" className="pl-9" />
        </div>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="rounded-md border border-border bg-bg px-2.5 py-1.5 text-sm text-fg focus:border-accent focus:outline-none"
        >
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon={Database} title="No datasets match" description="Try a different search term or category." />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((d) => (
            <Link key={d.slug} href={`/datasets/${d.slug}`}>
              <Card className="flex h-full flex-col gap-3 p-4 transition-colors hover:border-accent">
                <div className="flex items-center justify-between">
                  <span className="font-semibold">{d.name}</span>
                  <Badge tone="neutral">{d.category}</Badge>
                </div>
                <p className="text-sm text-fg-muted">{d.description}</p>
                <div className="mt-auto flex flex-wrap items-center justify-between gap-2 pt-1">
                  <div className="flex flex-wrap gap-1.5">
                    {d.engines.map((e) => (
                      <Badge key={e} tone="info">
                        {e}
                      </Badge>
                    ))}
                  </div>
                  <span className="text-xs text-fg-muted">
                    {d.rowCountLabel} · {d.problemCount} problems
                  </span>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
