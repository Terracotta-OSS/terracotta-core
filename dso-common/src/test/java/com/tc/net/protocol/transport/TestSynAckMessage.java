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

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.NetworkLayer;

public class TestSynAckMessage extends TestTransportHandshakeMessage implements SynAckMessage {

  @Override
  public String getErrorContext() {
    throw new ImplementMe();
  }

  @Override
  public short getErrorType() {
    throw new ImplementMe();
  }

  @Override
  public boolean hasErrorContext() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSyn() {
    return false;
  }

  @Override
  public boolean isSynAck() {
    return true;
  }

  @Override
  public boolean isAck() {
    return false;
  }

  @Override
  public void recycle() {
    return;
  }

  @Override
  public short getStackLayerFlags() {
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  @Override
  public int getCallbackPort() {
    throw new ImplementMe();
  }

}
