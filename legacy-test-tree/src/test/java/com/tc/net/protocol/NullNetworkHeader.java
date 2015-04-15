/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;

/**
 * @author teck
 */
public class NullNetworkHeader implements TCNetworkHeader {

  public NullNetworkHeader() {
    super();
  }

  @Override
  public int getHeaderByteLength() {
    return 0;
  }

  @Override
  public TCByteBuffer getDataBuffer() {
    return TCByteBufferFactory.getInstance(false, 0);
  }

  @Override
  public void validate() {
    return;
  }

  public void dispose() {
    return;
  }

  @Override
  public void recycle() {
    return;
  }

}