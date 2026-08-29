"use client";

import type { EngineKind } from "@DBArena/api-client";
import { Button } from "@DBArena/ui";
import {
  Eraser,
  History,
  Play,
  Send,
  Sparkles,
  Wand2,
  Waypoints,
  X,
} from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { AiAssistantPanel } from "@/features/ai-assistant/AiAssistantPanel";
import { DatabaseExplorer } from "@/features/playground/DatabaseExplorer";
import { QueryEditor } from "@/features/playground/QueryEditor";
import { ResultsPanel } from "@/features/playground/ResultsPanel";
import { datasetsRepository, playgroundRepository, problemsRepository, submissionsRepository } from "@/lib/mock/repositories";
import type { Dataset, MockQueryResult, QueryHistoryEntry } from "@/lib/mock/types";

const ENGINE_LABEL: Record<EngineKind, string> = { POSTGRES: "PostgreSQL", MYSQL: "MySQL", MONGODB: "MongoDB" };

function formatQuery(query: string, engine: EngineKind): string {
  if (engine === "MONGODB") return query;
  const keywords = ["select", "from", "where", "join", "left join", "group by", "order by", "having", "limit", "insert into", "values", "update", "set", "delete"];
  let out = query;
  for (const kw of keywords) {
    out = out.replace(new RegExp(`\\b${kw}\\b`, "gi"), kw.toUpperCase());
  }
  return out;
}

