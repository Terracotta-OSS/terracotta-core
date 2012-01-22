/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io;

import java.io.FileNotFoundException;

public interface TCRandomFileAccess {
  public TCFileChannel getChannel(TCFile tcFile, String mode) throws FileNotFoundException;
}
