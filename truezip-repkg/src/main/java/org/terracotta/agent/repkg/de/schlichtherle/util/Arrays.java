/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Arrays.java
 *
 * Created on 13. Oktober 2005, 02:46
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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
 * @author Christian Schlichtherle
 * @version @version@
 */
public class Arrays {

    /**
     * Compares <tt>max</tt> bytes at the specified offsets of the given
     * arrays.
     * If the remaining bytes at the given offset of any array is smaller than
     * <tt>max</tt> bytes, it must match the number of remaining bytes at the
     * given offset in the other array.
     */
    public static boolean equals(
            final byte[] b1,
            final int b1off,
            final byte[] b2,
            final int b2off,
            int max) {
        if (b1 == null)
            throw new NullPointerException("b1");
        if (b2 == null)
            throw new NullPointerException("b2");
        if (0 > b1off || b1off > b1.length)
            throw new IndexOutOfBoundsException("b1off = " + b1off + ": not in [0, " + b1.length + "[!");
        if (0 > b2off || b2off > b2.length)
            throw new IndexOutOfBoundsException("b2off = " + b2off + ": not in [0, " + b2.length + "[!");
        if (max < 1)
            throw new IllegalArgumentException("len = " + max + ": too small");

        final int b1rem = b1.length - b1off;
        final int b2rem = b2.length - b2off;
        if (max > b1rem) {
            max = b1rem;
            if (max != b2rem)
                return false;
        } else if (max > b2rem) {
            max = b2rem;
            if (max != b1rem)
                return false;
        }

        while (--max >= 0)
            if (b1[b1off + max] != b2[b2off + max])
                return false;

        return true;
    }
    
    protected Arrays() {
    }
}
