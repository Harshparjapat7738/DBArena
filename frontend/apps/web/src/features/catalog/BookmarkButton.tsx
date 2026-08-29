"use client";

import type { Difficulty } from "@DBArena/api-client";
import { Button } from "@DBArena/ui";
import { Bookmark } from "lucide-react";
import { useEffect, useState } from "react";
import { bookmarksRepository } from "@/lib/mock/repositories";

export function BookmarkButton({ slug, title, difficulty }: { slug: string; title: string; difficulty: Difficulty }) {
  const [bookmarked, setBookmarked] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    bookmarksRepository.isBookmarked(slug).then((v) => {
      setBookmarked(v);
      setReady(true);
    });
  }, [slug]);

  async function toggle() {
    const next = await bookmarksRepository.toggle({ slug, title, difficulty });
    setBookmarked(next);
  }

  return (
    <Button variant="secondary" size="sm" onClick={toggle} disabled={!ready} aria-pressed={bookmarked}>
      <Bookmark className={`h-4 w-4 ${bookmarked ? "fill-accent text-accent" : ""}`} aria-hidden />
      {bookmarked ? "Bookmarked" : "Bookmark"}
    </Button>
  );
}
