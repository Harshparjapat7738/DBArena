import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  transpilePackages: ["@DBArena/ui", "@DBArena/api-client"],
};

export default nextConfig;
