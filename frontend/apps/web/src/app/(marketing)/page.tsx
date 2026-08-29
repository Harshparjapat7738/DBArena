import { Badge, Button } from "@DBArena/ui";
import { Database, GitBranch, Sparkles, Zap } from "lucide-react";
import Link from "next/link";

const FEATURES = [
  {
    icon: Database,
    title: "One dataset, three engines",
    body: "Every problem is authored once and materialized into byte-identical PostgreSQL, MySQL, and MongoDB data - solve it in SQL, then again as an aggregation pipeline.",
  },
  {
    icon: Sparkles,
    title: "AI hints that don't spoil it",
    body: "Graduated hints - concept, then approach, then a near-miss nudge at your own query - never a full solution handed to you.",
  },
  {
    icon: GitBranch,
    title: "A real DataGrip-style workbench",
    body: "Schema explorer, a query console bound to your own session, a virtualized result grid, and an execution log - not a toy code box.",
  },
  {
    icon: Zap,
    title: "Every color theme you already use",
    body: "Every VS Code built-in and the community classics - Dracula, Nord, One Dark Pro, GitHub - switchable in Settings, applied everywhere.",
  },
];

export default function MarketingHomePage() {
  return (
    <main className="relative min-h-screen overflow-hidden">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-40"
        style={{
          background:
            "radial-gradient(600px circle at 20% -10%, color-mix(in srgb, var(--color-accent) 25%, transparent), transparent), radial-gradient(500px circle at 90% 10%, color-mix(in srgb, var(--color-info) 20%, transparent), transparent)",
        }}
      />

      <header className="relative z-10 flex items-center justify-between px-6 py-5 sm:px-10">
        <div className="flex items-center gap-2 font-mono text-lg font-semibold">
          <Database className="h-5 w-5 text-accent" aria-hidden />
          DBArena
        </div>
        <nav className="flex items-center gap-3">
          <Link href="/login">
            <Button variant="ghost" size="sm">
              Log in
            </Button>
          </Link>
          <Link href="/register">
            <Button size="sm">Get started</Button>
          </Link>
        </nav>
      </header>

      <section className="relative z-10 mx-auto flex max-w-5xl flex-col items-center px-6 pb-20 pt-16 text-center sm:pt-24">
        <Badge tone="accent" className="mb-6">
          Now with AI-powered hints
        </Badge>
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight sm:text-6xl">
          Practice databases like you practice algorithms.
        </h1>
        <p className="mt-6 max-w-2xl text-lg text-fg-muted">
          A LeetCode-style bench for SQL and MongoDB. One dataset, authored once, materialized
          identically across engines - so the problem is the same whether you write a{" "}
          <code className="rounded bg-bg-elevated px-1.5 py-0.5 font-mono text-sm text-accent">JOIN</code>{" "}
          or an{" "}
          <code className="rounded bg-bg-elevated px-1.5 py-0.5 font-mono text-sm text-accent">
            $lookup
          </code>
          .
        </p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link href="/register">
            <Button size="lg">Start practicing</Button>
          </Link>
          <Link href="/catalog">
            <Button variant="secondary" size="lg">
              Browse problems
            </Button>
          </Link>
        </div>

        <div className="mt-16 w-full max-w-2xl overflow-hidden rounded-xl border border-border bg-editor-bg text-left shadow-xl">
          <div className="flex items-center gap-1.5 border-b border-border bg-bg-elevated px-4 py-2.5">
            <span className="h-3 w-3 rounded-full bg-danger" />
            <span className="h-3 w-3 rounded-full bg-warning" />
            <span className="h-3 w-3 rounded-full bg-success" />
            <span className="ml-3 text-xs text-fg-muted">two-sum.sql</span>
          </div>
          <pre className="overflow-x-auto p-5 font-mono text-sm leading-relaxed text-editor-fg">
            <span className="text-info">SELECT</span> a.id, b.id{"\n"}
            <span className="text-info">FROM</span> numbers a{"\n"}
            <span className="text-info">JOIN</span> numbers b <span className="text-info">ON</span> a.value + b.value ={" "}
            <span className="text-warning">10</span>{"\n"}
            <span className="text-info">WHERE</span> a.id &lt; b.id;
          </pre>
        </div>
      </section>

      <section className="relative z-10 mx-auto grid max-w-5xl gap-6 px-6 pb-24 sm:grid-cols-2">
        {FEATURES.map((feature) => (
          <div key={feature.title} className="rounded-xl border border-border bg-bg-elevated p-6">
            <feature.icon className="mb-3 h-6 w-6 text-accent" aria-hidden />
            <h3 className="mb-1.5 text-lg font-semibold">{feature.title}</h3>
            <p className="text-sm text-fg-muted">{feature.body}</p>
          </div>
        ))}
      </section>
    </main>
  );
}
