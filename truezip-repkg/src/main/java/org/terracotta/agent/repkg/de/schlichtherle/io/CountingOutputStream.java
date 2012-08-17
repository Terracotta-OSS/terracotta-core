/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * CountingOutputStream.java
 *
 * Created on 4. Januar 2007, 14:19
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

package org.terracotta.agent.repkg.de.schlichtherle.io;

import java.io.*;

/**
 * An output stream which logs the number of bytes written.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
final class CountingOutputStream extends FilterOutputStream {
    private static volatile long total;
    private static volatile boolean reset;

    CountingOutputStream(OutputStream out) {
        super(out);
        init();
    }

    /** Returns the total number of bytes written. */
    static long getTotal() {
        return total;
    }

    /**
     * Resets the total number of bytes written if {@link #resetOnInit} has
     * been called before.
     */
    static void init() {
        if (reset) {
            reset = false;
            total = 0;
        }
    }

    /**
     * Requests that the total number of bytes written gets reset on the
     * next call to {@link #init}.
     */
    static void resetOnInit() {
        reset = true;
    }

    public void write(final int b) throws IOException {
        out.write(b);
        total++;
    }

    public void write(byte[] b) throws IOException {
        int len = b.length;
        out.write(b, 0, len);
        total += len;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        total += len;
    }
}
