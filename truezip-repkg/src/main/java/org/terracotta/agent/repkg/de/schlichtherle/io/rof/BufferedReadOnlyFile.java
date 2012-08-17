/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * BufferedReadOnlyFile.java
 *
 * Created on 5. Oktober 2005, 17:22
 */
/*
 * Copyright 2006-2007 Schlichtherle IT Services
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
 * A {@link ReadOnlyFile} implementation which provides buffered random read
 * only access to another <code>ReadOnlyFile</code>.
 * <p>
 * <b>Note:</b> This class implements its own virtual file pointer.
 * Thus, if you would like to access the underlying <code>ReadOnlyFile</code>
 * again after you have finished working with an instance of this class,
 * you should synchronize their file pointers using the pattern as described
 * in {@link FilterReadOnlyFile}.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 */
public class BufferedReadOnlyFile extends FilterReadOnlyFile {

    /**
     * The default buffer length of the window to the file.
     */
    public static final int WINDOW_LEN = 4096;

    /** Returns the smaller parameter. */
    protected static final long min(long a, long b) {
        return a < b ? a : b;
    }

    /** Returns the greater parameter. */
    protected static final long max(long a, long b) {
        return a < b ? b : a;
    }

    private long length;

    /**
     * The virtual file pointer in the file data.
     * This is relative to the start of the file.
     */
    private long fp;

    /**
     * The current offset in the read only file where the buffer window starts.
     * This is always a multiple of the buffer window size.
     */
    private long windowOff;

    /**
     * The buffer window to the file data.
     */
    private final byte[] window;

    private boolean closed;

