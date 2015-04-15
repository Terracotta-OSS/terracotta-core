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
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;

public class TestWireProtocolMessage implements WireProtocolMessage {

  public TCConnection connection;
  public TCNetworkHeader header;
  
  @Override
  public short getMessageProtocol() {
    throw new ImplementMe();
  }

  @Override
  public WireProtocolHeader getWireProtocolHeader() {
    throw new ImplementMe();
  }

  @Override
  public TCNetworkHeader getHeader() {
    return header;
  }

  @Override
  public TCNetworkMessage getMessagePayload() {
    throw new ImplementMe();
  }

  @Override
  public TCByteBuffer[] getPayload() {
    throw new ImplementMe();
  }

  @Override
  public TCByteBuffer[] getEntireMessageData() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSealed() {
    throw new ImplementMe();
  }

  @Override
  public void seal() {
    throw new ImplementMe();

  }

  @Override
  public int getDataLength() {
    throw new ImplementMe();
  }

  @Override
  public int getHeaderLength() {
    throw new ImplementMe();
  }

  @Override
  public int getTotalLength() {
    throw new ImplementMe();
  }

  @Override
  public void wasSent() {
    throw new ImplementMe();

  }

  @Override
  public void setSentCallback(Runnable callback) {
    throw new ImplementMe();

  }

  @Override
  public Runnable getSentCallback() {
    throw new ImplementMe();
  }

  @Override
  public TCConnection getSource() {
    return connection;
  }

  @Override
  public void recycle() {
    return;
  }

}
