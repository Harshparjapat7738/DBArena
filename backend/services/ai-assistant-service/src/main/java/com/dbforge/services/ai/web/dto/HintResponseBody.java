package com.dbforge.services.ai.web.dto;

import com.dbforge.services.ai.domain.HintLevel;
import com.dbforge.services.ai.domain.HintResult;

public record HintResponseBody(String problemSlug, HintLevel level, String hint, String provider, boolean truncated) {

    public static HintResponseBody from(HintResult result) {
        return new HintResponseBody(result.problemSlug(), result.level(), result.hint(), result.provider(),
                result.truncated());
    }
}
