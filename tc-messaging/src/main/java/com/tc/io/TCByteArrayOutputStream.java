/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.io;

import java.io.OutputStream;

/**
 * No-synch, reset()'able byte array stream, with public access to underlying
 * byte[]
 *
 * @author teck
 */
public final class TCByteArrayOutputStream extends OutputStream {

    private int size;

    private byte[] buffer;

    public TCByteArrayOutputStream() {
        this(64);
    }

    public TCByteArrayOutputStream(int initialSize) {
        buffer = new byte[initialSize];
    }

    private void ensureCapacity(int newCap) {
        byte newBuffer[] = new byte[Math.max(buffer.length * 2, newCap)];
        System.arraycopy(buffer, 0, newBuffer, 0, size);
        buffer = newBuffer;
    }

    @Override
    public final void write(int b) {
        int newSize = size + 1;
        if (newSize > buffer.length)
            ensureCapacity(newSize);
        buffer[size] = (byte) b;
        size = newSize;
    }

    @Override
    public final void write(byte b[], int offset, int len) {
        int newSize = size + len;
        if (newSize > buffer.length)
            ensureCapacity(newSize);
        System.arraycopy(b, offset, buffer, size, len);
        size = newSize;
    }

    public final void reset() {
        size = 0;
    }

    public final byte[] getInternalArray() {
        return buffer;
    }

    public final int size() {
        return size;
    }

}
