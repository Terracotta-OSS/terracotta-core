/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.process;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * An object that reads a stream asynchronously and collects it into a data buffer.
 */
public class StreamCollector extends StreamCopier {
  
  public StreamCollector(InputStream stream) {
    super(stream, new ByteArrayOutputStream());
  }
  
  public String toString() {
    return new String(((ByteArrayOutputStream) this.out).toByteArray());
  }

}
