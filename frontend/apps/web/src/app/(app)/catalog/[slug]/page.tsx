import { redirect } from "next/navigation";

/** `/catalog/[slug]` -> `/practice/[slug]` - see `/catalog/page.tsx`'s note. */
export default async function CatalogSlugRedirect({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  redirect(`/practice/${slug}`);
}
