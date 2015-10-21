/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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