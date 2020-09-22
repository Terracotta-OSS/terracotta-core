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
package com.tc.objectserver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.management.AbstractTerracottaMBean;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

import static com.tc.management.beans.L2MBeanNames.TOPOLOGY_MBEAN;
import static java.lang.String.join;

public class TopologyManager {

  private Topology topology;
  private volatile TopologyMbean topologyMbean;
  private final List<TopologyListener> listeners = new ArrayList<>();

  public TopologyManager(Set<String> hostPorts) {
    this.topology = new Topology(hostPorts);
    initializeMbean();
  }

  public synchronized Topology getTopology() {
    return topology;
  }

  private void initializeMbean() {
    if (topologyMbean != null) return;
    try {
      this.topologyMbean = new TopologyMbeanImpl(this);
    } catch (NotCompliantMBeanException e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized boolean addPassive(String host, int port, int group) {
    final Topology old = this.topology;
    Set<String> newServers = new HashSet<>(old.getServers());
    if (newServers.add(host + ":" + port)) {
      this.topology = new Topology(newServers);
      for (TopologyListener listener : listeners) {
        listener.nodeAdded(host,port,group);
      }
      return true;
    }

    return false;
  }

  private synchronized boolean removePassive(String host, int port, int group) {
    final Topology old = this.topology;
    Set<String> newServers = new HashSet<>(old.getServers());
    if (newServers.remove(host + ":" + port)) {
      this.topology = new Topology(newServers);
      for (TopologyListener listener : listeners) {
        listener.nodeRemoved(host,port,group);
      }
      return true;
    }

    return false;
  }

  public synchronized void addListener(TopologyListener listener) {
    this.listeners.add(listener);
  }

  public interface TopologyMbean {
    boolean addPassive(String host, int port, int group);

    boolean addPassive(String hostPortGroup);

    boolean removePassive(String hostPort, int port, int group);

    boolean removePassive(String hostPortGroup);

    String getTopology();
  }

  private static class TopologyMbeanImpl extends AbstractTerracottaMBean implements TopologyMbean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyMbeanImpl.class);
    private final TopologyManager topologyManager;

    TopologyMbeanImpl(TopologyManager topologyManager) throws NotCompliantMBeanException {
      super(TopologyMbean.class, false);
      this.topologyManager = topologyManager;

      try {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, TOPOLOGY_MBEAN);
      } catch (Exception e) {
        LOGGER.warn("Problem registering MBean with name " + TOPOLOGY_MBEAN.getCanonicalName(), e);
      }
    }

    public boolean addPassive(String host, int port, int group) {
      return this.topologyManager.addPassive(host,port,group);
    }

    public boolean addPassive(String hostPortGroup) {
      String[] split = hostPortGroup.split("\\:");
      return this.topologyManager.addPassive(split[0],Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }
    
    public boolean removePassive(String host, int port, int group) {
      return this.topologyManager.removePassive(host,port,group);
    }

    public boolean removePassive(String hostPortGroup) {
      String[] split = hostPortGroup.split("\\:");
      return this.topologyManager.removePassive(split[0],Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    @Override
    public String getTopology() {
      return join(",", this.topologyManager.getTopology().getServers());
    }

    @Override
    public void reset() {
      // no-op
    }
  }
}
