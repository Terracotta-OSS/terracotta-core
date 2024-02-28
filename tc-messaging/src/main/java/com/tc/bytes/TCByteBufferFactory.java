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

import com.tc.util.Assert;
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
  private static int                  FIXED_BUFFER_SIZE       = 4 * 1024;                                                    // 4KiB
  private static final int                 WARN_THRESHOLD          = 10 * 1024 * 1024;                                                // 10MiB
  private static final TCByteBuffer        ZERO_BYTE_BUFFER        = TCByteBufferImpl.wrap(new byte[0]);
  private static final Logger logger = LoggerFactory.getLogger(TCByteBufferFactory.class);
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
    TCByteBufferImpl buf = new TCByteBufferImpl(size, false);
    Assert.assertTrue(buf.position() == 0);
    return buf;
  }

  public static void setFixedBufferSize(int size) {
    if (size > 512 && size < 32 * 1024 *1024) {
      FIXED_BUFFER_SIZE = size;
    } else {
      logger.warn("fixed buffer size ignored, outside sane range {}", size);
    }
  }

  public static int getFixedBufferSize() {
    return FIXED_BUFFER_SIZE;
  }

  public static TCByteBuffer getDirectByteBuffer() {
    return new TCByteBufferImpl(FIXED_BUFFER_SIZE, true);
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

    if (buffer.isReadOnly() || buffer.isDirect() || buffer.position() != 0 || buffer.remaining() != buffer.array().length || buffer.arrayOffset() != 0) {
      byte[] array = new byte[buffer.remaining()];
      TCByteBuffer b = buffer.slice();
      while (b.hasRemaining()) {
        b.get(array, b.position(), b.remaining());
      }
      return array;
    } else {
      return buffer.array();
    }
  }

  public static TCByteBuffer copyAndWrap(byte[] buf) {
    TCByteBuffer rv;
    if (buf != null) {
      rv = getInstance(buf.length);
      rv.put(buf).rewind();
    } else {
      rv = getInstance(0);
    }
    return rv;
  }
}
