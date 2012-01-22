/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
