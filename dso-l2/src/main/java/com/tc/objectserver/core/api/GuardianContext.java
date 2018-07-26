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
package com.tc.objectserver.core.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.ServerMessageChannel;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.util.Assert;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 *
 */
public class GuardianContext  implements DSOChannelManagerEventListener {
  private static final ConcurrentHashMap<ChannelID, MessageChannel> CONTEXT = new ConcurrentHashMap<>();
  private static final ThreadLocal<ChannelID>  CURRENTID = new ThreadLocal<>();  
  
  public static Properties createGuardContext(String callName) {
    Properties props = new Properties();
    props.setProperty("methodName", callName);
    Optional.ofNullable(CURRENTID.get()).ifPresent(cid->{
      props.setProperty("channelID", cid.toString());
      MessageChannel c = CONTEXT.get(cid);
      if (c != null) {
        props.setProperty(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT, String.valueOf(c.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT)));
        translateMaptoProperty(props, ServerMessageChannel.TRANSPORT_INFO, (Map<String, Object>)c.getAttachment(ServerMessageChannel.TRANSPORT_INFO));
      }
    });
    return props;
  }
  
  private static void translateMaptoProperty(Properties props, String root, Map<String, Object> map) {
    map.forEach((k, v) -> {
      if (v instanceof Map) {
        translateMaptoProperty(props, root + "." + k, (Map<String, Object>)v);
      } else {
        props.put(k, String.valueOf(v));
      }
    });
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    CONTEXT.put(channel.getChannelID(), channel);
  }

  @Override
  public void channelRemoved(MessageChannel channel, boolean wasActive) {
    CONTEXT.remove(channel.getChannelID());
  }
  
  public static boolean attach(String name, Object data) {
    MessageChannel channel = CONTEXT.get(CURRENTID.get());
    if (channel != null) {
      channel.addAttachment(name, data, true);
      return true;
    } else {
      return false;
    }
  }
  
  public static MessageChannel getCurrentMessageChannel() {
    return CONTEXT.get(CURRENTID.get());
  }

  public static Properties inquireContext(Function<MessageChannel, Properties> converter) {
    return converter.apply(CONTEXT.get(CURRENTID.get()));
  }
  
  public static void setCurrentChannelID(ChannelID cid) {
    CURRENTID.set(cid);
  }
  
  public static void clearCurrentChannelID(ChannelID cid) {
    ChannelID current = CURRENTID.get();
    Assert.assertEquals(cid, current);
    CURRENTID.remove();
  }
}
