/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.abortable.AbortableOperationManager;

import java.util.concurrent.FutureTask;

public class NonstopToolkitReadWriteLockDelegateProvider extends AbstractNonStopDelegateProvider<ToolkitReadWriteLock> {

  private final String        name;

  public NonstopToolkitReadWriteLockDelegateProvider(AbortableOperationManager abortableOperationManager,
                                             NonStopConfigRegistryImpl nonStopConfigRegistry,
                                             NonstopTimeoutBehaviorResolver behaviorResolver,
                                                     FutureTask<ToolkitInternal> toolkitDelegateFutureTask, String name) {
    super(toolkitDelegateFutureTask, abortableOperationManager, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
  }

  @Override
  public ToolkitReadWriteLock getToolkitObject() {
    return getToolkit().getReadWriteLock(name);
  }

  @Override
  public ToolkitObjectType getTolkitObjectType() {
    return ToolkitObjectType.LOCK;
  }

}
