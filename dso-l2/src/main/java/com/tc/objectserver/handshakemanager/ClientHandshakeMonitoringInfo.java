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

/**
 *
 */
public class ClientHandshakeMonitoringInfo {
  public static String MONITORING_INFO_ATTACHMENT = "client_monitoring_info_attachment";
  
  private final int pid;
  private final String uuid;
  private final String name;

  public ClientHandshakeMonitoringInfo(int pid, String uuid, String name) {
    this.pid = pid;
    this.uuid = uuid;
    this.name = name;
  }

  public int getPid() {
    return pid;
  }

  public String getUuid() {
    return uuid;
  }

  public String getName() {
    return name;
  }
}
