/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * SynchronizedInputStream.java
 *
 * Created on 23. November 2006, 20:21
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
 * A decorator which synchronizes all access to an {@link InputStream}
 * via an object provided to its constructor.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
public class SynchronizedInputStream extends InputStream {
    /**
     * The object to synchronize on - never <code>null</code>.
     */
    protected final Object lock;
    
    /**
     * The decorated input stream.
     */
    protected InputStream in;

    /**
     * Constructs a new synchronized input stream.
     * This object will synchronize on itself.
     *
     * @param in The input stream to wrap in this decorator.
     */
    public SynchronizedInputStream(final InputStream in) {
  this(in, null);
    }

    /**
     * Constructs a new synchronized input stream.
     *
     * @param in The input stream to wrap in this decorator.
     * @param lock The object to synchronize on.
     *        If <code>null</code>, then this object is used, not the stream.
     */
    public SynchronizedInputStream(final InputStream in, final Object lock) {
  this.in = in;
  this.lock = lock != null ? lock : this;
    }

    public int read() throws IOException {
  synchronized (lock) {
      return in.read();
  }
    }

    public int read(byte[] b) throws IOException {
  synchronized (lock) {
      return read(b, 0, b.length);
  }
    }

    public int read(byte[] b, int off, int len) throws IOException {
  synchronized (lock) {
      return in.read(b, off, len);
  }
    }

    public long skip(long n) throws IOException {
  synchronized (lock) {
      return in.skip(n);
  }
    }

    public int available() throws IOException {
  synchronized (lock) {
      return in.available();
  }
    }

    /** Synchronizes on the {@link #lock} and calls {@link #doClose}. */
    public void close() throws IOException {
  synchronized (lock) {
      doClose();
  }
    }

    /** Closes the underlying stream. This method is not synchronized! */
    protected void doClose() throws IOException {
        in.close();
    }

    public void mark(int readlimit) {
  synchronized (lock) {
      in.mark(readlimit);
  }
    }

    public void reset() throws IOException {
  synchronized (lock) {
      in.reset();
  }
    }

    public boolean markSupported() {
  synchronized (lock) {
      return in.markSupported();
  }
    }
}
