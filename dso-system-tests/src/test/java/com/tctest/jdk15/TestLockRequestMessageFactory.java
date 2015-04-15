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
package com.tctest.jdk15;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.session.SessionID;

public class TestLockRequestMessageFactory implements LockRequestMessageFactory {

  @Override
  public LockRequestMessage newLockRequestMessage(final NodeID nodeId) {
    TestMessageChannel channel = new TestMessageChannel();
    channel.channelID = new ChannelID(100);
    return new LockRequestMessage(new SessionID(100), new NullMessageMonitor(), new TCByteBufferOutputStream(),
                                  channel, TCMessageType.LOCK_REQUEST_MESSAGE);
  }

}
