import { Badge } from "@dbforge/ui";
import type { Difficulty, EngineKind } from "@dbforge/api-client";

const DIFFICULTY_TONE: Record<Difficulty, "success" | "warning" | "danger"> = {
  EASY: "success",
  MEDIUM: "warning",
  HARD: "danger",
};

export function DifficultyBadge({ difficulty }: { difficulty: Difficulty }) {
  return (
    <Badge tone={DIFFICULTY_TONE[difficulty]}>
      {difficulty.charAt(0) + difficulty.slice(1).toLowerCase()}
    </Badge>
  );
}

const ENGINE_LABEL: Record<EngineKind, string> = {
  POSTGRES: "PostgreSQL",
  MYSQL: "MySQL",
  MONGODB: "MongoDB",
};

export function EngineBadge({ engine }: { engine: EngineKind }) {
  return <Badge tone="info">{ENGINE_LABEL[engine]}</Badge>;
}
