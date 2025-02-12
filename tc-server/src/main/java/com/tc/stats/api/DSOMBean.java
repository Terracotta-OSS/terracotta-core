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
package com.tc.stats.api;

import com.tc.management.TerracottaMBean;
import com.tc.stats.Client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

/**
 * This describes the management interface for the DSO subsystem. It's envisioned that this acts as a top-level object
 * aggregating statistical, configuration, and operational child interfaces.
 */

public interface DSOMBean extends TerracottaMBean {

  static final String CLIENT_ATTACHED = "dso.client.attached";
  static final String CLIENT_DETACHED = "dso.client.detached";

  ObjectName[] getClients();

  List<Client> getConnectedClients();

  Map<ObjectName, Exception> setAttribute(Set<ObjectName> onSet, String attrName, Object attrValue);

  Map<ObjectName, Exception> setAttribute(String attrName, Map<ObjectName, Object> attrMap);

  Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                       TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit, Object[] args,
                                 String[] sigs);

  int getCurrentClientCount();

  int getClientHighCount();

  String getJmxRemotePort();

  void setJmxRemotePort(String port);

  String startJMXRemote();

  String stopJMXRemote();
  
  int getCurrentBackoff();
  
  void setBackoffActive(boolean active);
  
  boolean isBackoffActive();
  
  boolean isCurrentlyDirect();
  
  boolean isDirectExecution();
  
  void setDirectExecution(boolean activate);
  
  long getMaxBackoffTime();
  
  long getBackoffCount();
  
  int getBufferCount();
  
  int getGroupBufferCount();
  
  void setAlwaysHydrate(boolean hydrate);
  
  boolean isAlwaysHydrate();
}
