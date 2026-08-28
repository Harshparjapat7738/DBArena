"use client";

import { Card, CardContent, CardDescription, CardHeader, CardTitle, ThemeSwitcher } from "@dbforge/ui";
import { useAuthStore } from "@/lib/auth/authStore";

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Settings</h1>
        <p className="text-sm text-fg-muted">Manage your account and how DBForge looks.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Account</CardTitle>
          <CardDescription>Your DBForge profile.</CardDescription>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <div className="text-xs font-medium text-fg-muted">Display name</div>
            <div className="text-sm">{user?.displayName}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-fg-muted">Email</div>
            <div className="text-sm">{user?.email}</div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Appearance</CardTitle>
          <CardDescription>
            Pick light or dark mode, or choose any of the VS Code and community themes below - it applies
            instantly, everywhere.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ThemeSwitcher />
        </CardContent>
      </Card>
    </div>
  );
}
