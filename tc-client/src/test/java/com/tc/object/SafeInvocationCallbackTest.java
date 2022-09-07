/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import org.junit.Test;
import org.terracotta.entity.InvocationCallback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SafeInvocationCallbackTest {

    @Test
    public void testSafeInvocationCallbackIsTypedCorrectly() {
        assertThat(SafeInvocationCallback.safe(mock(InvocationCallback.class)), is(instanceOf(SafeInvocationCallback.class)));
    }

    @Test
    public void testSafeInvocationCallbackCatchesAll() throws InvocationTargetException, IllegalAccessException {
        for (Method method : InvocationCallback.class.getDeclaredMethods()) {
            System.out.println(method);
            InvocationCallback<Object> callback = mock(InvocationCallback.class, inv -> {
                if (inv.getMethod().getDeclaringClass().equals(InvocationCallback.class)) {
                    throw new Throwable();
                } else {
                    return null;
                }
            });

            SafeInvocationCallback<Object> safe = SafeInvocationCallback.safe(callback);

            Object[] parameters = new Object[method.getParameterCount()];
            try {
                method.invoke(callback, parameters);
                fail("Expected Throwable");
            } catch (InvocationTargetException t) {
                assertThat(t.getCause().getClass(), is(equalTo(Throwable.class)));
            }
            method.invoke(safe, parameters);
            method.invoke(verify(callback, times(2)), parameters);
        }
    }
}
