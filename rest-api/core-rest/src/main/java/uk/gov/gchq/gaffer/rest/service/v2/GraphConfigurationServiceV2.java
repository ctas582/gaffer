/*
 * Copyright 2016-2020 Crown Copyright
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

package uk.gov.gchq.gaffer.rest.service.v2;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;

import uk.gov.gchq.gaffer.data.generator.ElementGenerator;
import uk.gov.gchq.gaffer.data.generator.ObjectGenerator;
import uk.gov.gchq.gaffer.rest.SystemProperty;
import uk.gov.gchq.gaffer.rest.factory.GraphFactory;
import uk.gov.gchq.gaffer.rest.factory.UserFactory;
import uk.gov.gchq.gaffer.serialisation.util.JsonSerialisationUtil;
import uk.gov.gchq.koryphe.serialisation.json.SimpleClassNameIdResolver;
import uk.gov.gchq.koryphe.signature.Signature;
import uk.gov.gchq.koryphe.util.ReflectionUtil;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

import static uk.gov.gchq.gaffer.rest.ServiceConstants.GAFFER_MEDIA_TYPE;
import static uk.gov.gchq.gaffer.rest.ServiceConstants.GAFFER_MEDIA_TYPE_HEADER;

/**
 * An implementation of {@link uk.gov.gchq.gaffer.rest.service.v2.IGraphConfigurationServiceV2}. By default it will use a singleton
 * {@link uk.gov.gchq.gaffer.graph.Graph} generated using the {@link uk.gov.gchq.gaffer.rest.factory.GraphFactory}.
 * <p>
 * Currently the {@link uk.gov.gchq.gaffer.operation.Operation}s, {@link java.util.function.Predicate}s,
 * {@link java.util.function.Function}s and {@link uk.gov.gchq.gaffer.data.generator.ElementGenerator}s available
 * are only returned if they are in a package prefixed with 'gaffer'.
 */
public class GraphConfigurationServiceV2 implements IGraphConfigurationServiceV2 {
    @Inject
    private GraphFactory graphFactory;

    @Inject
    private UserFactory userFactory;

    public GraphConfigurationServiceV2() {
        updateReflectionPaths();
    }

    public static void initialise() {
        // Invoking this method will cause the static lists to be populated.
        updateReflectionPaths();
    }

    @Override
    public Response getSchema() {
        return Response.ok(graphFactory.getGraph().getSchema())
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getFilterFunction() {
        return Response.ok(ReflectionUtil.getSubTypes(Predicate.class))
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getAggregationFunctions() {
        return Response.ok(ReflectionUtil.getSubTypes(BinaryOperator.class))
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Need to wrap all runtime exceptions before they are given to the user")
    @Override
    public Response getFilterFunction(final String inputClass) {
        if (StringUtils.isEmpty(inputClass)) {
            return getFilterFunction();
        }

        final Class<?> clazz;
        try {
            clazz = Class.forName(SimpleClassNameIdResolver.getClassName(inputClass));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Input class was not recognised: " + inputClass, e);
        }

        final Set<Class> classes = new HashSet<>();
        for (final Class functionClass : ReflectionUtil.getSubTypes(Predicate.class)) {
            try {
                final Predicate function = (Predicate) functionClass.newInstance();
                final Signature signature = Signature.getInputSignature(function);
                if (null == signature.getNumClasses()
                        || (1 == signature.getNumClasses() &&
                        (Signature.UnknownGenericType.class.isAssignableFrom(signature
                                .getClasses()[0])
                                || signature.getClasses()[0].isAssignableFrom(clazz)))) {
                    classes.add(functionClass);
                }
            } catch (final Exception e) {
                // just add the function.
                classes.add(functionClass);
            }
        }

        return Response.ok(classes)
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Need to wrap all runtime exceptions before they are given to the user")
    @Override
    public Response getSerialisedFields(final String className) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(SimpleClassNameIdResolver.getClassName(className));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Class name was not recognised: " + className, e);
        }

        final ObjectMapper mapper = new ObjectMapper();
        final JavaType type = mapper.getTypeFactory().constructType(clazz);
        final BeanDescription introspection = mapper.getSerializationConfig()
                .introspect(type);
        final List<BeanPropertyDefinition> properties = introspection.findProperties();

        final Set<String> fields = new HashSet<>();
        for (final BeanPropertyDefinition property : properties) {
            fields.add(property.getName());
        }

        return Response.ok(fields)
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Need to wrap all runtime exceptions before they are given to the user")
    @Override
    public Response getSerialisedFieldClasses(final String className) {
        return Response.ok(JsonSerialisationUtil.getSerialisedFieldClasses(className))
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getDescription() {
        return Response.ok(graphFactory.getGraph().getDescription())
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getGraphId() {
        return Response.ok(graphFactory.getGraph().getGraphId())
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getTransformFunctions() {
        return Response.ok(ReflectionUtil.getSubTypes(Function.class))
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getStoreTraits() {
        return Response.ok(graphFactory.getGraph().getStoreTraits())
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getElementGenerators() {
        return Response.ok(ReflectionUtil.getSubTypes(ElementGenerator.class))
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getObjectGenerators() {
        return Response.ok(ReflectionUtil.getSubTypes(ObjectGenerator.class))
                .header(GAFFER_MEDIA_TYPE_HEADER, GAFFER_MEDIA_TYPE)
                .build();
    }

    private static void updateReflectionPaths() {
        ReflectionUtil.addReflectionPackages(System.getProperty(SystemProperty.PACKAGE_PREFIXES, SystemProperty.PACKAGE_PREFIXES_DEFAULT));
    }
}
