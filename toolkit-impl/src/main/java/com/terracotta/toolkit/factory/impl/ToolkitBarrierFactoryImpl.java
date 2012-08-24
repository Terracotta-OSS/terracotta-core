/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.concurrent.ToolkitBarrierImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitIDGeneratorImpl;

public class ToolkitBarrierFactoryImpl implements ToolkitObjectFactory<ToolkitBarrier> {
  public static final String           PARTIES_CONFIG_NAME = "PARTIES";
  private final ToolkitStore           barriers;
  private final ToolkitIDGeneratorImpl barrierIdGenerator;

  public ToolkitBarrierFactoryImpl(ToolkitStore clusteredMap) {
    this.barriers = clusteredMap;
    barrierIdGenerator = new ToolkitIDGeneratorImpl(ToolkitTypeConstants.TOOLKIT_BARRIER_UID_NAME, barriers);
  }

  @Override
  public ToolkitBarrier getOrCreate(String name, Configuration config) {
    int parties = config.getInt(PARTIES_CONFIG_NAME);
    return new ToolkitBarrierImpl(name, parties, barriers, barrierIdGenerator);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.BARRIER;
  }
}
