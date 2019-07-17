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
package com.tc.objectserver.handshakemanager;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.text.PrettyPrintable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientHandshakePrettyPrintable implements PrettyPrintable {

  private final MessageChannel[] channels;
  
  public ClientHandshakePrettyPrintable(MessageChannel[] message) {
    channels = message;
  }

  @Override
  public Map<String, ?> getStateMap() {
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
            map.put(c.toString(), hs);
          }
        }
      }
    }
    return map;
  }

  
}
