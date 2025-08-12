/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.core.impl;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.ServerMessageChannel;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.server.TCServerImpl;
import com.tc.spi.Guardian;
import com.tc.util.Assert;

/**
 *
 */
public class GuardianContext {
  private static final ConcurrentHashMap<ChannelID, MessageChannel> CONTEXT = new ConcurrentHashMap<>();
  private static final ThreadLocal<ChannelID>  CURRENTID = new ThreadLocal<>();

  private static TCServerImpl server;

  public static void setServer(TCServerImpl server) {
    GuardianContext.server = server;
  }
  
  private static Properties createGuardContext(String callName) {
    return createGuardContext(callName, new Properties());
  }

  private static Properties createGuardContext(String callName, Properties existing) {
    ChannelID cid = CURRENTID.get();
    if (cid != null) {
      Properties props =  createGuardContext(callName, existing, CONTEXT.get(cid));
      props.setProperty("clientID", Long.toString(cid.toLong()));
      return props;
    } else {
      Properties props = new Properties();
      if (callName != null) {
        props.setProperty("id", callName);
      }
      return props;
    }
  }
  
  private static Properties createGuardContext(String callName, Properties props, MessageChannel c) {
    if (callName != null) {
      props.setProperty("id", callName);
    }
    if (c != null) {
      ClientHandshakeMonitoringInfo info = (ClientHandshakeMonitoringInfo)c.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT);
      props.setProperty(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT, String.valueOf(info));
      String handshakeAddress = "";
      if (info != null) {
        translateMaptoProperty(props, "handshake", info.getStateMap());
        handshakeAddress = info.getClientReportedAddress();
      }
      props.setProperty("product", c.getProductID().name());
      MessageTransport transport = (MessageTransport)c.getAttachment(ServerMessageChannel.TRANSPORT_INFO);
      String remoteAddress = "";
      if (transport != null) {
        translateMaptoProperty(props, ServerMessageChannel.TRANSPORT_INFO, transport.getStateMap());
        remoteAddress = TCSocketAddress.getStringForm(transport.getRemoteAddress());
      }
      props.setProperty("natDetected", Boolean.toString(!handshakeAddress.equals(remoteAddress)));
    }
    return props;
  }
  
  private static void translateMaptoProperty(Properties props, String root, Map<String, ?> map) {
    map.forEach((k, v) -> {
      if (v instanceof Map) {
        translateMaptoProperty(props, root + "." + k, (Map<String, Object>)v);
      } else {
        props.put(root + "." + k, String.valueOf(v));
      }
    });
  }

  public static void channelCreated(MessageChannel channel) {
    CONTEXT.put(channel.getChannelID(), channel);
  }

  public static void channelRemoved(MessageChannel channel) {
    CONTEXT.remove(channel.getChannelID());
  }
  
  public static void clientRemoved(ClientID removed) {
    CONTEXT.remove(removed.getChannelID());
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
  
  public static void setCurrentChannelID(ChannelID cid) {
    CURRENTID.set(cid);
  }
  
  public static void clearCurrentChannelID(ChannelID cid) {
    ChannelID current = CURRENTID.get();
    Assert.assertEquals(cid, current);
    CURRENTID.remove();
  }
  
  public static Properties getCurrentChannelProperties() {
    return createGuardContext("context");
  }

  public static MessageChannel getCurrentMessageChannel() {
    ChannelID current = CURRENTID.get();
    return CONTEXT.get(current);
  }
  
  private static Guardian getOperationGuardian() {
    if (server != null) {
      DistributedObjectServer dso = server.getDSOServer();
      if (dso != null) {
        ServerManagementContext cxt = dso.getManagementContext();
        if (cxt != null) {
          return cxt.getOperationGuardian();
        }
      }
    }
    return (o, p)->true;
  }
  
  public static boolean validate(Guardian.Op op, String id) {
    return getOperationGuardian().validate(op, createGuardContext(id));
  }
  
  public static boolean validate(Guardian.Op op, String id, MessageChannel channel) {
    return validate(op, id, new Properties(), channel);
  }

  public static boolean validate(Guardian.Op op, String id, Properties props, MessageChannel channel) {
    return getOperationGuardian().validate(op, createGuardContext(id, props, channel));
  }

  public static boolean validate(Guardian.Op op, String id, Properties additional) {
    return getOperationGuardian().validate(op, createGuardContext(id, additional));
  }
}
