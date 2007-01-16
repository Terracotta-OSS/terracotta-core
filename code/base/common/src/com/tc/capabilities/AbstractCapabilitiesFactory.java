/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.capabilities;

import com.tc.util.factory.AbstractFactory;

public abstract class AbstractCapabilitiesFactory extends AbstractFactory {
  private static Capabilities capabilitiesManager;
  private static String FACTORY_SERVICE_ID = "com.tc.capabilities.CapabilitiesFactory";
  private static Class STANDARD_CAPABILITIES_FACTORY_CLASS = StandardCapabilitiesFactory.class;
  
  public static AbstractCapabilitiesFactory getFactory() {
    return (AbstractCapabilitiesFactory)getFactory(FACTORY_SERVICE_ID, STANDARD_CAPABILITIES_FACTORY_CLASS);
  }

  public abstract Capabilities createCapabilitiesManager();
  
  public static Capabilities getCapabilitiesManager() {
    if(capabilitiesManager == null) {
      capabilitiesManager = getFactory().createCapabilitiesManager();
    }
    return capabilitiesManager;
  }
}
