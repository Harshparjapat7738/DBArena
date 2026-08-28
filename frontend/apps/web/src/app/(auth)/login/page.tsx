"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError } from "@dbforge/api-client";
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, FieldError, Input, Label } from "@dbforge/ui";
import { Database } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { authApi } from "@/lib/api/clients";
import { useAuthStore } from "@/lib/auth/authStore";

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
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
      const res = await authApi.login(values);
      setSession(res.accessToken, res.user);
      router.push("/catalog");
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-bg px-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="items-center text-center">
          <Link href="/" className="mb-2 flex items-center gap-2 font-mono text-lg font-semibold text-fg">
            <Database className="h-5 w-5 text-accent" aria-hidden />
            DBForge
          </Link>
          <CardTitle>Welcome back</CardTitle>
          <CardDescription>Log in to keep practicing.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" autoComplete="email" {...register("email")} />
              <FieldError>{errors.email?.message}</FieldError>
            </div>
            <div>
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" autoComplete="current-password" {...register("password")} />
              <FieldError>{errors.password?.message}</FieldError>
            </div>
            {formError && <p className="text-sm text-danger">{formError}</p>}
            <Button type="submit" disabled={isSubmitting} className="mt-2">
              {isSubmitting ? "Logging in…" : "Log in"}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-fg-muted">
            No account?{" "}
            <Link href="/register" className="font-medium text-accent hover:underline">
              Sign up
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
