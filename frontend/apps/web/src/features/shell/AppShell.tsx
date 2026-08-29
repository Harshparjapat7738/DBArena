"use client";

import { Button } from "@DBArena/ui";
import { Database, LayoutGrid, LogOut, Settings as SettingsIcon } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { authApi } from "@/lib/api/clients";
import { useAuthStore } from "@/lib/auth/authStore";

const NAV = [
  { href: "/catalog", label: "Catalog", icon: LayoutGrid },
  { href: "/settings", label: "Settings", icon: SettingsIcon },
];

/**
 * The authed app shell: sidebar nav + top bar. `AuthProvider` (root
 * layout) attempts a silent refresh on every load; this component just
 * reacts to the result - redirects to `/login` once bootstrap finishes
 * and there's still no session, and never redirects while `status` is
 * still `"loading"` (that would bounce a genuinely logged-in user on
 * every page load, before their session has had a chance to resolve).
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
    <div className="flex min-h-screen bg-bg text-fg">
      <aside className="flex w-60 shrink-0 flex-col border-r border-border bg-bg-elevated">
        <div className="flex items-center gap-2 px-5 py-5 font-mono text-lg font-semibold">
          <Database className="h-5 w-5 text-accent" aria-hidden />
          DBArena
        </div>
        <nav className="flex flex-1 flex-col gap-1 px-3">
          {NAV.map((item) => {
            const active = pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  active ? "bg-accent text-accent-fg" : "text-fg-muted hover:bg-bg hover:text-fg"
                }`}
              >
                <item.icon className="h-4 w-4" aria-hidden />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-border px-3 py-3">
          <div className="mb-2 truncate px-2 text-sm font-medium">{user?.displayName}</div>
          <Button variant="ghost" size="sm" className="w-full justify-start" onClick={handleLogout}>
            <LogOut className="h-4 w-4" aria-hidden />
            Log out
          </Button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto">{children}</main>
    </div>
  );
}
