/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * AbstractReadOnlyFile.java
 *
 * Created on 5. Februar 2007, 23:36
 */
/*
 * Copyright 2007 Schlichtherle IT Services
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
 * A base class for <code>ReadOnlyFile</code> implementations which
 * implements the common boilerplate.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
public abstract class AbstractReadOnlyFile implements ReadOnlyFile {

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(final byte[] buf, final int off, final int len)
    throws IOException {
        int total = 0;
  do {
      final int read = read(buf, off + total, len - total);
      if (read < 0)
    throw new EOFException();
      total += read;
  } while (total < len);
    }

    /** @deprecated Use {@link #seek} instead. */
    public int skipBytes(int n) throws IOException {
        if (n <= 0)
            return 0; // for compatibility to RandomAccessFile

        final long fp = getFilePointer(); // should fail when closed
        final long len = length(); // may succeed when closed
        final long rem = len - fp;
        if (n > rem)
            n = (int) rem;
        seek(fp + n);
        assert getFilePointer() == fp + n;
        return n;
    }
}
