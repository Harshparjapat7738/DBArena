import { Skeleton } from "@DBArena/ui";

/**
 * Route-level Suspense fallback while `page.tsx`'s server-side
 * `catalogApi.listProblems`/`listTags` calls resolve. Without this,
 * navigating into `/practice` shows a blank page for the duration of the
 * fetch instead of an immediate, on-brand loading state.
 */
export default function PracticeLoading() {
  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-4 w-80" />
      </div>
      <Skeleton className="h-24 rounded-lg" />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 9 }).map((_, i) => (
          <Skeleton key={i} className="h-40 rounded-lg" />
        ))}
      </div>
    </div>
  );
}
