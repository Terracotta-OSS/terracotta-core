/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.text.PrettyPrintable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class ClientHandshakeMonitoringInfo implements PrettyPrintable {
  public static String MONITORING_INFO_ATTACHMENT = "client_monitoring_info_attachment";
  
  private final int pid;
  private final String uuid;
  private final String name;
  private final String version;
  private final String revision;
  private final String address;

  public ClientHandshakeMonitoringInfo(int pid, String uuid, String name, String version, String revision, String address) {
    this.pid = pid;
    this.uuid = uuid;
    this.name = name;
    this.version = version;
    this.revision = revision;
    this.address = address;
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

  public String getVersion() { return version; }

  public String getRevision() { return revision; }
  
  public String getClientReportedAddress() {
    return this.address;
  }
  
  public boolean hasClientReportedAddress() {
    return (this.address != null && this.address.length() > 0);
  }
  
  public boolean hasClientVersion() {
    return (this.version != null && this.version.length() > 0);
  }

  public boolean hasClientRevision() {
    return (this.revision != null && this.revision.length() > 0);
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("uuid", uuid);
    map.put("pid", Integer.toString(pid));
    map.put("name", name);
    map.put("version", version);
    map.put("revision", revision);
    map.put("address", address);
    return map;
  }

  @Override
  public String toString() {
    return "ClientHandshakeInfo{" + "pid=" + pid + ", uuid=" + uuid + ", name=" + name + ", version=" + version + ", revision=" + revision + ", address=" + address + '}';
  }
}
