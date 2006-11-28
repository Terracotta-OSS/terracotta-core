/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;


import java.io.IOException;

/**
 * @author teck
 */
public interface TCSerializable {
  public void serializeTo(TCByteBufferOutput serialOutput);

  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException;
}