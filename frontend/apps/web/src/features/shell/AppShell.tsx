"use client";

import { Avatar, Button } from "@DBArena/ui";
import { Database, LogOut } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { authApi } from "@/lib/api/clients";
import { useAuthStore } from "@/lib/auth/authStore";
import { HeaderStatus } from "./HeaderStatus";
import { PRIMARY_NAV, SECONDARY_NAV, type NavItem } from "./nav";

function isActive(pathname: string, item: NavItem): boolean {
  return item.matchPrefix ? pathname.startsWith(item.href) : pathname === item.href;
}

function NavLink({ item, pathname }: { item: NavItem; pathname: string }) {
  const active = isActive(pathname, item);
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
        active ? "bg-accent text-accent-fg" : "text-fg-muted hover:bg-bg hover:text-fg"
      }`}
    >
      <item.icon className="h-4 w-4 shrink-0" aria-hidden />
      <span className="truncate">{item.label}</span>
    </Link>
  );
}

/**
 * The authed app shell: sidebar nav + top bar. `AuthProvider` (root
 * layout) attempts a silent refresh on every load; this component just
 * reacts to the result - redirects to `/login` once bootstrap finishes
 * and there's still no session, and never redirects while `status` is
 * still `"loading"` (that would bounce a genuinely logged-in user on
 * every page load, before their session has had a chance to resolve).
 *
 * `h-screen` (not `min-h-screen`) + `overflow-y-auto` on `<main>` only:
 * every normal page scrolls inside `<main>`, but the Playground page can
 * instead fill `<main>` edge-to-edge with its own internal panes (each
 * scrolling independently) without the whole document also trying to
 * scroll - the standard app-shell layout, not just a dashboard affectation.
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const status = useAuthStore((s) => s.status);
  const user = useAuthStore((s) => s.user);
  const clearSession = useAuthStore((s) => s.clearSession);

  useEffect(() => {
    if (status === "anonymous") {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [status, pathname, router]);

  async function handleLogout() {
    try {
      await authApi.logout();
    } finally {
      clearSession();
      router.push("/login");
    }
  }

  if (status !== "authenticated") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg text-fg-muted">
        <span className="animate-pulse text-sm">Loading your session…</span>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-bg text-fg">
      <aside className="flex w-60 shrink-0 flex-col border-r border-border bg-bg-elevated">
        <Link href="/dashboard" className="flex items-center gap-2 px-5 py-5 font-mono text-lg font-semibold">
          <Database className="h-5 w-5 text-accent" aria-hidden />
          DBArena
        </Link>

        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-3">
          {PRIMARY_NAV.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
          <div className="my-2 border-t border-border" />
          {SECONDARY_NAV.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
        </nav>

        <div className="border-t border-border px-3 py-3">
          <Link href="/profile" className="mb-2 flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-bg">
            <Avatar name={user?.displayName ?? "?"} size="sm" />
            <span className="truncate text-sm font-medium">{user?.displayName}</span>
          </Link>
          <Button variant="ghost" size="sm" className="w-full justify-start" onClick={handleLogout}>
            <LogOut className="h-4 w-4" aria-hidden />
            Log out
          </Button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center justify-end border-b border-border px-6">
          <HeaderStatus />
        </header>
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
