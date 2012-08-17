/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * FastReadOnlyFile.java
 *
 * Created on 14. Oktober 2005, 20:36
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

/**
 * A {@link ReadOnlyFile} implementation using a {@link RandomAccessFile} with
 * a cached file pointer.
 * This implementation skips unnecessary file pointer operations for the
 * operating system.
 *
 * @deprecated Depending on the implementation of the J2SE API, this class
 *             most likely does not provide any performance improvement.
 *             Use where necessary only!
 * @author Christian Schlichtherle
 * @version @version@
 */
public class FastReadOnlyFile
        extends RandomAccessFile
        implements ReadOnlyFile
{
    /** The current file pointer in the file. */
    private long fp;

    private boolean closed;
    
    public FastReadOnlyFile(File file)
    throws FileNotFoundException {
        super(file, "r");
    }

    public long getFilePointer() throws IOException {
        ensureOpen();
        return fp;
    }

    public void seek(final long pos) throws IOException {
        if (pos == fp) {
            ensureOpen();
            return;
        }
        super.seek(pos);
        fp = pos;
    }

    public int read() throws IOException {
        final int ret = super.read();
        if (ret != -1)
            fp++;
        return ret;
    }

    public int read(final byte[] b, final int off, final int len)
    throws IOException {
        final int ret = super.read(b, off, len);
        if (ret != -1)
            fp += ret;
        return ret;
    }
    
    public void close() throws IOException {
        super.close();
        closed = true;
    }

    /**
     * Ensures that this file is open.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private final void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("file is closed");
    }
}
