/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.util.factory.AbstractFactory;

public abstract class AbstractNodeFactory extends AbstractFactory {
  private static String FACTORY_SERVICE_ID          = "com.tc.admin.NodeFactory";
  private static Class  STANDARD_NODE_FACTORY_CLASS = StandardNodeFactory.class;

  public static AbstractNodeFactory getFactory() {
    return (AbstractNodeFactory) getFactory(FACTORY_SERVICE_ID, STANDARD_NODE_FACTORY_CLASS);
  }

  public abstract ClusterNode createClusterNode(IAdminClientContext adminClientContext);

  public abstract ClusterNode createClusterNode(IAdminClientContext adminClientContext, String host, int jmxPort,
                                                boolean autoConnect);
}
