/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;

/**
 * @author teck
 */
public class NullNetworkHeader implements TCNetworkHeader {

  public NullNetworkHeader() {
    super();
  }

  @Override
  public int getHeaderByteLength() {
    return 0;
  }

  @Override
  public TCByteBuffer getDataBuffer() {
    return TCByteBufferFactory.getInstance(false, 0);
  }

  @Override
  public void validate() {
    return;
  }

  public void dispose() {
    return;
  }

  @Override
  public void recycle() {
    return;
  }

}