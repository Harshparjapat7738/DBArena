package com.DBArena.common.core.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    @Test
    void okMapsValue() {
        Result<Integer, String> ok = Result.ok(2);
        assertThat(ok.isOk()).isTrue();
        assertThat(ok.map(v -> v * 10).unwrap()).isEqualTo(20);
    }

    @Test
    void errShortCircuitsMap() {
        Result<Integer, String> err = Result.err("boom");
        assertThat(err.isErr()).isTrue();
        assertThat(err.map(v -> v * 10)).isEqualTo(Result.err("boom"));
    }

    @Test
    void flatMapChains() {
        Result<Integer, String> r = Result.<Integer, String>ok(2)
                .flatMap(v -> v > 0 ? Result.ok(v + 1) : Result.err("negative"));
        assertThat(r.unwrap()).isEqualTo(3);
    }

    @Test
    void getOrThrowThrowsMappedException() {
        Result<Integer, String> err = Result.err("bad input");
        assertThatThrownBy(() -> err.getOrThrow(IllegalStateException::new))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("bad input");
    }

    @Test
    void orElseFallsBackOnlyOnErr() {
        assertThat(Result.<Integer, String>ok(5).orElse(-1)).isEqualTo(5);
        assertThat(Result.<Integer, String>err("x").orElse(-1)).isEqualTo(-1);
    }
}
