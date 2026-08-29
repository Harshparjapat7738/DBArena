"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError } from "@DBArena/api-client";
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, FieldError, Input, Label } from "@DBArena/ui";
import { Database } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { authApi } from "@/lib/api/clients";
import { useAuthStore } from "@/lib/auth/authStore";

// Mirrors identity-service's RegisterRequest bean validation (12-200 char password) exactly,
// so a violation surfaces in the form before the round-trip, not just after a 422.
const schema = z.object({
  displayName: z.string().min(1, "Display name is required").max(200),
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(12, "Must be at least 12 characters").max(200),
});

type FormValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const setSession = useAuthStore((s) => s.setSession);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    setFormError(null);
    try {
      const res = await authApi.register(values);
      setSession(res.accessToken, res.user);
      router.push("/dashboard");
    } catch (err) {
      if (err instanceof ApiError && err.code === "auth.email_already_registered") {
        setFormError("An account with that email already exists.");
        return;
      }
      setFormError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-bg px-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="items-center text-center">
          <Link href="/" className="mb-2 flex items-center gap-2 font-mono text-lg font-semibold text-fg">
            <Database className="h-5 w-5 text-accent" aria-hidden />
            DBArena
          </Link>
          <CardTitle>Create your account</CardTitle>
          <CardDescription>Start solving database problems.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
            <div>
              <Label htmlFor="displayName">Display name</Label>
              <Input id="displayName" autoComplete="name" {...register("displayName")} />
              <FieldError>{errors.displayName?.message}</FieldError>
            </div>
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" autoComplete="email" {...register("email")} />
              <FieldError>{errors.email?.message}</FieldError>
            </div>
            <div>
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" autoComplete="new-password" {...register("password")} />
              <FieldError>{errors.password?.message}</FieldError>
            </div>
            {formError && <p className="text-sm text-danger">{formError}</p>}
            <Button type="submit" disabled={isSubmitting} className="mt-2">
              {isSubmitting ? "Creating account…" : "Create account"}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-fg-muted">
            Already have an account?{" "}
            <Link href="/login" className="font-medium text-accent hover:underline">
              Log in
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
