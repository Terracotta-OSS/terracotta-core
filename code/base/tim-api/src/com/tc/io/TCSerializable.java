/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;


import java.io.IOException;

/**
 * @author teck
 */
public interface TCSerializable {
  public void serializeTo(TCByteBufferOutput serialOutput);

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException;
}