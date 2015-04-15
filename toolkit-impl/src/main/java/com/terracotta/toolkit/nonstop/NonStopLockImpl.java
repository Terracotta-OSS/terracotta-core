/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class NonStopLockImpl implements ToolkitLock {
  private final NonStopContext                   context;
  private final NonStopConfigurationLookup       nonStopConfigurationLookup;
  private final ToolkitObjectLookup<ToolkitLock> toolkitObjectLookup;

  public NonStopLockImpl(NonStopContext context, NonStopConfigurationLookup nonStopConfigurationLookup,
                         ToolkitObjectLookup<ToolkitLock> toolkitObjectLookup) {
    this.context = context;
    this.nonStopConfigurationLookup = nonStopConfigurationLookup;
    this.toolkitObjectLookup = toolkitObjectLookup;

  }

  private long getTimeout(NonStopConfiguration nonStopConfiguration) {
    if (nonStopConfiguration.isEnabled()) {
      return nonStopConfiguration.getTimeoutMillis();
    } else {
      return -1;
    }
  }

  @Override
  public void unlock() {
    // unlock should never throw nonstop exception.
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod("unlock");

    if (!nonStopConfiguration.isEnabled()) {
      toolkitObjectLookup.getInitializedObject().unlock();
    } else {
      ToolkitLock localDelegate = toolkitObjectLookup.getInitializedObjectOrNull();
      if (localDelegate == null) {
        // since the toolkit is not initialized yet. This could never have been locked.
        throw new IllegalMonitorStateException();
      }
      try {
        localDelegate.unlock();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("unlock done but transaction Commit failed!");
      }
    }
  }

  @Override
  public void lock() {
    String methodName = "lock";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      toolkitObjectLookup.getInitializedObject().lock();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }

      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        toolkitObjectLookup.getInitializedObject().lock();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    String methodName = "lockInterruptibly";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup
        .getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      toolkitObjectLookup.getInitializedObject().lockInterruptibly();
    } else {

      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        toolkitObjectLookup.getInitializedObject().lockInterruptibly();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public boolean tryLock() {
    String methodName = "tryLock";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().tryLock();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().tryLock();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    String methodName = "tryLock";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().tryLock(time, unit);
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().tryLock(time, unit);
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public String getName() {
    String methodName = "getName";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().getName();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().getName();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public Condition newCondition() throws UnsupportedOperationException {
    String methodName = "newCondition";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().newCondition();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().newCondition();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public Condition getCondition() {
    String methodName = "getCondition";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().getCondition();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }

      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().getCondition();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public ToolkitLockType getLockType() {
    String methodName = "getLockType";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().getLockType();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }

      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().getLockType();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public boolean isHeldByCurrentThread() {
    String methodName = "isHeldByCurrentThread";
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(methodName);

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().isHeldByCurrentThread();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw getNonStopException(methodName); }

      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().isHeldByCurrentThread();
      } catch (ToolkitAbortableOperationException e) {
        throw getNonStopException(methodName);
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw getNonStopException(methodName);
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  private NonStopException getNonStopException(String methodName) {
    if (context.getNonStopClusterListener().isNodeError()) {
      return new NonStopException(context.getNonStopClusterListener().getNodeErrorMessage());
    } else {
      return new NonStopException(methodName + " timed out");
    }
  }
}
