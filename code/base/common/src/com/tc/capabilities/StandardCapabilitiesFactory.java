/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.capabilities;

public class StandardCapabilitiesFactory extends AbstractCapabilitiesFactory {
  public Capabilities createCapabilitiesManager() {
    return new StandardCapabilitiesImpl();
  }
}
