/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;

public class NonStopConfigurationLookup {
  private final NonStopContext    context;
  private final ToolkitObjectType objectType;
  private final String            name;

  public NonStopConfigurationLookup(NonStopContext context, ToolkitObjectType objectType, String name) {
    this.context = context;
    this.objectType = objectType;
    this.name = name;
  }

  public ToolkitObjectType getObjectType() {
    return objectType;
  }

  public NonStopConfiguration getNonStopConfiguration() {
    return context.getNonStopConfigurationRegistry().getConfigForInstance(name, objectType);
  }

  public NonStopConfiguration getNonStopConfigurationForMethod(String methodName) {
    return context.getNonStopConfigurationRegistry().getConfigForInstanceMethod(methodName, name, objectType);
  }
}
