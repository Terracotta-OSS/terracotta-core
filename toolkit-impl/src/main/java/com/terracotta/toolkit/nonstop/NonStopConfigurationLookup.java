/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;

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
    NonStopConfiguration config = context.getNonStopConfigurationRegistry()
        .getConfigForInstance(name, objectType);
    if (!context.isEnabledForCurrentThread()) {
      return new DisabledNonStopConfiguration(config);
    }
    return config;
  }

  public NonStopConfiguration getNonStopConfigurationForMethod(String methodName) {
    NonStopConfiguration config = context.getNonStopConfigurationRegistry()
        .getConfigForInstanceMethod(methodName, name, objectType);
    if (!context.isEnabledForCurrentThread()) {
      return new DisabledNonStopConfiguration(config);
    }
    return config;
  }


  private static final class DisabledNonStopConfiguration implements NonStopConfiguration {

    private final NonStopConfiguration delegate;

    public DisabledNonStopConfiguration(NonStopConfiguration delegate) {
      this.delegate = delegate;
    }

    @Override
    public NonStopConfigurationFields.NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
      return delegate.getReadOpNonStopTimeoutBehavior();
    }

    @Override
    public NonStopConfigurationFields.NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
      return delegate.getWriteOpNonStopTimeoutBehavior();
    }

    @Override
    public long getTimeoutMillis() {
      return delegate.getTimeoutMillis();
    }

    @Override
    public long getSearchTimeoutMillis() {
      return delegate.getSearchTimeoutMillis();
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    public boolean isImmediateTimeoutEnabled() {
      return delegate.isImmediateTimeoutEnabled();
    }
  }

}
