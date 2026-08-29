package com.DBArena.engine.adapters.postgres;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresIdentifiersTest {

    @Test
    void quotesAPlainIdentifier() {
        assertThat(PostgresIdentifiers.quote("numbers")).isEqualTo("\"numbers\"");
    }

    @Test
    void quotesAMixedCaseOrKeywordClashingIdentifierUnconditionally() {
        assertThat(PostgresIdentifiers.quote("Order")).isEqualTo("\"Order\"");
        assertThat(PostgresIdentifiers.quote("select")).isEqualTo("\"select\"");
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThatThrownBy(() -> PostgresIdentifiers.quote(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PostgresIdentifiers.quote(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnIdentifierContainingADoubleQuoteRatherThanEscapingIt() {
        assertThatThrownBy(() -> PostgresIdentifiers.quote("evil\" ; DROP TABLE users; --"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnIdentifierLongerThan63Bytes() {
        String tooLong = "a".repeat(64);
        assertThatThrownBy(() -> PostgresIdentifiers.quote(tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsExactly63Bytes() {
        String exactly63 = "a".repeat(63);
        assertThat(PostgresIdentifiers.quote(exactly63)).isEqualTo("\"" + exactly63 + "\"");
    }
}
