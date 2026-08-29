package com.DBArena.services.catalog.service;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.Difficulty;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.Problem;
import com.DBArena.services.catalog.domain.ProblemFilter;
import com.DBArena.services.catalog.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogServiceTest {

    private ProblemRepository repository;
    private IdGenerator idGenerator;
    private Clock clock;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProblemRepository.class);
        idGenerator = mock(IdGenerator.class);
        clock = Clock.fixed(Instant.ofEpochMilli(5_000L), ZoneOffset.UTC);
        service = new CatalogService(repository, idGenerator, clock);
    }

    private static Problem sampleProblem(boolean published) {
        return new Problem(
                TypedId.of("id-1"), "two-sum", "Two Sum", "Statement", Difficulty.EASY,
                Set.of("arrays"), Set.of(EngineType.POSTGRES), "two-sum-dataset", published, 1000L, 1000L);
    }

    @Test
    void createProblemRejectsADuplicateSlug() {
        when(repository.existsBySlug("two-sum")).thenReturn(true);

        CreateProblemCommand command = new CreateProblemCommand(
                "two-sum", "Two Sum", "Statement", Difficulty.EASY, Set.of("arrays"),
                Set.of(EngineType.POSTGRES), "two-sum-dataset");

        assertThatThrownBy(() -> service.createProblem(command)).isInstanceOf(DuplicateSlugException.class);
        verify(repository, never()).insert(any());
    }

    @Test
    void createProblemInsertsAnUnpublishedProblemStampedWithTheClock() {
        when(repository.existsBySlug("two-sum")).thenReturn(false);
        when(idGenerator.nextTyped()).thenReturn(TypedId.of("id-1"));

        CreateProblemCommand command = new CreateProblemCommand(
                "two-sum", "Two Sum", "Statement", Difficulty.EASY, Set.of("arrays"),
                Set.of(EngineType.POSTGRES), "two-sum-dataset");

        Problem created = service.createProblem(command);

        assertThat(created.published()).isFalse();
        assertThat(created.createdAtEpochMillis()).isEqualTo(5000L);
        assertThat(created.updatedAtEpochMillis()).isEqualTo(5000L);

        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue()).isEqualTo(created);
    }

    @Test
    void getPublishedProblemBySlugThrowsNotFoundForAnUnpublishedProblem() {
        when(repository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem(false)));

        assertThatThrownBy(() -> service.getPublishedProblemBySlug("two-sum"))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void getPublishedProblemBySlugReturnsAPublishedProblem() {
        when(repository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem(true)));

        assertThat(service.getPublishedProblemBySlug("two-sum")).isEqualTo(sampleProblem(true));
    }

    @Test
    void getAnyProblemBySlugThrowsNotFoundWhenAbsent() {
        when(repository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAnyProblemBySlug("missing"))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void setPublishedTogglesAndStampsUpdatedAt() {
        when(repository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem(false)));

        Problem published = service.setPublished("two-sum", true);

        assertThat(published.published()).isTrue();
        assertThat(published.updatedAtEpochMillis()).isEqualTo(5000L);
        assertThat(published.createdAtEpochMillis()).isEqualTo(1000L); // unchanged
        verify(repository).replace(published);
    }

    @Test
    void updateProblemPreservesIdSlugPublishedAndCreatedAt() {
        when(repository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem(true)));

        UpdateProblemCommand command = new UpdateProblemCommand(
                "Two Sum (Revised)", "New statement", Difficulty.MEDIUM, Set.of("arrays", "hash-map"),
                Set.of(EngineType.POSTGRES, EngineType.MONGODB), "two-sum-dataset-v2");

        Problem updated = service.updateProblem("two-sum", command);

        assertThat(updated.id()).isEqualTo(TypedId.of("id-1"));
        assertThat(updated.slug()).isEqualTo("two-sum");
        assertThat(updated.published()).isTrue();
        assertThat(updated.createdAtEpochMillis()).isEqualTo(1000L);
        assertThat(updated.updatedAtEpochMillis()).isEqualTo(5000L);
        assertThat(updated.title()).isEqualTo("Two Sum (Revised)");
        assertThat(updated.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(updated.version()).isEqualTo(2); // sampleProblem() defaults to version 1
    }

    @Test
    void browsePublishedProblemsAlwaysForcesPublishedOnlyRegardlessOfTheIncomingFilter() {
        when(repository.findPage(any(), any())).thenReturn(CursorPage.lastPage(java.util.List.of()));

        ProblemFilter incoming = new ProblemFilter(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false);
        service.browsePublishedProblems(incoming, PageRequest.first());

        ArgumentCaptor<ProblemFilter> captor = ArgumentCaptor.forClass(ProblemFilter.class);
        verify(repository).findPage(captor.capture(), any());
        assertThat(captor.getValue().publishedOnly()).isTrue();
    }

    @Test
    void browsePublishedProblemsWithSortForcesPublishedOnlyAndPreservesDatasetAndSlugFilters() {
        when(repository.findPage(any(), any(), any())).thenReturn(CursorPage.lastPage(java.util.List.of()));

        ProblemFilter incoming = new ProblemFilter(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false,
                Optional.of("sales-dataset"), Optional.of(Set.of("two-sum")), Optional.empty());

        service.browsePublishedProblems(incoming, PageRequest.first(), com.DBArena.services.catalog.domain.ProblemSort.NEWEST_FIRST);

        ArgumentCaptor<ProblemFilter> filterCaptor = ArgumentCaptor.forClass(ProblemFilter.class);
        ArgumentCaptor<com.DBArena.services.catalog.domain.ProblemSort> sortCaptor =
                ArgumentCaptor.forClass(com.DBArena.services.catalog.domain.ProblemSort.class);
        verify(repository).findPage(filterCaptor.capture(), any(), sortCaptor.capture());

        assertThat(filterCaptor.getValue().publishedOnly()).isTrue();
        assertThat(filterCaptor.getValue().datasetSlug()).contains("sales-dataset");
        assertThat(filterCaptor.getValue().slugIn()).contains(Set.of("two-sum"));
        assertThat(sortCaptor.getValue()).isEqualTo(com.DBArena.services.catalog.domain.ProblemSort.NEWEST_FIRST);
    }
}
