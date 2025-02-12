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
package com.tc.objectserver.handshakemanager;

import com.tc.net.protocol.tcm.MessageChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import com.tc.text.PrettyPrintable;

public class ClientHandshakePrettyPrintable implements PrettyPrintable {

  private final MessageChannel[] channels;
  
  public ClientHandshakePrettyPrintable(MessageChannel[] message) {
    channels = message;
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> connections = new LinkedHashMap<>();
    Map<String, Object> map = new LinkedHashMap<>();
    if (channels != null) {
      for (MessageChannel c : channels) {
        if (c != null) {
          Object info = c.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT);
          if (info instanceof ClientHandshakeMonitoringInfo) {
            ClientHandshakeMonitoringInfo target = (ClientHandshakeMonitoringInfo)info;
            Map<String, Object> hs = new LinkedHashMap<>();
            hs.put("name", target.getName());
            hs.put("pid", target.getPid());
            hs.put("uuid", target.getUuid());
            hs.put("version", target.getVersion());
            hs.put("revision", target.getRevision());
            hs.put("clientReportedAddress", target.getClientReportedAddress());
            map.put(c.toString(), hs);
          }
        }
      }
    }
    connections.put("activeClientConnections", map);
    return connections;
  }

  
}
