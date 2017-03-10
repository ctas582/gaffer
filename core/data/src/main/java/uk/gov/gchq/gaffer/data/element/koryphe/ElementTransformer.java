/*
 * Copyright 2016 Crown Copyright
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

package uk.gov.gchq.gaffer.data.element.koryphe;

import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.koryphe.composite.Composite;
import uk.gov.gchq.koryphe.tuple.Tuple;
import uk.gov.gchq.koryphe.tuple.function.TupleFunction;
import uk.gov.gchq.koryphe.tuple.mask.TupleMask;
import uk.gov.gchq.koryphe.tuple.n.mask.TupleMaskN;
import java.util.function.Function;

public class ElementTransformer
        extends Composite<TupleFunction<String, ?, ?>>
        implements Function<Tuple<String>, Tuple<String>> {

    private final ElementTuple elementTuple = new ElementTuple();

    public Element apply(final Element element) {
        elementTuple.setElement(element);
        apply(elementTuple);
        return element;
    }

    @Override
    public Tuple<String> apply(final Tuple<String> input) {
        Tuple<String> result = input;
        for (TupleFunction<String, ?, ?> function : getFunctions()) {
            // Assume the output of one is the input of the next
            result = function.apply(result);
        }
        return result;
    }

    public static class Builder {
        private final ElementTransformer transformer;
        private TupleFunction currentFunction = new TupleFunction();
        private boolean selected;
        private boolean executed;
        private boolean projected;

        public Builder() {
            this(new ElementTransformer());
        }

        public Builder(final ElementTransformer transformer) {
            this.transformer = transformer;
        }

        public Builder select(final String... selection) {
            if (selected) {
                transformer.getFunctions().add(currentFunction);
                currentFunction = new TupleFunction();
                selected = false;
            }
            final TupleMask tupleMask = new TupleMaskN<>();
            tupleMask.setFields(selection);
            currentFunction.setSelection(tupleMask);
            selected = true;
            return this;
        }

        public Builder project(final String... projection) {
            if (projected) {
                transformer.getFunctions().add(currentFunction);
                currentFunction = new TupleFunction();
                projected = false;
            }
            final TupleMask tupleMask = new TupleMaskN<>();
            tupleMask.setFields(projection);
            currentFunction.setProjection(tupleMask);
            projected = true;
            return this;
        }

        public Builder execute(final Function function) {
            if (executed) {
                transformer.getFunctions().add(currentFunction);
                currentFunction = new TupleFunction();
                executed = false;
            }
            currentFunction.setFunction(function);
            executed = true;
            return this;
        }

        public ElementTransformer build() {
            if (executed || selected || projected) {
                transformer.getFunctions().add(currentFunction);
                currentFunction = new TupleFunction();
                selected = false;
                executed = false;
                projected = false;
            }
            return transformer;
        }
    }
}
