"use client";

import type { HintLevel, HintResponse } from "@DBArena/api-client";
import { ApiError } from "@DBArena/api-client";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from "@DBArena/ui";
import { Lightbulb, Sparkles } from "lucide-react";
import { useState } from "react";
import { aiApi } from "@/lib/api/clients";

const LEVELS: { value: HintLevel; label: string; description: string }[] = [
  { value: "CONCEPT", label: "Concept", description: "A nudge toward the right idea." },
  { value: "APPROACH", label: "Approach", description: "A concrete strategy, no code." },
  { value: "NEAR_MISS", label: "Near miss", description: "What's likely wrong with your attempt." },
];

/**
 * Learner pastes their query attempt (and optionally the error/result they
 * got back) and picks a graduated hint level; calls ai-assistant-service
 * (M16) through the same BFF proxy as everything else. No execution
 * service wired up yet, so this is manual paste-in by design (confirmed
 * scope for the AI feature).
 */
export function HintPanel({ problemSlug }: { problemSlug: string }) {
  const [learnerQuery, setLearnerQuery] = useState("");
  const [errorOrResultText, setErrorOrResultText] = useState("");
  const [level, setLevel] = useState<HintLevel>("CONCEPT");
  const [hint, setHint] = useState<HintResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function requestHint() {
    if (!learnerQuery.trim()) {
      setError("Paste your query attempt first.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await aiApi.getHint(problemSlug, {
        learnerQuery,
        errorOrResultText: errorOrResultText || undefined,
        level,
      });
      setHint(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't get a hint right now. Try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-accent" aria-hidden />
          AI hint assistant
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex gap-2">
          {LEVELS.map((l) => (
            <button
              key={l.value}
              type="button"
              onClick={() => setLevel(l.value)}
              title={l.description}
              className={`flex-1 rounded-md border px-2.5 py-1.5 text-sm font-medium transition-colors ${
                level === l.value
                  ? "border-accent bg-accent text-accent-fg"
                  : "border-border text-fg-muted hover:text-fg"
              }`}
            >
              {l.label}
            </button>
          ))}
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-fg-muted">Your query attempt</label>
          <textarea
            value={learnerQuery}
            onChange={(e) => setLearnerQuery(e.target.value)}
            rows={5}
            placeholder="SELECT ..."
            className="w-full rounded-md border border-border bg-bg p-2.5 font-mono text-sm text-fg placeholder:text-fg-muted focus:border-accent focus:outline-none"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-fg-muted">Error or result (optional)</label>
          <textarea
            value={errorOrResultText}
            onChange={(e) => setErrorOrResultText(e.target.value)}
            rows={3}
            placeholder="Paste the error message or unexpected output…"
            className="w-full rounded-md border border-border bg-bg p-2.5 font-mono text-sm text-fg placeholder:text-fg-muted focus:border-accent focus:outline-none"
          />
        </div>

        {error && <p className="text-sm text-danger">{error}</p>}

        <Button onClick={requestHint} disabled={loading} className="self-start">
          <Lightbulb className="h-4 w-4" aria-hidden />
          {loading ? "Thinking…" : "Get a hint"}
        </Button>

        {hint && (
          <div className="rounded-md border border-border bg-bg-elevated p-3">
            <div className="mb-2 flex items-center justify-between">
              <Badge tone="accent">{LEVELS.find((l) => l.value === hint.level)?.label ?? hint.level}</Badge>
              <span className="text-xs text-fg-muted">via {hint.provider}</span>
            </div>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-fg">{hint.hint}</p>
            {hint.truncated && (
              <p className="mt-2 text-xs text-warning">This hint was trimmed to stay within the response limit.</p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