    /**
     * Creates a new instance of <tt>BufferedReadOnlyFile</tt>.
     * 
     * @param file The file to read.
     * @throws NullPointerException If any of the parameters is <tt>null</tt>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    public BufferedReadOnlyFile(
            final File file)
    throws  NullPointerException,
            FileNotFoundException,
            IOException {
        this(null, file, WINDOW_LEN);
    }

    /**
     * Creates a new instance of <tt>BufferedReadOnlyFile</tt>.
     * 
     * @param file The file to read.
     * @param windowLen The size of the buffer window in bytes.
     * @throws NullPointerException If any of the parameters is <tt>null</tt>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    public BufferedReadOnlyFile(
            final File file,
            final int windowLen)
    throws  NullPointerException,
            FileNotFoundException,
            IOException {
        this(null, file, windowLen);
    }

    /**
     * Creates a new instance of <tt>BufferedReadOnlyFile</tt>.
     * 
     * @param rof The read only file to read.
     * @throws NullPointerException If any of the parameters is <tt>null</tt>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    public BufferedReadOnlyFile(
            final ReadOnlyFile rof)
    throws  NullPointerException,
            FileNotFoundException,
            IOException {
        this(rof, null, WINDOW_LEN);
    }

    /**
     * Creates a new instance of <tt>BufferedReadOnlyFile</tt>.
     * 
     * @param rof The read only file to read.
     * @param windowLen The size of the buffer window in bytes.
     * @throws NullPointerException If any of the parameters is <tt>null</tt>.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    public BufferedReadOnlyFile(
            final ReadOnlyFile rof,
            final int windowLen)
    throws  NullPointerException,
            FileNotFoundException,
            IOException {
        this(rof, null, windowLen);
    }

    private BufferedReadOnlyFile(
            ReadOnlyFile rof,
            final File file,
            final int windowLen)
    throws  NullPointerException,
            FileNotFoundException,
            IOException {
        super(rof);

        // Check parameters (fail fast).
        if (rof == null) {
            if (file == null)
                throw new NullPointerException();
            rof = createReadOnlyFile(file);
        } else { // rof != null
            assert file == null;
        }
        if (windowLen <= 0)
            throw new IllegalArgumentException();

        super.rof = rof;
        length = rof.length();
        fp = rof.getFilePointer();
        window = new byte[windowLen];
        invalidateWindow();
        
        assert window.length > 0;
    }

    /**
     * A factory method called by the constructor to get a read only file
     * to access the contents of the read only file.
     * This method is only used if the constructor isn't called with a read
     * only file as its parameter.
     * 
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    protected ReadOnlyFile createReadOnlyFile(File file)
    throws IOException {
        return new SimpleReadOnlyFile(file);
        //return new FastReadOnlyFile(file);
        //return new ChannelReadOnlyFile(file);
    }

    public long length()
    throws IOException {
        final long newLength = rof.length();
        if (newLength != length) {
            length = newLength;
            invalidateWindow();
        }

        return length;
    }

    public long getFilePointer()
    throws IOException {
        ensureOpen();
        return fp;
    }

    public void seek(final long fp)
    throws IOException {
        ensureOpen();

        if (fp < 0)
            throw new IOException("file pointer must not be negative");
        final long length = length();
        if (fp > length)
            throw new IOException("file pointer (" + fp
                    + ") is larger than file length (" + length + ")");

        this.fp = fp;
    }

    public int read()
    throws IOException {
        // Check state.
        ensureOpen();
        if (fp >= length())
            return -1;

        // Position window and return its data.
        positionWindow();
        return window[(int) (fp++ % window.length)] & 0xff;
    }

    public int read(final byte[] buf, final int off, final int len)
    throws IOException {
        if (len == 0)
            return 0; // be fault-tolerant and compatible to RandomAccessFile

        // Check state.
        ensureOpen();
        final long length = length();
        if (fp >= length)
            return -1;

        // Check parameters.
        if (buf == null)
            throw new NullPointerException("buf");
        final int offPlusLen = off + len;
        if ((off | len | offPlusLen | buf.length - offPlusLen) < 0)
      throw new IndexOutOfBoundsException();

        // Setup.
        final int windowLen = window.length;
        int read = 0; // amount of decrypted data copied to buf
        
        {
            // Partial read of window data at the start.
            final int o = (int) (fp % windowLen);
            if (o != 0) {
                // The file pointer is not on a window boundary.
                positionWindow();
                read = (int) min(len, windowLen - o);
                read = (int) min(read, length - fp);
                System.arraycopy(window, o, buf, off, read);
                fp += read;
            }
        }

        {
            // Full read of window data in the center.
            while (read + windowLen < len && fp + windowLen <= length) {
                // The file pointer is starting and ending on window boundaries.
                positionWindow();
                System.arraycopy(window, 0, buf, off + read, windowLen);
                read += windowLen;
                fp += windowLen;
            }
        }
        
        // Partial read of window data at the end.
        if (read < len && fp < length) {
            // The file pointer is not on a window boundary.
            positionWindow();
            final int n = (int) min(len - read, length - fp);
            System.arraycopy(window, 0, buf, off + read, n);
            read += n;
            fp += n;
        }
        
        // Assert that at least one byte has been read if len isn't zero.
        // Note that EOF has been tested before.
        assert read > 0;
        return read;
    }

    /**
     * Closes this read only file.
     * As a side effect, this will set the reference to the underlying read
     * only file ({@link #rof} to <tt>null</tt>.
     */
    public void close()
    throws IOException {
        if (closed)
            return;

        // Order is important here!
        closed = true;
        rof.close();
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

    //
    // Buffer window operations.
    //

    /**
     * Ensures that the window is positioned so that the block containing
     * the current virtual file pointer in the encrypted file is entirely
     * contained in it.
     * 
     * @throws IOException On any I/O related issue.
     *         The window is invalidated in this case.
     */
    private void positionWindow()
    throws IOException {
        // Check window position.
        final long fp = this.fp;
        final int windowLen = window.length;
        final long nextWindowOff = windowOff + windowLen;
        if (windowOff <= fp && fp < nextWindowOff)
            return;
    
        try {
            // Move window in the buffered file.
            windowOff = (fp / windowLen) * windowLen; // round down to multiple of window size
            if (windowOff != nextWindowOff)
                rof.seek(windowOff);

            // Fill window until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of ReadOnlyFile's
            // contract.
            int n = 0;
            do {
                int read = rof.read(window, n, windowLen - n);
                if (read < 0)
                    break;
                n += read;
            } while (n < windowLen);
        } catch (IOException ioe) {
            windowOff = -windowLen - 1; // force seek() at next positionWindow()
            throw ioe;
        }
    }

    /**
     * Forces the window to be reloaded on the next call to
     * {@link #positionWindow()}.
     */
    private final void invalidateWindow() {
        windowOff = Long.MIN_VALUE;
    }
}
