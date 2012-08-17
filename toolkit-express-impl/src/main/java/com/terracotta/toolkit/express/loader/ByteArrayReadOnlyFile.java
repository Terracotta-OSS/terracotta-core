/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express.loader;

import org.terracotta.agent.repkg.de.schlichtherle.io.rof.AbstractReadOnlyFile;

class ByteArrayReadOnlyFile extends AbstractReadOnlyFile {
  private final byte[] data;
  private int          index = 0;

  ByteArrayReadOnlyFile(byte[] data) {
    this.data = data;
  }

  public void close() {
    //
  }

  public long getFilePointer() {
    return index;
  }

  public long length() {
    return data.length;
  }

  public int read() {
    if (index >= data.length) { return -1; }
    return data[index++];
  }

  public int read(byte[] b, int off, int len) {
    if (index >= data.length) { return -1; }
    len = Math.min(data.length - index, len);
    System.arraycopy(data, index, b, off, len);
    index += len;
    return len;
  }

  public void seek(long pos) {
    index = (int) pos;
  }

}
