import {
  Bookmark,
  CalendarCheck,
  Database,
  GraduationCap,
  LayoutDashboard,
  ListChecks,
  Settings as SettingsIcon,
  TerminalSquare,
  TrendingUp,
  Trophy,
  User,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  /** Match this path *and* its children as "active" (most items); a few (like Practice, which also owns /practice/[slug]) need the same. Kept explicit rather than always prefix-matching so `/practice` doesn't light up for `/practice-something-else` down the line. */
  matchPrefix?: boolean;
}

/**
 * Primary information architecture. Deliberately narrower than the brief's
 * literal list: "Problems" is folded into "Practice" (the same concept
 * under two names in most competitive-practice products - LeetCode's
 * "Problems" and HackerRank's "Practice" are the same page), and
 * "Databases" is folded into "Datasets" (this product's whole premise is
 * one dataset materialized into three databases, so a separate top-level
 * "Databases" section would just be the same catalogue browsable a second
 * way - see the deliverable notes for the full reasoning).
 */
export const PRIMARY_NAV: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/practice", label: "Practice", icon: ListChecks, matchPrefix: true },
  { href: "/playground", label: "Playground", icon: TerminalSquare },
  { href: "/datasets", label: "Datasets", icon: Database, matchPrefix: true },
  { href: "/learning", label: "Learning", icon: GraduationCap, matchPrefix: true },
  { href: "/daily-challenge", label: "Daily Challenge", icon: CalendarCheck },
  { href: "/leaderboard", label: "Leaderboard", icon: Trophy },
  { href: "/progress", label: "Progress", icon: TrendingUp },
];

export const SECONDARY_NAV: NavItem[] = [
  { href: "/bookmarks", label: "Bookmarks & History", icon: Bookmark },
  { href: "/profile", label: "Profile", icon: User },
  { href: "/settings", label: "Settings", icon: SettingsIcon },
];
