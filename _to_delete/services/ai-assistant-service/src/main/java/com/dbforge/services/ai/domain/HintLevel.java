package com.dbforge.services.ai.domain;

/**
 * A graduated hint ladder, weakest to strongest. The learner picks the
 * level explicitly on each request (there is no server-side hint-history
 * or auto-escalation yet - see this milestone's Carried forward) - a
 * learner who wants to climb the ladder just asks again with the next
 * level.
 */
public enum HintLevel {

    /** Names the relevant concept ("this is a self-join problem") without touching the learner's query at all. */
    CONCEPT,

    /** Describes the shape of an approach in prose (which clauses/stages, in what order) - still no code. */
    APPROACH,

    /**
     * Points at what's specifically wrong or missing in the learner's own
     * submitted query, as close to the answer as this system gets - but
     * never the reference solution and never a complete, directly
     * copy-pasteable final query (enforced by {@link
     * com.dbforge.services.ai.prompt.HintPromptBuilder} and backstopped by
     * {@link com.dbforge.services.ai.guard.OutputGuard}).
     */
    NEAR_MISS
}
