/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class TCRandomFileAccessImpl implements TCRandomFileAccess {
  private RandomAccessFile randomAccessFile;
  
  public TCRandomFileAccessImpl() {
    randomAccessFile = null;
  }

  public TCFileChannel getChannel(TCFile tcFile, String mode) throws FileNotFoundException {
    randomAccessFile = new RandomAccessFile(tcFile.getFile(), mode);
    return new TCFileChannelImpl(randomAccessFile.getChannel());
  }
}
