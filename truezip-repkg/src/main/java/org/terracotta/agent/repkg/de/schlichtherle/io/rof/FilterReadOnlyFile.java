/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * FilterReadOnlyFile.java
 *
 * Created on 14. Oktober 2005, 20:57
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
 * A base class for any class which wants to decorate a {@link ReadOnlyFile}.
 * <p>
 * Note that subclasses of this class often implement their own virtual file
 * pointer.
 * Thus, if you would like to access the underlying <code>ReadOnlyFile</code>
 * again after you have finished working with the
 * <code>FilteredReadOnlyFile</code>, you should synchronize their file
 * pointers a'la:
 * <pre>
 *     ReadOnlyFile rof = new SimpleReadOnlyFile(new File("HelloWorld.java"));
 *     try {
 *         ReadOnlyFile frof = new FilteredReadOnlyFile(rof);
 *         try {
 *             // Do any file input on brof here...
 *             frof.seek(1);
 *         } finally {
 *             // Synchronize the file pointers.
 *             rof.seek(frof.getFilePointer());
 *         }
 *         // This assertion would fail if we hadn't done the file pointer
 *         // synchronization!
 *         assert rof.getFilePointer() == 1;
 *     } finally {
 *         rof.close();
 *     }
 * </pre>
 * This does not apply to this base class, however.
 * <p>
 * Subclasses implemententing their own virtual file pointer should add a note
 * referring to this classes Javadoc like this:
 * <blockquote>
 * <b>Note:</b> This class implements its own virtual file pointer.
 * Thus, if you would like to access the underlying <code>ReadOnlyFile</code>
 * again after you have finished working with an instance of this class,
 * you should synchronize their file pointers using the pattern as described
 * in {@link FilterReadOnlyFile}.
 * </blockquote>
 *
 * @author Christian Schlichtherle
 * @version @version@
 */
public class FilterReadOnlyFile extends AbstractReadOnlyFile {

    /** The read only file to be filtered. */
    protected ReadOnlyFile rof;
    
    /**
     * Creates a new instance of <tt>FilterReadOnlyFile</tt>,
     * which filters the given read only file.
     */
    public FilterReadOnlyFile(ReadOnlyFile rof) {
        this.rof = rof;
    }

    public long length() throws IOException {
        return rof.length();
    }

    public long getFilePointer() throws IOException {
        return rof.getFilePointer();
    }

    public void seek(long pos) throws IOException {
        rof.seek(pos);
    }

    public int read() throws IOException {
        return rof.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return rof.read(b, off, len);
    }

    public void close() throws IOException {
        rof.close();
    }
}
