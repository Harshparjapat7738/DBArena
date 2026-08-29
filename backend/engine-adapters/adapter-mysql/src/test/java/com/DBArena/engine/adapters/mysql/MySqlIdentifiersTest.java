package com.DBArena.engine.adapters.mysql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlIdentifiersTest {

    @Test
    void quotesAPlainIdentifier() {
        assertThat(MySqlIdentifiers.quote("numbers")).isEqualTo("`numbers`");
    }

    @Test
    void quotesAMixedCaseOrKeywordClashingIdentifierUnconditionally() {
        assertThat(MySqlIdentifiers.quote("Order")).isEqualTo("`Order`");
        assertThat(MySqlIdentifiers.quote("select")).isEqualTo("`select`");
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThatThrownBy(() -> MySqlIdentifiers.quote(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MySqlIdentifiers.quote(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnIdentifierContainingABacktickRatherThanDoublingIt() {
        assertThatThrownBy(() -> MySqlIdentifiers.quote("evil` ; DROP TABLE users; --"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnIdentifierLongerThan64Characters() {
        String tooLong = "a".repeat(65);
        assertThatThrownBy(() -> MySqlIdentifiers.quote(tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsExactly64Characters() {
        String exactly64 = "a".repeat(64);
        assertThat(MySqlIdentifiers.quote(exactly64)).isEqualTo("`" + exactly64 + "`");
    }

    @Test
    void qualifyJoinsAQuotedSchemaAndObjectNameWithADot() {
        assertThat(MySqlIdentifiers.qualify("DBArena_x", "numbers")).isEqualTo("`DBArena_x`.`numbers`");
    }
}
