/*
 * Copyright 2016-2021 Crown Copyright
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

package uk.gov.gchq.gaffer.operation.impl.output;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.gaffer.operation.OperationTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ToStreamTest extends OperationTest<ToStream> {

    @Test
    @Override
    public void builderShouldCreatePopulatedOperation() {
        // Given
        final ToStream<String> toStream = new ToStream.Builder<String>().input("1", "2").build();

        // Then
        assertThat(toStream.getInput())
                .hasSize(2)
                .containsOnly("1", "2");
    }

    @Test
    @Override
    public void shouldShallowCloneOperation() {
        // Given
        final String input = "1";
        final ToStream toStream = new ToStream.Builder<>()
                .input(input)
                .build();

        // When
        final ToStream clone = toStream.shallowClone();

        // Then
        assertNotSame(toStream, clone);
        assertEquals(Lists.newArrayList(input), clone.getInput());
    }

    @Test
    public void shouldGetOutputClass() {
        // When
        final Class<?> outputClass = getTestObject().getOutputClass();

        // Then
        assertEquals(Stream.class, outputClass);
    }

    @Override
    protected ToStream getTestObject() {
        return new ToStream();
    }
}
