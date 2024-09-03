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
