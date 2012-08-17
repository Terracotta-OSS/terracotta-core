/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ThreadLocalLong.java
 *
 * Created on 25. Juli 2006, 13:39
 */
/*
 * Copyright 2006 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.util;

/**
 * A long which's value is local to each thread.
 * Its initial value is <code>0</code>.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.2
 */
public class ThreadLocalLong extends ThreadLocal {

    public final long getValue() {
        return ((Holder) get()).value;
    }

    public final void setValue(long value) {
        ((Holder) get()).value = value;
    }

    protected final Object initialValue() {
        return new Holder();
    }

    private final static class Holder {
        long value;
    }
}