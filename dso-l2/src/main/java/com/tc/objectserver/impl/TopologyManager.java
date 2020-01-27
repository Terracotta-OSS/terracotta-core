package com.tc.objectserver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import static java.lang.String.join;

public class TopologyManager {

  private Topology topology;
  private volatile TopologyMbean topologyMbean;
  private final List<TopologyListener> listeners = new ArrayList<>();

  private TopologyManager() {
  }

  private static final TopologyManager INSTANCE = new TopologyManager();

  public static TopologyManager get() {
    return INSTANCE;
  }

  public synchronized Topology getTopology() {
    return topology;
  }

  public synchronized void initialize(Set<String> hostPorts) {
    this.topology = new Topology(hostPorts);
    initializeMbean();
  }

  private void initializeMbean() {
    if (topologyMbean != null) return;
    try {
      this.topologyMbean = new TopologyMbeanImpl(this);
    } catch (NotCompliantMBeanException e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized boolean addPassive(String hostPort) {
    final Topology old = this.topology;
    Set<String> newServers = new HashSet<>(old.getServers());
    if (newServers.add(hostPort)) {
      this.topology = new Topology(newServers);
      for (TopologyListener listener : listeners) {
        listener.nodeAdded(hostPort);
      }
      return true;
    }

    return false;
  }

  private synchronized boolean removePassive(String hostPort) {
    final Topology old = this.topology;
    Set<String> newServers = new HashSet<>(old.getServers());
    if (newServers.remove(hostPort)) {
      this.topology = new Topology(newServers);
      for (TopologyListener listener : listeners) {
        listener.nodeRemoved(hostPort);
      }
      return true;
    }

    return false;
  }

  public synchronized void addListener(TopologyListener listener) {
    this.listeners.add(listener);
  }

  public interface TopologyMbean {
    boolean addPassive(String hostPort);

    boolean removePassive(String hostPort);

    String getTopology();
  }

  private static class TopologyMbeanImpl extends AbstractTerracottaMBean implements TopologyMbean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyMbeanImpl.class);
    private static final String MBEAN_NAME = "TopologyMBean";
    private final TopologyManager topologyManager;

    TopologyMbeanImpl(TopologyManager topologyManager) throws NotCompliantMBeanException {
      super(TopologyMbean.class, false);
      this.topologyManager = topologyManager;

      try {
        ObjectName topologyObjectName =
            TerracottaManagement.createObjectName(null, MBEAN_NAME, TerracottaManagement.MBeanDomain.PUBLIC);
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, topologyObjectName);
      } catch (Exception e) {
        LOGGER.warn("Problem registering MBean with name " + MBEAN_NAME, e);
      }
    }

    public boolean addPassive(String hostPort) {
      return this.topologyManager.addPassive(hostPort);
    }

    public boolean removePassive(String hostPort) {
      return this.topologyManager.removePassive(hostPort);
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
