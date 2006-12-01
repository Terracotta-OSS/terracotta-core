/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import com.tc.util.Assert;

/**
 * Holds TC specific (but not JDK specific) features on byte buffers
 * 
 * @author teck
 */
abstract class AbstractTCByteBuffer implements TCByteBuffer {
  
  public final TCByteBuffer get(int index, byte[] dst) {
    return get(index, dst, 0, dst.length);
  }

  public final TCByteBuffer get(int index, byte[] dst, int offset, int length) {
    final int origPosition = position();

    try {
      position(index);
      get(dst, offset, length);
    } finally {
      position(origPosition);
    }

    return this;
  }

  public final TCByteBuffer put(int index, byte[] src) {
    return put(index, src, 0, src.length);
  }

  public final TCByteBuffer put(int index, byte[] src, int offset, int length) {
    final int origPosition = position();

    try {
      position(index);
      put(src, offset, length);
    } finally {
      position(origPosition);
    }

    return this;
  }

  public final TCByteBuffer putUint(long i) {
    if ((i > 0xFFFFFFFFL) || (i < 0L)) {
      // make code formatter sane
      throw new IllegalArgumentException("Unsigned integer value must be positive and <= (2^32)-1");
    }

    put((byte) ((i >> 24) & 0x000000FF));
    put((byte) ((i >> 16) & 0x000000FF));
    put((byte) ((i >> 8) & 0x000000FF));
    put((byte) (i & 0x000000FF));

    return this;
  }

  public final TCByteBuffer putUint(int index, long i) {
    final int origPosition = position();

    try {
      position(index);
      putUint(i);
    } finally {
      position(origPosition);
    }

    return this;
  }

  public final TCByteBuffer putUshort(int s) {
    if ((s > 0x0000FFFF) || (s < 0)) { throw new IllegalArgumentException(
                                                                          "Unsigned integer value must be positive and <= (2^16)-1"); }

    put((byte) ((s >> 8) & 0x000000FF));
    put((byte) (s & 0x000000FF));

    return this;
  }

  public final TCByteBuffer putUshort(int index, int s) {
    final int origPosition = position();

    try {
      position(index);
      putUshort(s);
    } finally {
      position(origPosition);
    }

    return this;
  }

  public final long getUint() {
    long rv = 0;

    rv += ((long) (get() & 0xFF) << 24);
    rv += ((get() & 0xFF) << 16);
    rv += ((get() & 0xFF) << 8);
    rv += ((get() & 0xFF));

    return rv;
  }

  public final long getUint(int index) {
    final int origPosition = position();

    try {
      position(index);
      return getUint();
    } finally {
      position(origPosition);
    }
  }

  public final int getUshort() {
    int rv = 0;

    rv += ((get() & 0xFF) << 8);
    rv += ((get() & 0xFF));

    Assert.eval((rv >= 0) && (rv <= 0xFFFF));

    return rv;
  }

  public final int getUshort(int index) {
    final int origPosition = position();

    try {
      position(index);
      return getUshort();
    } finally {
      position(origPosition);
    }
  }

  public final short getUbyte() {
    return (short) (get() & 0xFF);
  }

  public final short getUbyte(int index) {
    final int origPosition = position();

    try {
      position(index);
      return getUbyte();
    } finally {
      position(origPosition);
    }
  }

  public final TCByteBuffer putUbyte(int index, short value) {
    final int origPosition = position();

    try {
      position(index);
      putUbyte(value);
    } finally {
      position(origPosition);
    }

    return this;
  }

  public final TCByteBuffer putUbyte(short value) {
    if ((value < 0) || (value > 0xFF)) { throw new IllegalArgumentException(
                                                                            "Unsigned byte value must in range 0-255 inclusive"); }
    put((byte) (value & 0xFF));
    return this;
  }
}