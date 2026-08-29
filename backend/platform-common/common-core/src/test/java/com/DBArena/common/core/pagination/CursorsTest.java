package com.DBArena.common.core.pagination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorsTest {

    @Test
    void encodeThenDecodeRoundTrips() {
        String raw = "2026-08-28T00:00:00Z|01J000EXAMPLE";
        String cursor = Cursors.encode(raw);
        assertThat(cursor).doesNotContain("|");
        assertThat(Cursors.decode(cursor)).isEqualTo(raw);
    }

    @Test
    void decodeRejectsMalformedCursor() {
        assertThatThrownBy(() -> Cursors.decode("not-valid-base64!!"))
                .isInstanceOf(Cursors.InvalidCursorException.class);
    }

    @Test
    void pageRequestRejectsOutOfRangeLimit() {
        assertThatThrownBy(() -> new PageRequest(0, java.util.Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(PageRequest.MAX_LIMIT + 1, java.util.Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
