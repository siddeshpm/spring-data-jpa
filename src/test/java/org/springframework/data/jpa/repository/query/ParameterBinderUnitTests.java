/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryCreatorUnitTests.SampleEmbeddable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Parameters;


/**
 * Unit test for {@link ParameterBinder}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterBinderUnitTests {

    private Method valid;

    @Mock
    private Query query;
    private Method useIndexedParameters;
    private Method indexedParametersWithSort;


    @Before
    public void setUp() throws SecurityException, NoSuchMethodException {

        valid = SampleDao.class.getMethod("valid", String.class);

        useIndexedParameters =
                SampleDao.class.getMethod("useIndexedParameters", String.class);
        indexedParametersWithSort =
                SampleDao.class.getMethod("indexedParameterWithSort",
                        String.class, Sort.class);
    }

    static class User {

    }

    static interface SampleDao {

        User useIndexedParameters(String lastname);


        User indexedParameterWithSort(String lastname, Sort sort);


        User valid(@Param("username") String username);


        User validWithPageable(@Param("username") String username,
                Pageable pageable);


        User validWithSort(@Param("username") String username, Sort sort);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsToManyParameters() throws Exception {

        new ParameterBinder(new Parameters(valid),
                new Object[] { "foo", "bar" });
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullParameters() throws Exception {

        new ParameterBinder(new Parameters(valid), (Object[]) null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsToLittleParameters() throws SecurityException,
            NoSuchMethodException {

        Parameters parameters = new Parameters(valid);
        new ParameterBinder(parameters);
    }


    @Test
    public void returnsNullIfNoPageableWasProvided() throws SecurityException,
            NoSuchMethodException {

        Method method =
                SampleDao.class.getMethod("validWithPageable", String.class,
                        Pageable.class);

        Parameters parameters = new Parameters(method);
        ParameterBinder binder =
                new ParameterBinder(parameters, new Object[] { "foo", null });

        assertThat(binder.getPageable(), is(nullValue()));
    }


    @Test
    public void bindWorksWithNullForSort() throws Exception {

        Method validWithSort =
                SampleDao.class.getMethod("validWithSort", String.class,
                        Sort.class);

        new ParameterBinder(new Parameters(validWithSort), new Object[] {
                "foo", null }).bind(query);
        verify(query).setParameter(eq(1), eq("foo"));
    }


    @Test
    public void bindWorksWithNullForPageable() throws Exception {

        Method validWithPageable =
                SampleDao.class.getMethod("validWithPageable", String.class,
                        Pageable.class);

        new ParameterBinder(new Parameters(validWithPageable), new Object[] {
                "foo", null }).bind(query);
        verify(query).setParameter(eq(1), eq("foo"));
    }


    @Test
    public void usesIndexedParametersIfNoParamAnnotationPresent()
            throws Exception {

        new ParameterBinder(new Parameters(useIndexedParameters),
                new Object[] { "foo" }).bind(query);
        verify(query).setParameter(eq(1), anyObject());
    }


    @Test
    public void usesParameterNameIfAnnotated() throws Exception {

        when(query.setParameter(eq("username"), anyObject())).thenReturn(query);
        new ParameterBinder(new Parameters(valid), new Object[] { "foo" }) {

            @Override
            boolean hasNamedParameter(Query query) {

                return true;
            }
        }.bind(query);
        verify(query).setParameter(eq("username"), anyObject());
    }


    @Test
    public void bindsEmbeddableCorrectly() throws Exception {

        Method method =
                QueryCreatorUnitTests.class.getMethod("findByEmbeddable",
                        SampleEmbeddable.class);
        Parameters parameters = new Parameters(method);
        SampleEmbeddable embeddable = new SampleEmbeddable();

        new ParameterBinder(parameters, new Object[] { embeddable })
                .bind(query);

        verify(query).setParameter(1, embeddable);
    }


    @Test
    public void bindsSortForIndexedParameters() throws Exception {

        Sort sort = new Sort("name");
        ParameterBinder binder =
                new ParameterBinder(new Parameters(indexedParametersWithSort),
                        new Object[] { "name", sort });
        assertThat(binder.getSort(), is(sort));
    }
}
