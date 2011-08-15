/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.commons.SerialVersionUIDAdder;


/**
 * <code>SerialVersionUIDAdder</code> not using JCE API. Using SHA1Digest
 * implementation from the Bouncy Castle framework.
 * 
 * @see SerialVersionUIDAdder
 * @see http://www.bouncycastle.org/
 */
public class SafeSerialVersionUIDAdder extends SerialVersionUIDAdder {

    public SafeSerialVersionUIDAdder(ClassVisitor cv) {
        super(cv);
    }

    protected byte[] computeSHAdigest(final byte[] value) {
        return update(value, 0, value.length);
    }

    /*
     * implementation of SHA-1 as outlined in "Handbook of Applied
     * Cryptography", pages 346 - 349.
     * 
     * Copyright (c) 2000 - 2006 The Legion Of The Bouncy Castle
     * (http://www.bouncycastle.org)
     * 
     * Permission is hereby granted, free of charge, to any person obtaining a
     * copy of this software and associated documentation files (the
     * "Software"), to deal in the Software without restriction, including
     * without limitation the rights to use, copy, modify, merge, publish,
     * distribute, sublicense, and/or sell copies of the Software, and to permit
     * persons to whom the Software is furnished to do so, subject to the
     * following conditions:
     * 
     * The above copyright notice and this permission notice shall be included
     * in all copies or substantial portions of the Software.
     * 
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
     * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
     * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
     * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
     * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
     * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
     * USE OR OTHER DEALINGS IN THE SOFTWARE.
     */

    private int H1 = 0x67452301;
    private int H2 = 0xefcdab89;
    private int H3 = 0x98badcfe;
    private int H4 = 0x10325476;
    private int H5 = 0xc3d2e1f0;

    private int[] X = new int[80];
    private int xOff = 0;

    private byte[] xBuf = new byte[4];
    private int xBufOff = 0;

    private long byteCount = 0;

    private byte[] update(byte[] in, int inOff, int len) {
        // fill the current word
        while ((xBufOff != 0) && (len > 0)) {
            update(in[inOff]);

            inOff++;
            len--;
        }

        // process whole words.
        while (len > xBuf.length) {
            processWord(in, inOff);

            inOff += xBuf.length;
            len -= xBuf.length;
            byteCount += xBuf.length;
        }

        // load in the remainder.
        while (len > 0) {
            update(in[inOff]);

            inOff++;
            len--;
        }

        long bitLength = (byteCount << 3);

        // add the pad bytes.
        update((byte) 128);

        while (xBufOff != 0) {
            update((byte) 0);
        }

        processLength(bitLength);

        processBlock();

        byte[] out = new byte[20];
        unpackWord(H1, out, 0);
        unpackWord(H2, out, 4);
        unpackWord(H3, out, 8);
        unpackWord(H4, out, 12);
        unpackWord(H5, out, 16);
        return out;
    }

    private void update(byte in) {
        xBuf[xBufOff++] = in;

        if (xBufOff == xBuf.length) {
            processWord(xBuf, 0);
            xBufOff = 0;
        }

        byteCount++;
    }

    private void processWord(byte[] in, int inOff) {
        X[xOff++] = (in[inOff] & 0xff) << 24 | (in[inOff + 1] & 0xff) << 16
                | (in[inOff + 2] & 0xff) << 8 | in[inOff + 3] & 0xff;

        if (xOff == 16) {
            processBlock();
        }
    }

    private void unpackWord(int word, byte[] out, int outOff) {
        out[outOff++] = (byte) (word >>> 24);
        out[outOff++] = (byte) (word >>> 16);
        out[outOff++] = (byte) (word >>> 8);
        out[outOff++] = (byte) word;
    }

    protected void processLength(long bitLength) {
        if (xOff > 14) {
            processBlock();
        }

        X[14] = (int) (bitLength >>> 32);
        X[15] = (int) (bitLength & 0xffffffff);
    }

    private static final int Y1 = 0x5a827999;
    private static final int Y2 = 0x6ed9eba1;
    private static final int Y3 = 0x8f1bbcdc;
    private static final int Y4 = 0xca62c1d6;

    private int f(int u, int v, int w) {
        return ((u & v) | ((~u) & w));
    }

    private int h(int u, int v, int w) {
        return (u ^ v ^ w);
    }

    private int g(int u, int v, int w) {
        return ((u & v) | (u & w) | (v & w));
    }

