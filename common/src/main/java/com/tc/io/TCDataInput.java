/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.DataInput;
import java.io.IOException;

/**
 * Input stream
 */
public interface TCDataInput extends DataInput {

  /**
   * Read string
   * @return String value
   */
  public String readString() throws IOException;

  /**
   * Read bytes into b starting at off for len. 
   * @param b The byte array to read into 
   * @param off The offset to start at in b
   * @param len The number of bytes to read
   * @return The number of bytes read
   * @throws IOException If there is an error reading the bytes
   */
  public int read(byte[] b, int off, int len) throws IOException;

}