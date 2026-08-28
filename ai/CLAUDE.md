# ai/ — Assistant prompts, context schemas, evals

This directory holds the *assets*. The service that uses them is
`backend/services/ai-assistant-service`.

## Layout

```
prompts/
  system/           base system prompt(s), one file per role
  tiers/            L1-nudge, L2-approach, L3-skeleton, L4-solution
  templates/        error-explanation, cross-engine-translation, editorial-draft
schemas/            JSON Schema for the context envelope and the expected output shapes
evals/
  fixtures/         frozen problems + schemas + sample rows
  cases/            expected behaviour per tier (what must and must not appear)
cost/               token budgets, model routing table, cost dashboards config
```

## Non-negotiable safety rules

1. **≤10 sample rows per entity.** Enforced in Java in `ContextBuilder`, hard-coded.
   Never a config value. Never adjustable by prompt.
2. **The reference solution and hidden-run values are never in the envelope.** They live in
   `catalog-service`, which `ai-assistant-service` cannot read. This is an architectural
   guarantee, not a prompt instruction.
3. **Dataset content is untrusted.** Sample rows are passed as a labelled JSON data block,
   never interpolated into instruction text. Imported user datasets can and will contain
   prompt-injection attempts.
4. **Model output is never auto-executed.** Generated queries are inserted into the editor
   for the user to run.
5. **Tier gates are server-side.** L2 needs ≥1 attempt or 3 min; L3 ≥2 attempts or 8 min;
   L4 ≥3 attempts, 15 min, or an already-solved problem.

## Writing or editing a prompt

Every change to a file in `prompts/` requires a corresponding case in `evals/cases/`.
An eval case asserts both what the output must contain and what it must **not** — an L1
nudge that leaks syntax is a regression even if it's helpful.

L4 output must follow the fixed seven-part structure in `docs/02` §6.4 (query, why this
shape, clause by clause, why not the alternatives, edge cases, performance note,
cross-engine equivalent).

## Cost

Tier → model routing lives in `cost/routing.yaml`. Static half of the envelope (problem +
schema + samples) is identical across users — prompt caching should exceed 80% hit rate.
If it doesn't, something is being interpolated that shouldn't be.
