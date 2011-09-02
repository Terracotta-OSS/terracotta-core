/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;

public class NullProtocolAdaptor implements TCProtocolAdaptor {
  private final TCLogger logger = TCLogging.getLogger(this.getClass());

  public NullProtocolAdaptor() {
    super();
  }

  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) {
    logger.warn("Null Protocol Adaptor isn't suppose to receive any data from the network.");
    return;
  }

  public TCByteBuffer[] getReadBuffers() {
    return TCByteBufferFactory.getFixedSizedInstancesForLength(false, 4096);
  }
}