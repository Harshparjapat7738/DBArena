import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  transpilePackages: ["@dbforge/ui", "@dbforge/api-client"],
};

export default nextConfig;
