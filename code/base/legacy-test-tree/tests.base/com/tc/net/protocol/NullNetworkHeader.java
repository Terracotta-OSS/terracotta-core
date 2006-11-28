package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class NullNetworkHeader implements TCNetworkHeader {

  public NullNetworkHeader() {
    super();
  }

  public int getHeaderByteLength() {
    return 0;
  }

  public TCByteBuffer getDataBuffer() {
    return TCByteBufferFactory.getInstance(false, 0);
  }

  public void validate() {
    return;
  }

  public void dispose() {
    return;
  }

  public void recycle() {
    return;
  }

}