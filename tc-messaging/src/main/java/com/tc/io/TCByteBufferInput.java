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

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCReference;

public interface TCByteBufferInput extends TCDataInput {

  /**
   * Duplicate this stream. The resulting stream will share data with the source stream (ie. no copying), but the two
   * streams will have independent read positions. The read position of the result stream will initially be the same as
   * the source stream
   */
  public TCByteBufferInput duplicate();

  public int getTotalLength();

  public int available();

  public void close();

  public int read(byte[] b);
  
  public TCReference readReference(int len);
  
  public TCByteBuffer read(int len);

  public int read();

  public long skip(long skip);

}