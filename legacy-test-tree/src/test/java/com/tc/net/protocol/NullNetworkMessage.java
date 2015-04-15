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

/**
 * @author teck
 */
public class NullNetworkMessage implements TCNetworkMessage {

  public NullNetworkMessage() {
    super();
  }

  @Override
  public TCNetworkHeader getHeader() {
    return new NullNetworkHeader();
  }

  @Override
  public TCNetworkMessage getMessagePayload() {
    return null;
  }

  @Override
  public TCByteBuffer[] getPayload() {
    return getEntireMessageData();
  }

  @Override
  public TCByteBuffer[] getEntireMessageData() {
    return new TCByteBuffer[] {};
  }

  @Override
  public boolean isSealed() {
    return true;
  }

  @Override
  public void seal() {
    return;
  }

  @Override
  public int getDataLength() {
    return 0;
  }

  @Override
  public int getHeaderLength() {
    return 0;
  }

  @Override
  public int getTotalLength() {
    return 0;
  }

  @Override
  public void wasSent() {
    return;
  }

  @Override
  public void setSentCallback(Runnable callback) {
    return;
  }

  @Override
  public Runnable getSentCallback() {
    return null;
  }

  @Override
  public void recycle() {
    return;
  }

}