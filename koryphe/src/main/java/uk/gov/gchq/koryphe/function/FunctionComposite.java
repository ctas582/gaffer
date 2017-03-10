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

package uk.gov.gchq.koryphe.function;

import uk.gov.gchq.koryphe.composite.Composite;
import javax.xml.transform.Transformer;
import java.util.function.Function;

/**
 * A composite {@link Transformer} that applies each transformer in turn, supplying the result of each transformer as
 * the input to the next, and returning the result of the last transformer. Transformer input/output types are assumed
 * to be compatible - no checking is done, and a class cast exception will be thrown if incompatible transformers are
 * executed.
 *
 * @param <I> Type of input of first transformer
 * @param <O> Type of output of last transformer
 */
public class FunctionComposite<I, O> extends Composite<Function> implements Function<I, O> {
    @Override
    public O apply(final I input) {
        Object result = input;
        for (Function function : getFunctions()) {
            // Assume the output of one is the input of the next
            result = function.apply(result);
        }
        return (O) result;
    }
}
