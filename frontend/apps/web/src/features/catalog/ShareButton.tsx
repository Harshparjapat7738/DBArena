"use client";

import { Button } from "@DBArena/ui";
import { Check, Share2 } from "lucide-react";
import { useState } from "react";

export function ShareButton({ path }: { path: string }) {
  const [copied, setCopied] = useState(false);

  async function share() {
    const url = typeof window !== "undefined" ? `${window.location.origin}${path}` : path;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      // Clipboard API can be unavailable (older browser, permissions) - not worth surfacing an error for a share link.
    }
  }

  return (
    <Button variant="secondary" size="sm" onClick={share}>
      {copied ? <Check className="h-4 w-4 text-success" aria-hidden /> : <Share2 className="h-4 w-4" aria-hidden />}
      {copied ? "Link copied" : "Share"}
    </Button>
  );
}
