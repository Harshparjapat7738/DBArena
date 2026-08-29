import { cn } from "../lib/cn";

export interface ProgressBarProps extends React.HTMLAttributes<HTMLDivElement> {
  /** 0-100. Clamped. */
  value: number;
  tone?: "accent" | "success" | "warning" | "danger";
  size?: "sm" | "md";
}

const TONE_CLASS: Record<NonNullable<ProgressBarProps["tone"]>, string> = {
  accent: "bg-accent",
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-danger",
};

/** A plain determinate progress bar - mastery %, XP-into-level, lesson completion. Native `role="progressbar"` semantics, no animation beyond the width transition. */
export function ProgressBar({ value, tone = "accent", size = "md", className, ...props }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <div
      role="progressbar"
      aria-valuenow={clamped}
      aria-valuemin={0}
      aria-valuemax={100}
      className={cn("w-full overflow-hidden rounded-full bg-bg-elevated", size === "sm" ? "h-1.5" : "h-2.5", className)}
      {...props}
    >
      <div
        className={cn("h-full rounded-full transition-[width] duration-300", TONE_CLASS[tone])}
        style={{ width: `${clamped}%` }}
      />
    </div>
  );
}
