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
package com.tc.net.protocol.tcm;

import java.util.Date;

public class ChannelEventImpl implements ChannelEvent {

  private final ChannelEventType type;
  private final MessageChannel   channel;
  private final Date             timestamp;

  public ChannelEventImpl(ChannelEventType type, MessageChannel channel) {
    this.type = type;
    this.channel = channel;
    this.timestamp = new Date();
  }

  @Override
  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[type = " + this.type + ", timestamp = "
           + timestamp + ", channel  = " + channel + " remote node  : " + channel.getRemoteNodeID();

  }

  @Override
  public MessageChannel getChannel() {
    return channel;
  }

  @Override
  public ChannelID getChannelID() {
    return getChannel().getChannelID();
  }

  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  @Override
  public ChannelEventType getType() {
    return type;
  }
}
