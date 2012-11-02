/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfig;
import org.terracotta.toolkit.nonstop.NonStopConfigRegistry;

import com.tc.exception.ImplementMe;

public class NonStopConfigRegistryImpl implements NonStopConfigRegistry {
  @Override
  public void registerForType(NonStopConfig config, ToolkitObjectType... types) {
    throw new ImplementMe();

  }

  @Override
  public void registerForInstance(NonStopConfig config, String toolkitTypeName, ToolkitObjectType... type) {
    throw new ImplementMe();

  }

  @Override
  public void registerForTypeMethod(NonStopConfig config, String methodName, ToolkitObjectType... type) {
    throw new ImplementMe();

  }

  @Override
  public void registerForInstanceMethod(NonStopConfig config, String methodName, String toolkitTypeName,
                                        ToolkitObjectType... type) {
    throw new ImplementMe();

  }

  @Override
  public NonStopConfig getConfigForType(ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig getConfigForInstance(String toolkitTypeName, ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig getConfigForTypeMethod(String methodName, ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig getConfigForInstanceMethod(String methodName, String toolkitTypeName, ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig deregisterForType(ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig deregisterForInstance(String toolkitTypeName, ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig deregisterForTypeMethod(String methodName, ToolkitObjectType type) {
    throw new ImplementMe();
  }

  @Override
  public NonStopConfig deregisterForInstanceMethod(String methodName, String toolkitTypeName, ToolkitObjectType type) {
    throw new ImplementMe();
  }
}
