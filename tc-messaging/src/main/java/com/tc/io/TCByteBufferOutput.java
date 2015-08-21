/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

public interface TCByteBufferOutput extends TCDataOutput, Recyclable {

  public TCByteBuffer[] toArray();

  public void write(TCByteBuffer[] data);

}
