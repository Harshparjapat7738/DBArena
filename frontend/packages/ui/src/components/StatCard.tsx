import { cn } from "../lib/cn";

export interface StatCardProps extends React.HTMLAttributes<HTMLDivElement> {
  label: string;
  value: React.ReactNode;
  icon?: React.ComponentType<{ className?: string }>;
  hint?: React.ReactNode;
  tone?: "accent" | "success" | "warning" | "danger" | "info" | "neutral";
}

const TONE_CLASS: Record<NonNullable<StatCardProps["tone"]>, string> = {
  accent: "text-accent bg-accent/10",
  success: "text-success bg-success/10",
  warning: "text-warning bg-warning/10",
  danger: "text-danger bg-danger/10",
  info: "text-info bg-info/10",
  neutral: "text-fg-muted bg-bg",
};

/** The one small metric tile used everywhere - dashboard header, profile, progress page. Icon is optional so it degrades to a plain label/value pair. */
export function StatCard({ label, value, icon: Icon, hint, tone = "neutral", className, ...props }: StatCardProps) {
  return (
    <div className={cn("flex items-center gap-3 rounded-lg border border-border bg-bg-elevated p-4", className)} {...props}>
      {Icon && (
        <div className={cn("flex h-10 w-10 shrink-0 items-center justify-center rounded-md", TONE_CLASS[tone])}>
          <Icon className="h-5 w-5" />
        </div>
      )}
      <div className="min-w-0">
        <div className="truncate text-xs font-medium text-fg-muted">{label}</div>
        <div className="text-lg font-semibold leading-tight text-fg">{value}</div>
        {hint && <div className="truncate text-xs text-fg-muted">{hint}</div>}
      </div>
    </div>
  );
}