    protected void processBlock() {
        // expand 16 word block into 80 word block.
        for (int i = 16; i < 80; i++) {
            int t = X[i - 3] ^ X[i - 8] ^ X[i - 14] ^ X[i - 16];
            X[i] = t << 1 | t >>> 31;
        }

        // set up working variables.
        int A = H1;
        int B = H2;
        int C = H3;
        int D = H4;
        int E = H5;

        // round 1
        int idx = 0;

        for (int j = 0; j < 4; j++) {
            // E = rotateLeft(A, 5) + f(B, C, D) + E + X[idx++] + Y1
            // B = rotateLeft(B, 30)
            E += (A << 5 | A >>> 27) + f(B, C, D) + X[idx++] + Y1;
            B = B << 30 | B >>> 2;

            D += (E << 5 | E >>> 27) + f(A, B, C) + X[idx++] + Y1;
            A = A << 30 | A >>> 2;

            C += (D << 5 | D >>> 27) + f(E, A, B) + X[idx++] + Y1;
            E = E << 30 | E >>> 2;

            B += (C << 5 | C >>> 27) + f(D, E, A) + X[idx++] + Y1;
            D = D << 30 | D >>> 2;

            A += (B << 5 | B >>> 27) + f(C, D, E) + X[idx++] + Y1;
            C = C << 30 | C >>> 2;
        }

        // round 2
        for (int j = 0; j < 4; j++) {
            // E = rotateLeft(A, 5) + h(B, C, D) + E + X[idx++] + Y2
            // B = rotateLeft(B, 30)
            E += (A << 5 | A >>> 27) + h(B, C, D) + X[idx++] + Y2;
            B = B << 30 | B >>> 2;

            D += (E << 5 | E >>> 27) + h(A, B, C) + X[idx++] + Y2;
            A = A << 30 | A >>> 2;

            C += (D << 5 | D >>> 27) + h(E, A, B) + X[idx++] + Y2;
            E = E << 30 | E >>> 2;

            B += (C << 5 | C >>> 27) + h(D, E, A) + X[idx++] + Y2;
            D = D << 30 | D >>> 2;

            A += (B << 5 | B >>> 27) + h(C, D, E) + X[idx++] + Y2;
            C = C << 30 | C >>> 2;
        }

        // round 3
        for (int j = 0; j < 4; j++) {
            // E = rotateLeft(A, 5) + g(B, C, D) + E + X[idx++] + Y3
            // B = rotateLeft(B, 30)
            E += (A << 5 | A >>> 27) + g(B, C, D) + X[idx++] + Y3;
            B = B << 30 | B >>> 2;

            D += (E << 5 | E >>> 27) + g(A, B, C) + X[idx++] + Y3;
            A = A << 30 | A >>> 2;

            C += (D << 5 | D >>> 27) + g(E, A, B) + X[idx++] + Y3;
            E = E << 30 | E >>> 2;

            B += (C << 5 | C >>> 27) + g(D, E, A) + X[idx++] + Y3;
            D = D << 30 | D >>> 2;

            A += (B << 5 | B >>> 27) + g(C, D, E) + X[idx++] + Y3;
            C = C << 30 | C >>> 2;
        }

        // round 4
        for (int j = 0; j <= 3; j++) {
            // E = rotateLeft(A, 5) + h(B, C, D) + E + X[idx++] + Y4
            // B = rotateLeft(B, 30)
            E += (A << 5 | A >>> 27) + h(B, C, D) + X[idx++] + Y4;
            B = B << 30 | B >>> 2;

            D += (E << 5 | E >>> 27) + h(A, B, C) + X[idx++] + Y4;
            A = A << 30 | A >>> 2;

            C += (D << 5 | D >>> 27) + h(E, A, B) + X[idx++] + Y4;
            E = E << 30 | E >>> 2;

            B += (C << 5 | C >>> 27) + h(D, E, A) + X[idx++] + Y4;
            D = D << 30 | D >>> 2;

            A += (B << 5 | B >>> 27) + h(C, D, E) + X[idx++] + Y4;
            C = C << 30 | C >>> 2;
        }

        H1 += A;
        H2 += B;
        H3 += C;
        H4 += D;
        H5 += E;

        // reset start of the buffer.
        xOff = 0;
        for (int i = 0; i < 16; i++) {
            X[i] = 0;
        }
    }

}
