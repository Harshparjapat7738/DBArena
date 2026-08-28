// `eslint-config-next`'s bundled presets are still legacy-format and go
// through `@rushstack/eslint-patch` to work with flat config; that patch
// does not recognize ESLint 10's internals and throws on load. Rather than
// pin ESLint back to 9 for one dependency, the equivalent rule sets are
// composed directly here: `typescript-eslint`'s recommended config,
// `eslint-plugin-react-hooks`, and `@next/eslint-plugin-next` (the actual
// Next-specific rules `eslint-config-next` itself just re-exports).
import js from "@eslint/js";
import nextPlugin from "@next/eslint-plugin-next";
import reactHooks from "eslint-plugin-react-hooks";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: [".next/**", "node_modules/**", "next-env.d.ts"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    plugins: {
      "react-hooks": reactHooks,
      "@next/next": nextPlugin,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...nextPlugin.configs.recommended.rules,
      ...nextPlugin.configs["core-web-vitals"].rules,
      "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
    },
  },
);
