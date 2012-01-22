/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.util.UUID;

public interface DSOMBeanConfig {

  /**
   * Returns a unique identifier for this helper, which is exposed to clients via Manager and ManagerUtil. This id
   * should be used when registering MBeans as the value for the ObjectName property <i>node</i>. This id is passed to
   * the ClientConnectEventHandler and is used to filter the set of beans to be tunneled to the server's MBeanServer.
   * 
   * @return {@code UUID}
   */
  UUID getUUID();

  String[] getTunneledDomains();

}
