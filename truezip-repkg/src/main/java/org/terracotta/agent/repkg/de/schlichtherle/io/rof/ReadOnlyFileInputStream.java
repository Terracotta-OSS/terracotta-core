/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ReadOnlyFileInputStream.java
 *
 * Created on 12. Dezember 2005, 17:23
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

package org.terracotta.agent.repkg.de.schlichtherle.io.rof;

import java.io.*;
import java.util.logging.*;

/**
 * An adapter class turning a provided {@link ReadOnlyFile} into
 * an {@link InputStream}.
 * Note that this stream supports marking.
 * Note that any of the methods in this class throw a
 * {@link NullPointerException} if {@link #rof} hasn't been initialized.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4 Support for marking.
 */
public class ReadOnlyFileInputStream extends InputStream {

    /**
     * The underlying {@link ReadOnlyFile}.
     * Any of the methods in this class throw a {@link NullPointerException}
     * if this hasn't been initialized.
     */
    protected ReadOnlyFile rof;

    /**
     * The position of the last mark.
     * Initialized to <code>-1</code> to indicate that no mark has been set.
     */
    private long mark = -1;

    /**
     * Adapts the given <code>ReadOnlyFile</code>.
     *
     * @param rof The underlying <code>ReadOnlyFile</code>. May be
     *        <code>null</code>, but must be initialized before any method
     *        of this class can be used.
     */
    public ReadOnlyFileInputStream(ReadOnlyFile rof) {
        this.rof = rof;
    }

    public int read() throws IOException {
        return rof.read();
    }

    public int read(byte[] b) throws IOException {
        return rof.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return rof.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        if (n <= 0)
            return 0; // for compatibility to RandomAccessFile

        final long fp = rof.getFilePointer(); // should fail when closed
        final long len = rof.length(); // may succeed when closed
        final long rem = len - fp;
        if (n > rem)
            n = (int) rem;
        rof.seek(fp + n);
        return n;
    }

    public int available() throws IOException {
        final long rem = rof.length() - rof.getFilePointer();
        return rem > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
    }

    public void close() throws IOException {
        rof.close();
    }

    public void mark(int readlimit) {
        try {
            mark = rof.getFilePointer();
        } catch (IOException failure) {
            Logger.getLogger(ReadOnlyFileInputStream.class.getName())
                .log(Level.WARNING, "mark()/reset() not supported", failure);
            mark = -2;
        }
    }

    public void reset() throws IOException {
        if (mark < 0)
            throw new IOException(mark == -1
                    ? "no mark set"
                    : "mark()/reset() not supported by underlying file");
        rof.seek(mark);
    }

    public boolean markSupported() {
        try {
            rof.seek(rof.getFilePointer());
            return true;
        } catch (IOException failure) {
            return false;
        }
    }
}
