/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

package org.terracotta.agent.repkg.de.schlichtherle.util.zip;

import java.io.*;
import java.util.*;

/**
 * Drop-in replacement for
 * {@link java.util.zip.ZipOutputStream java.util.zip.ZipOutputStream}.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @see ZipFile
 */
public class ZipOutputStream extends BasicZipOutputStream {
    
    /**
     * Creates a new ZIP output stream decorating the given output stream,
     * using the UTF-8 charset.
     *
     * @throws NullPointerException If <code>out</code> is <code>null</code>.
     */
    public ZipOutputStream(
            final OutputStream out)
    throws NullPointerException {
        super(out);
    }

    /**
     * Creates a new ZIP output stream decorating the given output stream.
     *
     * @throws NullPointerException If <code>out</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     */
    public ZipOutputStream(
            final OutputStream out,
            final String charset)
    throws  NullPointerException,
            UnsupportedEncodingException {
        super(out, charset);
    }

    public synchronized int size() {
        return super.size();
    }

    /**
     * Returns a safe enumeration of clones of the ZIP entries written so far.
     * This method takes a snapshot of the collection of all entries and
     * enumerates their clones, so concurrent modifications or state changes
     * do not affect this instance, the returned enumeration or the
     * enumerated ZIP entries.
     */
    public synchronized Enumeration entries() {
  return new Enumeration() {
      Enumeration e = Collections.enumeration(Collections.list(
                    ZipOutputStream.super.entries()));

      public boolean hasMoreElements() {
    return e.hasMoreElements();
      }

      public Object nextElement() {
    return ((ZipEntry) e.nextElement()).clone();
      }
        };
    }

    /**
     * Returns a clone of the {@link ZipEntry} for the given name or
     * <code>null</code> if no entry with that name exists.
     *
     * @param name Name of the ZIP entry.
     */
    public synchronized ZipEntry getEntry(String name) {
        ZipEntry entry = super.getEntry(name);
        return entry != null ? (ZipEntry) entry.clone() : null;
    }

    public synchronized void setComment(String comment) {
        super.setComment(comment);
    }

    public synchronized String getComment() {
        return super.getComment();
    }
    
    public synchronized void setLevel(int level) {
  super.setLevel(level);
    }

    public synchronized int getLevel() {
        return super.getLevel();
    }

    public synchronized int getMethod() {
        return super.getMethod();
    }

    public synchronized void setMethod(int method) {
        super.setMethod(method);
    }

    public synchronized long length() {
        return super.length();
    }

    public synchronized final boolean isBusy() {
        return super.isBusy();
    }

    public synchronized void putNextEntry(
            final ZipEntry entry,
            final boolean deflate)
    throws IOException {
        super.putNextEntry(entry, deflate);
    }

    public synchronized void write(int b)
    throws IOException {
        super.write(b);
    }

    public synchronized void write(final byte[] b, final int off, final int len)
    throws IOException {
        super.write(b, off, len);
    }

    public synchronized void closeEntry() throws IOException {
        super.closeEntry();
    }

    public synchronized void finish() throws IOException {
        super.finish();
    }

    public synchronized void close() throws IOException {
        super.close();
    }
}
