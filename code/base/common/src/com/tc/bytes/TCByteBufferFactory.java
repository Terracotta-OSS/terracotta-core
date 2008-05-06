/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bytes;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.util.Assert;

/**
 * TCByteBuffer source that hides JDK dependencies and that can pool instances. Instance pooling is likely to be a good
 * idea for fixed size buffers and definitely a good idea for java 1.4 direct buffers (since their
 * allocation/deallocation is more expensive than regular java objects).
 * 
 * @author teck
 */
public class TCByteBufferFactory {
  // 10485760 == 10MB
  private static final int                WARN_THRESHOLD          = 10485760;

  private static final int                poolMaxBufCount         = (TCPropertiesImpl.getProperties()
                                                                      .getInt(
                                                                              TCPropertiesConsts.TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT,
                                                                              2000));
  private static final int                commonPoolMaxBufCount   = (TCPropertiesImpl.getProperties()
                                                                      .getInt(
                                                                              TCPropertiesConsts.TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT,
                                                                              3000));

  // always use ThreadLocal variables for accessing the buffer pools.
  private static final BoundedLinkedQueue directCommonFreePool    = new BoundedLinkedQueue(commonPoolMaxBufCount);
  private static final BoundedLinkedQueue nonDirectCommonFreePool = new BoundedLinkedQueue(commonPoolMaxBufCount);

  private static final ThreadLocal        directFreePool          = new ThreadLocal() {
                                                                    protected Object initialValue() {
                                                                      if (TCThreadGroup.currentThreadInTCThreadGroup()) {
                                                                        return new BoundedLinkedQueue(poolMaxBufCount);
                                                                      } else {
                                                                        logger.debug("Buf pool direct for "
                                                                                     + Thread.currentThread().getName()
                                                                                     + " - using Common Pool");
                                                                        return directCommonFreePool;
                                                                      }
                                                                    }
                                                                  };
  private static final ThreadLocal        nonDirectFreePool       = new ThreadLocal() {
                                                                    protected Object initialValue() {
                                                                      if (TCThreadGroup.currentThreadInTCThreadGroup()) {
                                                                        return new BoundedLinkedQueue(poolMaxBufCount);
                                                                      } else {
                                                                        logger.debug("Buf pool nonDirect for "
                                                                                     + Thread.currentThread().getName()
                                                                                     + " - using Common Pool");
                                                                        return nonDirectCommonFreePool;
                                                                      }
                                                                    }
                                                                  };

  private static final int                DEFAULT_FIXED_SIZE      = 4096;
  private static final int                fixedBufferSize         = DEFAULT_FIXED_SIZE;

  private static final TCByteBuffer[]     EMPTY_BB_ARRAY          = new TCByteBuffer[0];

  private static final TCLogger           logger                  = TCLogging.getLogger(TCByteBufferFactory.class);

  private static final TCByteBuffer       ZERO_BYTE_BUFFER        = TCByteBufferImpl.wrap(new byte[0]);

  private static final boolean            disablePooling          = !(TCPropertiesImpl.getProperties()
                                                                      .getBoolean(TCPropertiesConsts.TC_BYTEBUFFER_POOLING_ENABLED));

  private static TCByteBuffer createNewInstance(boolean direct, int capacity, int index, int totalCount) {
    try {
      BoundedLinkedQueue poolQueue = (direct ? ((BoundedLinkedQueue) (directFreePool.get()))
          : ((BoundedLinkedQueue) (nonDirectFreePool.get())));
      TCByteBuffer rv = new TCByteBufferImpl(capacity, direct, poolQueue);
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
  public static TCByteBuffer getInstance(final boolean direct, int size) {

    if (size > WARN_THRESHOLD) {
      logger.warn("Asking for a large amount of memory: " + size + " bytes");
    }
    if (size < 0) { throw new IllegalArgumentException("Requested length cannot be less than zero"); }
    if (size == 0) { return ZERO_BYTE_BUFFER; }

    // Don't give 4k ByteBuffer from pool for smaller size requests.
    if (disablePooling || size < (fixedBufferSize - 500) || size > fixedBufferSize) {
      return createNewInstance(direct, size);
    } else {
      return getFromPoolOrCreate(direct);
    }
  }

  private static TCByteBuffer getFromPoolOrCreate(final boolean direct) {
    return getFromPoolOrCreate(direct, 0, 1);
  }

  private static TCByteBuffer getFromPoolOrCreate(boolean direct, int i, int numBuffers) {

    TCByteBuffer buffer = getFromPool(direct);
    if (null == buffer) {
      buffer = createNewInstance(direct, fixedBufferSize, i, numBuffers);
    }
    return buffer;
  }

  private static TCByteBuffer createNewInstance(boolean direct, int bufferSize) {
    return createNewInstance(direct, bufferSize, 0, 1);
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
  public static TCByteBuffer[] getFixedSizedInstancesForLength(final boolean direct, final int length) {
    if (length > WARN_THRESHOLD) {
      logger.warn("Asking for a large amount of memory: " + length + " bytes");
    }

    if (length < 0) { throw new IllegalArgumentException("Requested length cannot be less than zero"); }

    if (length == 0) { return EMPTY_BB_ARRAY; }

    int numBuffers = length / fixedBufferSize;
    if ((length % fixedBufferSize) != 0) {
      numBuffers++;
    }

    TCByteBuffer rv[] = new TCByteBuffer[numBuffers];

    if (disablePooling) {
      for (int i = 0; i < numBuffers; i++) {
        rv[i] = createNewInstance(direct, fixedBufferSize, i, numBuffers);
      }
    } else { // do pooling logic
      for (int i = 0; i < numBuffers; i++) {
        rv[i] = getFromPoolOrCreate(direct, i, numBuffers);
      }
    }

    // adjust limit of last buffer returned
    TCByteBuffer lastBuffer = rv[rv.length - 1];
    lastBuffer.limit(lastBuffer.capacity() - ((numBuffers * fixedBufferSize) - length));

    // ensureSpace(rv, length);

    return rv;
  }

  private static TCByteBuffer getFromPool(boolean direct) {
    if (disablePooling) return null;
    TCByteBuffer buf = null;

    BoundedLinkedQueue poolQueue = direct ? ((BoundedLinkedQueue) (directFreePool.get()))
        : ((BoundedLinkedQueue) (nonDirectFreePool.get()));

    Assert.assertNotNull(poolQueue);

    try {
      if ((buf = (TCByteBuffer) poolQueue.poll(0)) != null) {
        buf.checkedOut();
      }
    } catch (InterruptedException e) {
      logger.warn("interrupted while getting buffer from pool", e);
      return null;
    }
    return buf;
  }

  public static void returnBuffers(TCByteBuffer buffers[]) {
    if (disablePooling) { return; }

    for (int i = 0; i < buffers.length; i++) {
      TCByteBuffer buf = buffers[i];
      returnBuffer(buf);
    }
  }

  public static void returnBuffer(TCByteBuffer buf) {
    if (disablePooling) { return; }

    if (buf.capacity() == fixedBufferSize) {
      BoundedLinkedQueue bufferPool = buf.getBufferPool();
      buf.commit();

      if (bufferPool != null) {
        try {
          bufferPool.offer(buf, 0);
        } catch (InterruptedException e) {
          logger.warn("interrupted while trying to return buffer", e);
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

}