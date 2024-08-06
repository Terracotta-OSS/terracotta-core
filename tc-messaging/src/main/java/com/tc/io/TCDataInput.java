/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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