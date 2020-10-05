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
package com.tc.util;

/**
 * @author vmad
 */
public class Throwables {
    public static Throwable getRootCause(Throwable throwable) {
        Throwable tmp;
        while((tmp = throwable.getCause()) != null) {
            throwable = tmp;
        }
        return throwable;
    }

    public static RuntimeException propagate(Throwable throwable) throws RuntimeException, Error {
        propagateIfInstanceOf(throwable, RuntimeException.class);
        propagateIfInstanceOf(throwable, Error.class);
        return new RuntimeException(throwable);
    }

    public static <X extends Throwable> void propagateIfInstanceOf(Throwable throwable, Class<X> clazz) throws X {
        if(throwable != null && clazz.isInstance(throwable)) {
            throw clazz.cast(throwable);
        }
    }
}
