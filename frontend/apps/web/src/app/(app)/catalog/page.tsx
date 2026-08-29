import { redirect } from "next/navigation";

/**
 * `/catalog` was renamed to `/practice` (see that route's page.tsx) -
 * kept as a redirect, not deleted, so any bookmarked or externally-linked
 * `/catalog` URL still lands somewhere real. Query string (filters) is
 * preserved.
 */
export default async function CatalogRedirect({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | undefined>>;
}) {
  const sp = await searchParams;
  const qs = new URLSearchParams(sp as Record<string, string>).toString();
  redirect(`/practice${qs ? `?${qs}` : ""}`);
}
