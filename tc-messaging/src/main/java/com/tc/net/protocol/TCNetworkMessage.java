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
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

/**
 * @author teck
 */
public interface TCNetworkMessage extends Recyclable {

  public TCNetworkHeader getHeader();

  public TCNetworkMessage getMessagePayload();

  public TCByteBuffer[] getPayload();

  public TCByteBuffer[] getEntireMessageData();

  public boolean isSealed();

  public void seal();

  public int getDataLength();

  public int getHeaderLength();

  public int getTotalLength();

  public void wasSent();

  public void setSentCallback(Runnable callback);
  
  public Runnable getSentCallback();
}
