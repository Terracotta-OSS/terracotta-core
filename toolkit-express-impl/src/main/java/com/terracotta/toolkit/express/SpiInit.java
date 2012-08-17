/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.DSOContext;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

public class SpiInit implements DSOContextControl {

  private final DSOContext dsoContext;

  public SpiInit(Object context) {
    this.dsoContext = (DSOContext) context;
  }

  @Override
  public void init(Set<String> tunnelledMBeanDomains) {
    ManagerUtil.enableSingleton(dsoContext.getManager());
    if (tunnelledMBeanDomains != null) {
      for (String mbeanDomain : tunnelledMBeanDomains) {
        dsoContext.getModuleConfigurtion().addTunneledMBeanDomain(mbeanDomain);
      }
    }
  }

  @Override
  public void shutdown() {
    dsoContext.shutdown();
  }

  @Override
  public void activateModules(Collection<URL> modules) {
    // module might have registered new mbean domains
    dsoContext.getManager().getTunneledDomainUpdater().sendCurrentTunneledDomains();

  }
}
