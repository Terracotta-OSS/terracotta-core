/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLongImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitIDGeneratorImpl;

public class ToolkitAtomicLongFactoryImpl implements ToolkitObjectFactory<ToolkitAtomicLong> {
  private final ToolkitStore           atomicLongs;
  private final ToolkitIDGeneratorImpl longIdGenerator;

  public ToolkitAtomicLongFactoryImpl(ToolkitStore atomicLongs) {
    this.atomicLongs = atomicLongs;
    longIdGenerator = new ToolkitIDGeneratorImpl(ToolkitTypeConstants.TOOLKIT_LONG_UID_NAME, atomicLongs);
  }

  @Override
  public ToolkitAtomicLong getOrCreate(String name, Configuration config) {
    return new ToolkitAtomicLongImpl(name, atomicLongs, longIdGenerator);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.ATOMIC_LONG;
  }
}
