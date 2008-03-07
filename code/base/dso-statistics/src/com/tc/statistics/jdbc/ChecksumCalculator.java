/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.jdbc;

import com.tc.exception.TCRuntimeException;

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ChecksumCalculator {
  private final StringBuffer buffer = new StringBuffer();

  public void append(final String string) {
    buffer.append(string);
  }

  public long checksum() {
    try {
      byte[] bytes = buffer.toString().getBytes("UTF-8");
      Checksum checksumEngine = new CRC32();
      checksumEngine.update(bytes, 0, bytes.length);
      return checksumEngine.getValue();
    } catch (UnsupportedEncodingException e) {
      // should never happen
      throw new TCRuntimeException(e);
    }
  }
}
