/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
