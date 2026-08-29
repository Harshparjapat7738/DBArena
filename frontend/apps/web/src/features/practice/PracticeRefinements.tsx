"use client";

import { Bookmark } from "lucide-react";
import { useEffect, useState } from "react";
import { datasetsRepository, problemsRepository } from "@/lib/mock/repositories";
import type { Dataset, ProblemStatus } from "@/lib/mock/types";

export type SortMode = "recommended" | "newest" | "difficulty" | "completion";

export interface RefinementState {
  topic: string;
  dataset: string;
  status: ProblemStatus | "";
  sort: SortMode;
  bookmarkedOnly: boolean;
}

export const DEFAULT_REFINEMENTS: RefinementState = {
  topic: "",
  dataset: "",
  status: "",
  sort: "recommended",
  bookmarkedOnly: false,
};

const STATUS_LABEL: Record<ProblemStatus, string> = {
  solved: "Solved",
  attempted: "Attempted",
  "not-started": "Not started",
};

const SORT_LABEL: Record<SortMode, string> = {
  recommended: "Recommended",
  newest: "Newest",
  difficulty: "Difficulty",
  completion: "Completion rate",
};

/**
 * Refinements Practice/Problems needs that the real catalog API doesn't
 * support (topic, dataset, attempt status, sort, bookmarked-only) - kept
 * separate from `CatalogFilters` (q/difficulty/engine/tag, which *is*
 * server-driven through URL params) so it's obvious at a glance which
 * controls will eventually hit a real endpoint and which are purely
 * client-side conveniences over data this session's mock layer owns.
 */
export function PracticeRefinements({
  value,
  onChange,
}: {
  value: RefinementState;
  onChange: (next: RefinementState) => void;
}) {
  const [topics, setTopics] = useState<string[]>([]);
  const [datasets, setDatasets] = useState<Dataset[]>([]);

  useEffect(() => {
    problemsRepository.listTopics().then(setTopics);
    datasetsRepository.listDatasets().then(setDatasets);
  }, []);

  function set<K extends keyof RefinementState>(key: K, val: RefinementState[K]) {
    onChange({ ...value, [key]: val });
  }

  const selectClass =
    "rounded-md border border-border bg-bg px-2.5 py-1.5 text-sm text-fg focus:border-accent focus:outline-none";

  return (
    <div className="flex flex-wrap items-center gap-2">
      <select value={value.topic} onChange={(e) => set("topic", e.target.value)} className={selectClass}>
        <option value="">All topics</option>
        {topics.map((t) => (
          <option key={t} value={t}>
            {t.replace(/-/g, " ")}
          </option>
        ))}
      </select>

      <select value={value.dataset} onChange={(e) => set("dataset", e.target.value)} className={selectClass}>
        <option value="">All datasets</option>
        {datasets.map((d) => (
          <option key={d.slug} value={d.slug}>
            {d.name}
          </option>
        ))}
      </select>

      <select
        value={value.status}
        onChange={(e) => set("status", e.target.value as ProblemStatus | "")}
        className={selectClass}
      >
        <option value="">Any status</option>
        {(Object.keys(STATUS_LABEL) as ProblemStatus[]).map((s) => (
          <option key={s} value={s}>
            {STATUS_LABEL[s]}
          </option>
        ))}
      </select>

      <select value={value.sort} onChange={(e) => set("sort", e.target.value as SortMode)} className={selectClass}>
        {(Object.keys(SORT_LABEL) as SortMode[]).map((s) => (
          <option key={s} value={s}>
            Sort: {SORT_LABEL[s]}
          </option>
        ))}
      </select>

      <button
        type="button"
        onClick={() => set("bookmarkedOnly", !value.bookmarkedOnly)}
        aria-pressed={value.bookmarkedOnly}
        className={`flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-sm font-medium transition-colors ${
          value.bookmarkedOnly ? "border-accent bg-accent text-accent-fg" : "border-border text-fg-muted hover:text-fg"
        }`}
      >
        <Bookmark className={`h-3.5 w-3.5 ${value.bookmarkedOnly ? "fill-current" : ""}`} aria-hidden />
        Bookmarked
      </button>
    </div>
  );
}
