/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * CountingReadOnlyFile.java
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

import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;

/**
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
final class CountingReadOnlyFile extends FilterReadOnlyFile {
    private static volatile long total;
    private static volatile boolean reset;

    CountingReadOnlyFile(ReadOnlyFile rof) {
        super(rof);
        init();
    }

    /** Returns the total number of bytes read. */
    static long getTotal() {
        return total;
    }

    /**
     * Resets the total number of bytes read if {@link #resetOnInit} has been
     * called before.
     */
    static void init() {
        if (reset) {
            reset = false;
            total = 0;
        }
    }

    /**
     * Requests that the total number of bytes read gets reset on the
     * next call to {@link #init}.
     */
    static void resetOnInit() {
        reset = true;
    }

    public int read() throws IOException {
        int ret = rof.read();
        if (ret != -1)
            total++;
        return ret;
    }

    public int read(byte[] b) throws IOException {
        int ret = rof.read(b);
        if (ret != -1)
            total += ret;
        return ret;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int ret = rof.read(b, off, len);
        if (ret != -1)
            total += ret;
        return ret;
    }

    /** @deprecated */
    public int skipBytes(int n) throws IOException {
        int ret = rof.skipBytes(n);
        total += ret;
        return ret;
    }
}
