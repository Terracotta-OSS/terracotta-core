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
package com.tc.management.remote.protocol.terracotta;

import com.tc.management.JMXAttributeContext;
import com.tc.net.protocol.tcm.MessageChannel;

import java.io.IOException;

import javax.management.remote.message.Message;

public class TunnelingMessageConnectionWrapper extends TunnelingMessageConnection {
  private final RemoteJMXAttributeProcessor jmAttributeProcessor = new RemoteJMXAttributeProcessor();

  public TunnelingMessageConnectionWrapper(MessageChannel channel, boolean isJmxConnectionServer) {
    super(channel, isJmxConnectionServer);
  }

  @Override
  public void writeMessage(Message outboundMessage) throws IOException {
    if (closed.isSet()) { throw new IOException("connection closed"); }
    
    JMXAttributeContext attributeContext = new JMXAttributeContext(this.channel, outboundMessage);
    jmAttributeProcessor.add(attributeContext);
  }

  @Override
  public void close() {
    super.close();
    jmAttributeProcessor.close();
  }
}