function PlaygroundInner() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [datasetSlug, setDatasetSlug] = useState(searchParams.get("dataset") ?? "");
  const [engine, setEngine] = useState<EngineKind>((searchParams.get("engine") as EngineKind) ?? "POSTGRES");
  const [query, setQuery] = useState(searchParams.get("query") ?? "");
  const [problemSlug, setProblemSlug] = useState(searchParams.get("problem") ?? "");
  const [problemTitle, setProblemTitle] = useState("");
  const [result, setResult] = useState<MockQueryResult | null>(null);
  const [running, setRunning] = useState(false);
  const [aiOpen, setAiOpen] = useState(true);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [history, setHistory] = useState<QueryHistoryEntry[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    datasetsRepository.listDatasets().then((all) => {
      setDatasets(all);
      if (!datasetSlug && all[0]) setDatasetSlug(all[0].slug);
    });
    playgroundRepository.getHistory().then(setHistory);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (problemSlug) {
      problemsRepository.getMockProblem(problemSlug).then((p) => setProblemTitle(p?.title ?? problemSlug));
    }
  }, [problemSlug]);

  const dataset = datasets.find((d) => d.slug === datasetSlug);

  async function run() {
    if (!dataset) return;
    setRunning(true);
    try {
      const res = await playgroundRepository.runQuery({ engine, datasetSlug: dataset.slug, query });
      setResult(res);
      playgroundRepository.getHistory().then(setHistory);
    } finally {
      setRunning(false);
    }
  }

  async function submit() {
    if (!problemSlug || submitting) return;
    setSubmitting(true);
    try {
      const submission = await submissionsRepository.submit({ problemSlug, problemTitle, engine, query });
      router.push(`/submissions/${submission.id}`);
    } finally {
      setSubmitting(false);
    }
  }

  if (!dataset) {
    return <div className="flex h-full items-center justify-center text-sm text-fg-muted">Loading playground…</div>;
  }

  return (
    <div className="flex h-full flex-col">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-2 border-b border-border px-3 py-2">
        <select
          value={dataset.slug}
          onChange={(e) => {
            setDatasetSlug(e.target.value);
            setProblemSlug("");
          }}
          className="rounded-md border border-border bg-bg px-2 py-1 text-xs text-fg focus:border-accent focus:outline-none"
        >
          {datasets.map((d) => (
            <option key={d.slug} value={d.slug}>
              {d.name}
            </option>
          ))}
        </select>
        <select
          value={engine}
          onChange={(e) => setEngine(e.target.value as EngineKind)}
          className="rounded-md border border-border bg-bg px-2 py-1 text-xs text-fg focus:border-accent focus:outline-none"
        >
          {dataset.engines.map((e) => (
            <option key={e} value={e}>
              {ENGINE_LABEL[e]}
            </option>
          ))}
        </select>

        {problemSlug && (
          <span className="rounded-full bg-accent/15 px-2.5 py-1 text-xs text-accent">Practicing: {problemTitle || problemSlug}</span>
        )}

        <div className="ml-auto flex flex-wrap items-center gap-1.5">
          <Button size="sm" variant="secondary" onClick={() => setQuery((q) => formatQuery(q, engine))} title="Format query">
            <Wand2 className="h-3.5 w-3.5" aria-hidden />
            Format
          </Button>
          <Button size="sm" variant="secondary" onClick={() => setQuery("")} title="Clear editor">
            <Eraser className="h-3.5 w-3.5" aria-hidden />
            Clear
          </Button>
          <div className="relative">
            <Button size="sm" variant="secondary" onClick={() => setHistoryOpen((o) => !o)} title="Query history">
              <History className="h-3.5 w-3.5" aria-hidden />
              History
            </Button>
            {historyOpen && (
              <div className="absolute right-0 top-full z-20 mt-1 max-h-72 w-80 overflow-y-auto rounded-md border border-border bg-bg-elevated p-1 shadow-lg">
                {history.length === 0 ? (
                  <p className="p-3 text-xs text-fg-muted">No queries run yet this session.</p>
                ) : (
                  history.map((h) => (
                    <button
                      key={h.id}
                      type="button"
                      onClick={() => {
                        setQuery(h.query);
                        setEngine(h.engine);
                        setHistoryOpen(false);
                      }}
                      className="block w-full truncate rounded px-2 py-1.5 text-left font-mono text-xs text-fg hover:bg-bg"
                    >
                      {h.query.split("\n")[0]}
                    </button>
                  ))
                )}
              </div>
            )}
          </div>
          <Button size="sm" variant="secondary" onClick={run} disabled={running}>
            <Waypoints className="h-3.5 w-3.5" aria-hidden />
            Explain
          </Button>
          <Button size="sm" onClick={run} disabled={running}>
            <Play className="h-3.5 w-3.5" aria-hidden />
            Run
          </Button>
          {problemSlug && (
            <Button size="sm" variant="primary" onClick={submit} disabled={submitting || running}>
              <Send className="h-3.5 w-3.5" aria-hidden />
              {submitting ? "Submitting…" : "Submit"}
            </Button>
          )}
          <Button size="sm" variant={aiOpen ? "primary" : "secondary"} onClick={() => setAiOpen((o) => !o)} title="Toggle AI assistant">
            {aiOpen ? <X className="h-3.5 w-3.5" aria-hidden /> : <Sparkles className="h-3.5 w-3.5" aria-hidden />}
          </Button>
        </div>
      </div>

      {/* Body: explorer | editor+results | AI */}
      <div className="grid min-h-0 flex-1 grid-cols-[220px_1fr] lg:grid-cols-[240px_1fr_360px]">
        <div className="border-r border-border">
          <DatabaseExplorer dataset={dataset} engine={engine} />
        </div>

        <div className="flex min-w-0 flex-col">
          <div className="h-1/2 min-h-[160px] border-b border-border">
            <QueryEditor value={query} onChange={setQuery} onRun={run} placeholder={engine === "MONGODB" ? "db.collection.aggregate([...])" : "SELECT ..."} />
          </div>
          <div className="h-1/2 min-h-[160px]">
            <ResultsPanel result={result} running={running} />
          </div>
        </div>

        {aiOpen && (
          <div className="hidden overflow-y-auto border-l border-border p-3 lg:block">
            <AiAssistantPanel problemSlug={problemSlug || dataset.slug} problemTitle={problemTitle || dataset.name} isRealProblem={false} />
          </div>
        )}
      </div>
    </div>
  );
}

export default function PlaygroundPage() {
  return (
    <Suspense fallback={<div className="flex h-full items-center justify-center text-sm text-fg-muted">Loading playground…</div>}>
      <PlaygroundInner />
    </Suspense>
  );
}
