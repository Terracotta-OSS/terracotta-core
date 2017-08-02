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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * TCByteBuffer source that hides JDK dependencies and that can pool instances. Instance pooling is likely to be a good
 * idea for fixed size buffers and definitely a good idea for java direct buffers (since their allocation/deallocation
 * is more expensive than regular java objects).
 * 
 * @author teck
 */
public class TCByteBufferFactory {

  public static final int                  FIXED_BUFFER_SIZE       = 4 * 1024;                                                        // 4KiB
  private static final int                 WARN_THRESHOLD          = 10 * 1024 * 1024;                                                // 10MiB
  private static final TCByteBuffer[]      EMPTY_BB_ARRAY          = new TCByteBuffer[0];
  private static final TCByteBuffer        ZERO_BYTE_BUFFER        = TCByteBufferImpl.wrap(new byte[0]);
  private static final Logger logger = LoggerFactory.getLogger(TCByteBufferFactory.class);
  
  private static final Set<ThreadGroup> handledGroups = Collections.newSetFromMap(new WeakHashMap<ThreadGroup, Boolean>());
  
  private static TCByteBuffer createNewInstance(boolean direct, int capacity, int index, int totalCount) {
    try {
      TCByteBuffer rv = new TCByteBufferImpl(capacity, direct, null);
      // Assert.assertEquals(0, rv.position());
      // Assert.assertEquals(capacity, rv.capacity());
      // Assert.assertEquals(capacity, rv.limit());
      return rv;
    } catch (OutOfMemoryError oome) {
      // try to log some useful context. Most OOMEs don't have stack traces unfortunately
      logger.error("OOME trying to allocate " + (direct ? "direct" : "non-direct") + " buffer of size " + capacity
                   + " (index " + index + " of count " + totalCount + ")");
      throw oome;
    }
  }
  /**
   * Get a single variable sized TCByteBuffer instance Note: These are not pooled (yet)
   * 
   * @param size The desired minimum capacity of the buffer. The actual capacity may be higher. The buffer's limit will
   *        be equal to it's capacity.
   * @param direct True to hint that the buffer should be a direct buffer (ie. not on the Java heap). A direct buffer
   *        will never be returned if this parameter is false. A direct buffer may or MAY NOT returned if the parameter
   *        is true TODO :: Make this the only interface and make it return fixed size buffer also make sure only
   *        fixedBufferSize gets to the pool back.
   */
  public static TCByteBuffer getInstance(boolean direct, int size) {

    if (size > WARN_THRESHOLD) {
      logger.warn("Asking for a large amount of memory: " + size + " bytes");
    }
    if (size < 0) { throw new IllegalArgumentException("Requested length cannot be less than zero"); }
    if (size == 0) { return ZERO_BYTE_BUFFER; }

    // Don't give 4k ByteBuffer from pool for smaller size requests.
    return createNewInstance(direct, size);
  }

  private static TCByteBuffer createNewInstance(boolean direct, int bufferSize) {
    return createNewInstance(direct, bufferSize, 0, 1);
  }

  public static void registerThreadGroup(ThreadGroup group) {
    handledGroups.add(group);
  }
  
  /**
   * Get enough fixed sized TCByteBuffer instances to contain the given number of bytes
   * 
   * @param direct True to hint that the buffers should be direct buffers (ie. not on the Java heap). Direct buffers
   *        will never be returned if this parameter is false. Direct buffers may or MAY NOT returned if the parameter
   *        is true. The returned buffers may be a mix of direct and non-direct
   * @param length
   * @return an array of TCByteBuffer instances. The limit of the last buffer may be less then it's capacity to adjust
   *         for the given length
   */
  public static TCByteBuffer[] getFixedSizedInstancesForLength(boolean direct, int length) {
    if (length > WARN_THRESHOLD) {
      logger.warn("Asking for a large amount of memory: " + length + " bytes");
    }

    if (length < 0) { throw new IllegalArgumentException("Requested length cannot be less than zero"); }

    if (length == 0) { return EMPTY_BB_ARRAY; }

    int numBuffers = getBufferCountNeededForMessageSize(length);
    TCByteBuffer rv[] = new TCByteBuffer[numBuffers];

    for (int i = 0; i < numBuffers; i++) {
      rv[i] = createNewInstance(direct, FIXED_BUFFER_SIZE, i, numBuffers);
    }

    // adjust limit of last buffer returned
    TCByteBuffer lastBuffer = rv[rv.length - 1];
    lastBuffer.limit(lastBuffer.capacity() - ((numBuffers * FIXED_BUFFER_SIZE) - length));

    // ensureSpace(rv, length);

    return rv;
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

  public static void returnBuffers(TCByteBuffer buffers[]) {
    for (TCByteBuffer buf : buffers) {
      returnBuffer(buf);
    }
  }

  public static void returnBuffer(TCByteBuffer buf) {
    if (buf.capacity() == FIXED_BUFFER_SIZE) {
      BufferPool bufferPool = buf.getBufferPool();
      buf.commit();

      if (bufferPool != null) {
        try {
          bufferPool.offer(buf);
        } catch (InterruptedException e) {
          logger.warn("interrupted while trying to return buffer", e);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public static TCByteBuffer wrap(byte[] buf) {
    return TCByteBufferImpl.wrap(buf);
  }

  public static TCByteBuffer copyAndWrap(byte[] buf) {
    TCByteBuffer rv = null;
    if (buf != null) {
      rv = getInstance(false, buf.length);
      rv.put(buf).rewind();
    } else {
      rv = getInstance(false, 0);
    }
    return rv;
  }

  public static boolean isPoolingEnabled() {
    return false;
  }
}
