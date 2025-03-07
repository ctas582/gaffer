/*
 * Copyright 2017-2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.store.operation.handler;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.commonutil.exception.LimitExceededException;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.impl.GenerateSplitPointsFromSample;
import uk.gov.gchq.gaffer.operation.impl.SampleElementsForSplitPoints;
import uk.gov.gchq.gaffer.serialisation.implementation.StringSerialiser;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.TestTypes;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaEdgeDefinition;
import uk.gov.gchq.gaffer.store.schema.SchemaEntityDefinition;
import uk.gov.gchq.gaffer.store.schema.TypeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public abstract class AbstractSampleElementsForSplitPointsHandlerTest<S extends Store> {

    protected Schema schema = new Schema.Builder()
            .entity(TestGroups.ENTITY, new SchemaEntityDefinition.Builder()
                    .vertex(TestTypes.ID_STRING)
                    .build())
            .edge(TestGroups.EDGE, new SchemaEdgeDefinition.Builder()
                    .source(TestTypes.ID_STRING)
                    .destination(TestTypes.ID_STRING)
                    .directed(TestTypes.DIRECTED_EITHER)
                    .build())
            .type(TestTypes.ID_STRING, new TypeDefinition.Builder()
                    .clazz(String.class)
                    .serialiser(new StringSerialiser())
                    .build())
            .type(TestTypes.DIRECTED_EITHER, Boolean.class)
            .vertexSerialiser(new StringSerialiser())
            .build();

    @Test
    public void shouldThrowExceptionForNullInput() throws OperationException {
        // Given
        final AbstractSampleElementsForSplitPointsHandler<?, S> handler = createHandler();
        final SampleElementsForSplitPoints operation = new SampleElementsForSplitPoints.Builder<>()
                .numSplits(1)
                .build();

        // When / Then
        assertThatExceptionOfType(OperationException.class)
                .isThrownBy(() -> handler.doOperation(operation, new Context(), createStore()))
                .withMessageContaining("input is required");
    }

    @Test
    public void shouldThrowExceptionIfNumberOfSampledElementsIsMoreThanMaxAllowed() throws OperationException {
        // Given
        int maxSampledElements = 5;
        final AbstractSampleElementsForSplitPointsHandler<?, ?> handler = createHandler();
        handler.setMaxSampledElements(maxSampledElements);
        final List<Element> elements =
                IntStream.range(0, 6)
                        .mapToObj(i -> new Entity(TestGroups.ENTITY, "vertex_" + i))
                        .collect(Collectors.toList());
        final SampleElementsForSplitPoints operation = new SampleElementsForSplitPoints.Builder<>()
                .input(elements)
                .numSplits(3)
                .build();

        // When / Then
        try {
            handler.doOperation(operation, new Context(), createStore());
            fail("Exception expected");
        } catch (final LimitExceededException e) {
            assertTrue(e.getMessage().equals("Limit of " + maxSampledElements + " exceeded."), e.getMessage());
        }
    }

    @Test
    public void shouldNotThrowExceptionIfNumberOfSampledElementsIsLessThanMaxAllowed() throws OperationException {
        // Given
        int maxSampledElements = 5;
        final AbstractSampleElementsForSplitPointsHandler<?, ?> handler = createHandler();
        handler.setMaxSampledElements(maxSampledElements);
        final List<Element> elements =
                IntStream.range(0, 5)
                        .mapToObj(i -> new Entity(TestGroups.ENTITY, "vertex_" + i))
                        .collect(Collectors.toList());
        elements.add(null);

        final SampleElementsForSplitPoints operation = new SampleElementsForSplitPoints.Builder<>()
                .input(elements)
                .numSplits(3)
                .build();

        // When
        handler.doOperation(operation, new Context(), createStore());

        // Then - no exception
    }

    @Test
    public void shouldUseFullSampleOfAllElementsByDefault() throws OperationException {
        // Given
        final int numSplits = 3;
        final List<Element> elements =
                IntStream.range(0, numSplits)
                        .mapToObj(i -> new Entity(TestGroups.ENTITY, "vertex_" + i))
                        .collect(Collectors.toList());

        final AbstractSampleElementsForSplitPointsHandler<?, S> handler = createHandler();
        final SampleElementsForSplitPoints operation = new SampleElementsForSplitPoints.Builder<>()
                .input(elements)
                .numSplits(numSplits)
                .build();

        final S store = createStore();

        // When
        handler.doOperation(operation, new Context(), store);

        // Then
        final ArgumentCaptor<GenerateSplitPointsFromSample> generateSplitPointsFromSampleCaptor = ArgumentCaptor.forClass(GenerateSplitPointsFromSample.class);
        verify(store).execute(generateSplitPointsFromSampleCaptor.capture(), any(Context.class));
        assertExpectedNumberOfSplitPointsAndSampleSize(generateSplitPointsFromSampleCaptor, numSplits, elements.size());
    }

    @Test
    public void shouldFilterOutNulls() throws OperationException {
        // Given
        final int numSplits = 3;
        final List<Element> elements =
                IntStream.range(0, numSplits)
                        .mapToObj(i -> new Entity(TestGroups.ENTITY, "vertex_" + i))
                        .collect(Collectors.toList());
        final List<Element> elementsWithNulls = new ArrayList<>();
        elementsWithNulls.add(null);
        elementsWithNulls.addAll(elements);
        elementsWithNulls.add(null);

        final AbstractSampleElementsForSplitPointsHandler<?, S> handler = createHandler();
        final SampleElementsForSplitPoints operation = new SampleElementsForSplitPoints.Builder<>()
                .input(elementsWithNulls)
                .numSplits(numSplits)
                .build();

        final S store = createStore();

        // When
        handler.doOperation(operation, new Context(), store);

        // Then
        final ArgumentCaptor<GenerateSplitPointsFromSample> generateSplitPointsFromSampleCaptor = ArgumentCaptor.forClass(GenerateSplitPointsFromSample.class);
        verify(store).execute(generateSplitPointsFromSampleCaptor.capture(), any(Context.class));
        assertExpectedNumberOfSplitPointsAndSampleSize(generateSplitPointsFromSampleCaptor, numSplits, elements.size());
    }

    @Test
    public void shouldSampleApproximatelyHalfOfElements() throws OperationException {
        // Given
        final int numSplits = 3;
        final List<Element> elements =
                IntStream.range(0, 1000 * numSplits)
                        .mapToObj(i -> new Entity(TestGroups.ENTITY, "vertex_" + i))
                        .collect(Collectors.toList());

        final AbstractSampleElementsForSplitPointsHandler<?, S> handler = createHandler();
        final SampleElementsForSplitPoints operation = new SampleElementsForSplitPoints.Builder<>()
                .input(elements)
                .numSplits(numSplits)
                .proportionToSample(0.5f)
                .build();

        final S store = createStore();

        // When
        handler.doOperation(operation, new Context(), store);

        // Then
        final ArgumentCaptor<GenerateSplitPointsFromSample> generateSplitPointsFromSampleCaptor = ArgumentCaptor.forClass(GenerateSplitPointsFromSample.class);
        verify(store).execute(generateSplitPointsFromSampleCaptor.capture(), any(Context.class));
        final int maximumExpectedSampleSize = (int) ((elements.size() / 2) * 1.1);
        assertExpectedNumberOfSplitPointsAndSampleSizeOfNoMoreThan(generateSplitPointsFromSampleCaptor, numSplits, maximumExpectedSampleSize);
    }

    protected abstract S createStore();

    protected abstract AbstractSampleElementsForSplitPointsHandler<?, S> createHandler();

    protected void assertExpectedNumberOfSplitPointsAndSampleSize(
            final ArgumentCaptor<GenerateSplitPointsFromSample> generateSplitPointsFromSampleCaptor,
            final int expectedNumSplits,
            final int expectedSampleSize) {

        assertEquals(expectedNumSplits, generateSplitPointsFromSampleCaptor.getValue().getNumSplits().intValue());

        final long sampleSize = StreamSupport.stream(generateSplitPointsFromSampleCaptor.getValue().getInput().spliterator(), false).count();

        assertEquals(expectedSampleSize, sampleSize);
    }

    private void assertExpectedNumberOfSplitPointsAndSampleSizeOfNoMoreThan(
            final ArgumentCaptor<GenerateSplitPointsFromSample> generateSplitPointsFromSampleCaptor,
            final int expectedNumSplits,
            final int maximumExpectedSampleSize) {

        assertEquals(expectedNumSplits, generateSplitPointsFromSampleCaptor.getValue().getNumSplits().intValue());

        final long sampleSize = StreamSupport.stream(generateSplitPointsFromSampleCaptor.getValue().getInput().spliterator(), false).count();

        assertTrue(maximumExpectedSampleSize > sampleSize);
    }
}
