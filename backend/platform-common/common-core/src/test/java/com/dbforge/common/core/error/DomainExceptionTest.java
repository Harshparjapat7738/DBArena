package com.dbforge.common.core.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void notFoundCarriesResourceAndId() {
        NotFoundException ex = new NotFoundException("Problem", "01J000EXAMPLE");
        assertThat(ex.status()).isEqualTo(404);
        assertThat(ex.code()).isEqualTo("resource.not_found");
        assertThat(ex.extensions()).containsEntry("resource", "Problem");
    }

    @Test
    void validationSummarizesFirstViolation() {
        ValidationException ex = new ValidationException(List.of(
                new FieldViolation("dataset.name", "must not be blank"),
                new FieldViolation("dataset.rows", "must not be empty")));
        assertThat(ex.status()).isEqualTo(422);
        assertThat(ex.violations()).hasSize(2);
        assertThat(ex.getMessage()).contains("dataset.name");
    }
}
