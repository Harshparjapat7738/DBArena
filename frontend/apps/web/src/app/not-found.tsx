import { buttonVariants } from "@DBArena/ui";
import { Database } from "lucide-react";
import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg px-4 text-center">
      <Link href="/" className="flex items-center gap-2 font-mono text-lg font-semibold text-fg">
        <Database className="h-5 w-5 text-accent" aria-hidden />
        DBArena
      </Link>
      <p className="text-6xl font-bold text-fg-muted">404</p>
      <h1 className="text-xl font-semibold text-fg">Page not found</h1>
      <p className="max-w-sm text-sm text-fg-muted">
        The page you're looking for doesn't exist, or the problem may have been unpublished.
      </p>
      <Link href="/dashboard" className={buttonVariants({ className: "mt-2" })}>
        Back to dashboard
      </Link>
    </main>
  );
}
