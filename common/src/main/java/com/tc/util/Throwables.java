/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
