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
package com.tc.objectserver.core.impl;

import java.io.Serializable;
import java.util.Collection;
import org.terracotta.monitoring.IMonitoringProducer;

/**
 *
 */
public class MonitoringProducerMultiplexor implements IMonitoringProducer {
  private final Collection<IMonitoringProducer> producers;

  public MonitoringProducerMultiplexor(Collection<IMonitoringProducer> producers) {
    this.producers = producers;
  }

  @Override
  public boolean addNode(String[] root, String name, Serializable object) {
    boolean result = true;
    for (IMonitoringProducer p : producers) {
      result = p.addNode(root, name, object);
    }
    return result;
  }

  @Override
  public boolean removeNode(String[] root, String name) {
    boolean result = true;
    for (IMonitoringProducer p : producers) {
      result = p.removeNode(root, name);
    }
    return result;
  }

  @Override
  public void pushBestEffortsData(String root, Serializable object) {
    for (IMonitoringProducer p : producers) {
      p.pushBestEffortsData(root, object);
    }
  }

}
