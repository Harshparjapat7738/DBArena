"use client";

import { Button, buttonVariants } from "@DBArena/ui";
import { AlertTriangle } from "lucide-react";
import Link from "next/link";
import { useEffect } from "react";

/**
 * Root error boundary: catches an unhandled render/render-effect error
 * anywhere under the app (Next.js App Router convention - one per
 * segment, this is the top-level fallback). Deliberately never shows
 * `error.message` to the user - only logs it - since an unexpected error
 * here could in principle carry something server-internal; the visible
 * copy stays generic and actionable instead.
 */
export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg px-4 text-center">
      <AlertTriangle className="h-10 w-10 text-danger" aria-hidden />
      <h1 className="text-xl font-semibold text-fg">Something went wrong</h1>
      <p className="max-w-sm text-sm text-fg-muted">
        An unexpected error occurred. You can try again, or head back to your dashboard.
      </p>
      <div className="mt-2 flex gap-3">
        <Button onClick={reset}>Try again</Button>
        <Link href="/dashboard" className={buttonVariants({ variant: "secondary" })}>
          Back to dashboard
        </Link>
      </div>
    </main>
  );
}
