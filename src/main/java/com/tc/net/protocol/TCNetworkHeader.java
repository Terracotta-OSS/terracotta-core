/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

/**
 * Generic network header interface
 * 
 * @author teck
 */
public interface TCNetworkHeader extends Recyclable {
  int getHeaderByteLength();

  TCByteBuffer getDataBuffer();

  void validate() throws TCProtocolException;

}
