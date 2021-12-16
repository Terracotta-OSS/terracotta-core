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
package com.tc.bytes;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.nio.ReadOnlyBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCByteBuffer source that hides JDK dependencies and that can pool instances. Instance pooling is likely to be a good
 * idea for fixed size buffers and definitely a good idea for java direct buffers (since their allocation/deallocation
 * is more expensive than regular java objects).
 * 
 * @author teck
 */
public class TCByteBufferFactory {
  public static int                  FIXED_BUFFER_SIZE       = 4 * 1024;                                                    // 4KiB
  private static final int                 WARN_THRESHOLD          = 10 * 1024 * 1024;                                                // 10MiB
  private static final TCByteBuffer[]      EMPTY_BB_ARRAY          = new TCByteBuffer[0];
  private static final TCByteBuffer        ZERO_BYTE_BUFFER        = TCByteBufferImpl.wrap(new byte[0]);
  private static final Logger logger = LoggerFactory.getLogger(TCByteBufferFactory.class);
  private static final ReferenceQueue<TCByteBuffer> DIRECT_POOL = new ReferenceQueue<>();
  /**
   * Get a single variable sized TCByteBuffer instance Note: These are not pooled (yet)
   * 
   * @param size The desired minimum capacity of the buffer. The actual capacity may be higher. The buffer's limit will
   *        be equal to it's capacity.
   */
  public static TCByteBuffer getInstance(int size) {

    if (size > WARN_THRESHOLD) {
      logger.warn("Asking for a large amount of memory: " + size + " bytes");
    }
    if (size < 0) { throw new IllegalArgumentException("Requested length cannot be less than zero"); }
    if (size == 0) { return ZERO_BYTE_BUFFER; }

    // Don't give 4k ByteBuffer from pool for smaller size requests.
    return new TCByteBufferImpl(size, false);
  }

  public static void setFixedBufferSize(int size) {
    FIXED_BUFFER_SIZE = size;
  }
  
  /**
   * Get enough fixed sized TCByteBuffer instances to contain the given number of bytes
   * 
   * @param length
   * @return an array of TCByteBuffer instances. The limit of the last buffer may be less then it's capacity to adjust
   *         for the given length
   */
  public static TCByteBuffer[] getDirectBuffersForLength(int length) {
    if (length > WARN_THRESHOLD) {
      logger.warn("Asking for a large amount of memory: " + length + " bytes");
    }

    if (length < 0) { throw new IllegalArgumentException("Requested length cannot be less than zero"); }

    if (length == 0) { return EMPTY_BB_ARRAY; }

    int numBuffers = getBufferCountNeededForMessageSize(length);
    TCByteBuffer rv[] = new TCByteBuffer[numBuffers];
    for (int i = 0; i < numBuffers; i++) {
      rv[i] = pullFromQueueOrCreate(i, numBuffers);
    }

    // adjust limit of last buffer returned
    TCByteBuffer lastBuffer = rv[rv.length - 1];
    lastBuffer.limit(lastBuffer.capacity() - ((numBuffers * FIXED_BUFFER_SIZE) - length));

    // ensureSpace(rv, length);

    return rv;
  }

  public static TCByteBuffer getDirectByteBuffer() {
    return pullFromQueueOrCreate(0, 1);
  }

  private static TCByteBuffer pullFromQueueOrCreate(int index, int totalCount) {
    TCByteBuffer usable = null;

    while (usable == null) {
      Reference<? extends TCByteBuffer> ref = DIRECT_POOL.poll();
      if (ref != null) {
        usable = ref.get();
        if (usable != null) {
          if (usable.capacity() == FIXED_BUFFER_SIZE) {
            usable = usable.reInit();
          } else {
            usable = null;
          }
        }
      } else {
        try {
          usable = new TCByteBufferImpl(FIXED_BUFFER_SIZE, true);
        } catch (OutOfMemoryError oome) {
          // try to log some useful context. Most OOMEs don't have stack traces unfortunately
          logger.error("OOME trying to allocate direct buffer of size " + FIXED_BUFFER_SIZE
                       + " (index " + index + " of count " + totalCount + ")");
          throw oome;
        }
      }
    }
    SoftReference root = new SoftReference(usable, DIRECT_POOL);
    return usable;
  }

  private static int getBufferCountNeededForMessageSize(int length) {
    int numBuffers = length / FIXED_BUFFER_SIZE;
    if ((length % FIXED_BUFFER_SIZE) != 0) {
      numBuffers++;
    }
    return numBuffers;
  }

  public static int getTotalBufferSizeNeededForMessageSize(int length) {
    return (getBufferCountNeededForMessageSize(length) * FIXED_BUFFER_SIZE);
  }

  public static TCByteBuffer wrap(byte[] buf) {
    if (buf == null) {
      return null;
    }
    return TCByteBufferImpl.wrap(buf);
  }
  
  public static byte[] unwrap(TCByteBuffer buffer) {
    if (buffer == null) {
      return null;
    }
    byte[] array = null;
    try {
      array = buffer.array();
    } catch (ReadOnlyBufferException ro) {
      // ignore
    }
    if (array == null || buffer.position() != 0 || buffer.remaining() != array.length || buffer.arrayOffset() != 0) {
      array = new byte[buffer.remaining()];
      buffer.duplicate().get(array);
    }
    return array;
  }

  public static TCByteBuffer copyAndWrap(byte[] buf) {
    TCByteBuffer rv = null;
    if (buf != null) {
      rv = getInstance(buf.length);
      rv.put(buf).rewind();
    } else {
      rv = getInstance(0);
    }
    return rv;
  }
}
