"use client";

import type { HintLevel } from "@DBArena/api-client";
import { ApiError } from "@DBArena/api-client";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from "@DBArena/ui";
import { Bug, Compass, Lightbulb, ScrollText, Sparkles, Wand2 } from "lucide-react";
import { useState } from "react";
import { aiApi } from "@/lib/api/clients";
import { aiRepository } from "@/lib/mock/repositories";
import type { AiAction, AiMessage } from "@/lib/mock/types";

const ACTIONS: { value: AiAction; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { value: "hint", label: "Hint", icon: Lightbulb },
  { value: "guide", label: "Guide me", icon: Compass },
  { value: "explain", label: "Explain my query", icon: ScrollText },
  { value: "debug", label: "Debug my query", icon: Bug },
  { value: "solution", label: "Show solution", icon: Sparkles },
  { value: "optimize", label: "Optimize query", icon: Wand2 },
];

const HINT_LEVELS: { value: HintLevel; label: string }[] = [
  { value: "CONCEPT", label: "Concept" },
  { value: "APPROACH", label: "Approach" },
  { value: "NEAR_MISS", label: "Near miss" },
];

/**
 * The full AI panel for a problem: Hint / Guide me / Explain / Debug /
 * Solution / Optimize, sharing one query+error input and one message
 * thread. "Hint" is the one action with a real backend behind it
 * (ai-assistant-service, M16) - used only when `problemSlug` is a real
 * catalog problem; every other action, and hint for a fixture-only
 * problem, goes through the mock `aiRepository` instead. Either way the
 * context notice underneath every response is real, not decorative: it
 * names exactly what this panel sends, mirroring backend hard rule #5's
 * hard-coded context cap.
 */
export function AiAssistantPanel({ problemSlug, problemTitle, isRealProblem }: { problemSlug: string; problemTitle: string; isRealProblem: boolean }) {
  const [action, setAction] = useState<AiAction>("hint");
  const [level, setLevel] = useState<HintLevel>("CONCEPT");
  const [userQuery, setUserQuery] = useState("");
  const [errorOrResult, setErrorOrResult] = useState("");
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function ask() {
    if (action !== "solution" && !userQuery.trim() && action !== "guide") {
      setError("Paste your query attempt first.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      let content: string;
      if (action === "hint" && isRealProblem) {
        const res = await aiApi.getHint(problemSlug, {
          learnerQuery: userQuery || "(no query attempt provided)",
          errorOrResultText: errorOrResult || undefined,
          level,
        });
        content = res.hint;
      } else {
        content = await aiRepository.respond(action, {
          problemTitle,
          userQuery: userQuery || undefined,
          errorOrResult: errorOrResult || undefined,
        });
      }
      const now = new Date().toISOString();
      setMessages((prev) => [
        ...prev,
        { id: `u-${prev.length}`, role: "user", action, content: aiRepository.actionLabel(action), timestamp: now },
        { id: `a-${prev.length}`, role: "assistant", action, content, timestamp: now },
      ]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't reach the assistant right now. Try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-accent" aria-hidden />
          AI assistant
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-1.5 sm:grid-cols-3">
          {ACTIONS.map((a) => (
            <button
              key={a.value}
              type="button"
              onClick={() => setAction(a.value)}
              className={`flex items-center justify-center gap-1.5 rounded-md border px-2 py-1.5 text-xs font-medium transition-colors ${
                action === a.value ? "border-accent bg-accent text-accent-fg" : "border-border text-fg-muted hover:text-fg"
              }`}
            >
              <a.icon className="h-3.5 w-3.5" aria-hidden />
              {a.label}
            </button>
          ))}
        </div>

        {action === "hint" && (
          <div className="flex gap-2">
            {HINT_LEVELS.map((l) => (
              <button
                key={l.value}
                type="button"
                onClick={() => setLevel(l.value)}
                className={`flex-1 rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors ${
                  level === l.value ? "border-accent bg-accent text-accent-fg" : "border-border text-fg-muted hover:text-fg"
                }`}
              >
                {l.label}
              </button>
            ))}
          </div>
        )}

        {action !== "solution" && (
          <>
            <div>
              <label className="mb-1 block text-xs font-medium text-fg-muted">Your query attempt</label>
              <textarea
                value={userQuery}
                onChange={(e) => setUserQuery(e.target.value)}
                rows={4}
                placeholder="SELECT ..."
                className="w-full rounded-md border border-border bg-bg p-2.5 font-mono text-sm text-fg placeholder:text-fg-muted focus:border-accent focus:outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-fg-muted">Error or result (optional)</label>
              <textarea
                value={errorOrResult}
                onChange={(e) => setErrorOrResult(e.target.value)}
                rows={2}
                placeholder="Paste the error message or unexpected output…"
                className="w-full rounded-md border border-border bg-bg p-2.5 font-mono text-sm text-fg placeholder:text-fg-muted focus:border-accent focus:outline-none"
              />
            </div>
          </>
        )}

        {error && <p className="text-sm text-danger">{error}</p>}

        <Button onClick={ask} disabled={loading} className="self-start">
          {loading ? "Thinking…" : aiRepository.actionLabel(action)}
        </Button>

        {messages.length > 0 && (
          <div className="flex flex-col gap-3 border-t border-border pt-3">
            {messages.map((m) =>
              m.role === "assistant" ? (
                <div key={m.id} className="rounded-md border border-border bg-bg-elevated p-3">
                  <div className="mb-2 flex items-center justify-between">
                    <Badge tone="accent">{aiRepository.actionLabel(m.action)}</Badge>
                    <span className="text-xs text-fg-muted">
                      {m.action === "hint" && isRealProblem ? "ai-assistant-service" : "mock response"}
                    </span>
                  </div>
                  <p className="whitespace-pre-wrap text-sm leading-relaxed text-fg">{m.content}</p>
                </div>
              ) : null,
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
