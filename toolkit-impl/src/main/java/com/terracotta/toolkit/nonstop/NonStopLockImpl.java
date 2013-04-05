/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod("lock");

    if (!nonStopConfiguration.isEnabled()) {
      toolkitObjectLookup.getInitializedObject().lock();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException("lock timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        toolkitObjectLookup.getInitializedObject().lock();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("lock timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("lock timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup
        .getNonStopConfigurationForMethod("lockInterruptibly");

    if (!nonStopConfiguration.isEnabled()) {
      toolkitObjectLookup.getInitializedObject().lockInterruptibly();
    } else {

      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "lockInterruptibly timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        toolkitObjectLookup.getInitializedObject().lockInterruptibly();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("lockInterruptibly timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("lockInterruptibly timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public boolean tryLock() {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod("tryLock");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().tryLock();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "tryLock timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().tryLock();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("tryLock timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("tryLock timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod("tryLock");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().tryLock(time, unit);
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "tryLock timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().tryLock(time, unit);
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("tryLock timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("tryLock timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public String getName() {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod("getName");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().getName();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "getName timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().getName();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("getName timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("getName timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public Condition newCondition() throws UnsupportedOperationException {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup
        .getNonStopConfigurationForMethod("newCondition");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().newCondition();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "newCondition timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().newCondition();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("newCondition timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("newCondition timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public Condition getCondition() {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup
        .getNonStopConfigurationForMethod("getCondition");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().getCondition();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "getCondition timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().getCondition();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("getCondition timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("getCondition timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public ToolkitLockType getLockType() {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup
        .getNonStopConfigurationForMethod("getLockType");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().getLockType();
    } else {

      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "getLockType timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().getLockType();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("getLockType timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("getLockType timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }

  @Override
  public boolean isHeldByCurrentThread() {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup
        .getNonStopConfigurationForMethod("isHeldByCurrentThread");

    if (!nonStopConfiguration.isEnabled()) {
      return toolkitObjectLookup.getInitializedObject().isHeldByCurrentThread();
    } else {
      if (nonStopConfiguration.isImmediateTimeoutEnabled()
          && !context.getNonStopClusterListener().areOperationsEnabled()) { throw new NonStopException(
                                                                                                       "isHeldByCurrentThread timed out"); }
      boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
      try {
        context.getNonStopClusterListener().waitUntilOperationsEnabled();
        return toolkitObjectLookup.getInitializedObject().isHeldByCurrentThread();
      } catch (ToolkitAbortableOperationException e) {
        throw new NonStopException("isHeldByCurrentThread timed out");
      } catch (RejoinException e) {
        // TODO: Review this.. Is this the right place to handle this...
        throw new NonStopException("isHeldByCurrentThread timed out");
      } finally {
        if (started) {
          context.getNonStopManager().finish();
        }
      }
    }
  }
}
