/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.tc.object.bytecode.hook.DSOContext;
import com.tc.platform.StaticPlatformApi;

import java.util.Set;

public class SpiInit implements DSOContextControl {

  private final DSOContext dsoContext;

  public SpiInit(Object context) {
    this.dsoContext = (DSOContext) context;
    StaticPlatformApi.enableSingleton(dsoContext.getManager());
    // ManagerUtil.enableSingleton(dsoContext.getManager());
  }

  @Override
  public void activateTunnelledMBeanDomains(Set<String> tunnelledMBeanDomains) {
    boolean sendCurrentTunnelledDomains = false;
    if (tunnelledMBeanDomains != null) {
      for (String mbeanDomain : tunnelledMBeanDomains) {
        dsoContext.addTunneledMBeanDomain(mbeanDomain);
        sendCurrentTunnelledDomains = true;
      }
    }
    if (sendCurrentTunnelledDomains) {
      dsoContext.getManager().getTunneledDomainUpdater().sendCurrentTunneledDomains();
    }
  }

  @Override
  public void shutdown() {
    dsoContext.shutdown();
  }

  @Override
  public boolean isOnline() {
    return dsoContext.getManager().getDsoCluster().areOperationsEnabled();
  }
}
