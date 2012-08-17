/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * SynchronizedOutputStream.java
 *
 * Created on 23. November 2006, 20:24
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

package org.terracotta.agent.repkg.de.schlichtherle.io.util;

import java.io.*;

/**
 * A decorator which synchronizes all access to an {@link OutputStream}
 * via an object provided to its constructor.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
public class SynchronizedOutputStream extends OutputStream {
    /**
     * The object to synchronize on - never <code>null</code>.
     */
    protected final Object lock;

    /**
     * The decorated output stream.
     */
    protected OutputStream out;

    /**
     * Constructs a new synchronized output stream.
     * This object will synchronize on itself.
     *
     * @param out The output stream to wrap in this decorator.
     */
    public SynchronizedOutputStream(final OutputStream out) {
  this(out, null);
    }

    /**
     * Constructs a new synchronized output stream.
     *
     * @param out The output stream to wrap in this decorator.
     * @param lock The object to synchronize on.
     *        If <code>null</code>, then this object is used, not the stream.
     */
    public SynchronizedOutputStream(final OutputStream out, final Object lock) {
  this.out = out;
  this.lock = lock != null ? lock : this;
    }

    public void write(int b) throws IOException {
  synchronized (lock) {
      out.write(b);
  }
    }

    public void write(byte[] b) throws IOException {
  synchronized (lock) {
      write(b, 0, b.length);
  }
    }

    public void write(byte[] b, int off, int len) throws IOException {
  synchronized (lock) {
      out.write(b, off, len);
  }
    }

    /** Synchronizes on the {@link #lock} and calls {@link #doFlush}. */
    public void flush() throws IOException {
  synchronized (lock) {
      doFlush();
  }
    }

    /** Flushes the underlying stream. This method is not synchronized! */
    protected void doFlush() throws IOException {
        out.flush();
    }

    /** Synchronizes on the {@link #lock} and calls {@link #doClose}. */
    public void close() throws IOException {
  synchronized (lock) {
            doClose();
  }
    }

    /**
     * Calls {@link #doFlush} and finally closes the underlying stream.
     * This method is not synchronized!
     */
    protected void doClose() throws IOException {
        try {
            doFlush();
        } finally {
            out.close();
        }
    }
}
