/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.FileNotFoundException;

public interface TCRandomFileAccess {
  public TCFileChannel getChannel(TCFile tcFile, String mode) throws FileNotFoundException;
}
