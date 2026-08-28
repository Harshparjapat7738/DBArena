// TypeScript 7 requires an ambient module declaration for side-effect CSS
// imports (e.g. `import "./globals.css"` in the root layout); Next's own
// shipped types don't declare this yet, so it's declared here.
declare module "*.css";
