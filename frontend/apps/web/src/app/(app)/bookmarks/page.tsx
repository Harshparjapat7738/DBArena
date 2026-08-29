"use client";

import { Badge, EmptyState, Skeleton, Tabs } from "@DBArena/ui";
import { Bookmark, CheckCircle2, History, XCircle } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { DifficultyBadge, EngineBadge } from "@/features/catalog/badges";
import { bookmarksRepository, submissionsRepository } from "@/lib/mock/repositories";
import type { Bookmark as BookmarkT, Submission } from "@/lib/mock/types";

export default function BookmarksPage() {
  const [tab, setTab] = useState<"bookmarks" | "history">("bookmarks");
  const [bookmarks, setBookmarks] = useState<BookmarkT[] | null>(null);
  const [submissions, setSubmissions] = useState<Submission[] | null>(null);

  useEffect(() => {
    bookmarksRepository.list().then(setBookmarks);
    submissionsRepository.list().then(setSubmissions);
  }, []);

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Bookmarks &amp; History</h1>
        <p className="text-sm text-fg-muted">Problems you&apos;ve saved for later, and every query you&apos;ve submitted.</p>
      </div>

      <Tabs
        items={[
          { value: "bookmarks", label: "Bookmarks", icon: Bookmark, badge: bookmarks ? <Badge tone="neutral">{bookmarks.length}</Badge> : undefined },
          { value: "history", label: "Submission history", icon: History, badge: submissions ? <Badge tone="neutral">{submissions.length}</Badge> : undefined },
        ]}
        value={tab}
        onChange={(v) => setTab(v as "bookmarks" | "history")}
      />

      {tab === "bookmarks" ? (
        !bookmarks ? (
          <Skeleton className="h-64 rounded-lg" />
        ) : bookmarks.length === 0 ? (
          <EmptyState
            icon={Bookmark}
            title="No bookmarks yet"
            description="Bookmark a problem from Practice or its detail page to save it here."
            action={
              <Link href="/practice" className="text-sm font-medium text-accent hover:underline">
                Browse problems →
              </Link>
            }
          />
        ) : (
          <div className="flex flex-col divide-y divide-border rounded-lg border border-border">
            {bookmarks.map((b) => (
              <Link key={b.problemSlug} href={`/practice/${b.problemSlug}`} className="flex items-center gap-3 px-4 py-3 text-sm hover:bg-bg-elevated">
                <Bookmark className="h-4 w-4 shrink-0 fill-accent text-accent" aria-hidden />
                <span className="min-w-0 flex-1 truncate">{b.title}</span>
                <DifficultyBadge difficulty={b.difficulty} />
              </Link>
            ))}
          </div>
        )
      ) : !submissions ? (
        <Skeleton className="h-64 rounded-lg" />
      ) : submissions.length === 0 ? (
        <EmptyState
          icon={History}
          title="No submissions yet"
          description="Once you submit a query in the Playground, it'll show up here."
          action={
            <Link href="/playground" className="text-sm font-medium text-accent hover:underline">
              Open Playground →
            </Link>
          }
        />
      ) : (
        <div className="flex flex-col divide-y divide-border rounded-lg border border-border">
          {submissions.map((s) => (
            <Link key={s.id} href={`/submissions/${s.id}`} className="flex items-center gap-3 px-4 py-3 text-sm hover:bg-bg-elevated">
              {s.status === "ACCEPTED" ? (
                <CheckCircle2 className="h-4 w-4 shrink-0 text-success" aria-hidden />
              ) : (
                <XCircle className="h-4 w-4 shrink-0 text-danger" aria-hidden />
              )}
              <span className="min-w-0 flex-1 truncate">{s.problemTitle}</span>
              <EngineBadge engine={s.engine} />
              <span className="shrink-0 text-xs text-fg-muted">{new Date(s.submittedAt).toLocaleString()}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
