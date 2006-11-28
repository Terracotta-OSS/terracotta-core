/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

public interface TCByteBufferOutput extends TCDataOutput, Recyclable {

  public TCByteBuffer[] toArray();

  public void write(TCByteBuffer[] data);

}
