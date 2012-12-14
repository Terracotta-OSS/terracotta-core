/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.terracotta.toolkit.AsyncToolkitInitializer;

public class NonstopToolkitLockDelegateProvider extends AbstractNonStopDelegateProvider<ToolkitLock> {

  private final String                  name;
  private final ToolkitLockTypeInternal lockType;

  public NonstopToolkitLockDelegateProvider(NonStopConfigRegistryImpl nonStopConfigRegistry,
                                            NonstopTimeoutBehaviorResolver behaviorResolver,
                                            AsyncToolkitInitializer asyncToolkitInitializer, String name,
                                            ToolkitLockTypeInternal lockType) {
    super(asyncToolkitInitializer, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
    this.lockType = lockType;
  }

  @Override
  public ToolkitLock getToolkitObject() {
    return getToolkit().getLock(name, lockType);
  }

  @Override
  public ToolkitObjectType getToolkitObjectType() {
    return ToolkitObjectType.LOCK;
  }

}
