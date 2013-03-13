/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Eugene Shelestovich
 */
class MockToolkitReadWriteLock implements ToolkitReadWriteLock {

  private final ToolkitLock readLock;
  private final ToolkitLock writeLock;
  private final String name;

  MockToolkitReadWriteLock() {
    this("mockToolkitReadWriteLock");
  }

  MockToolkitReadWriteLock(final String name) {
    final ReentrantReadWriteLock targetLock = new ReentrantReadWriteLock();
    this.writeLock = new MockToolkitWriteLock(targetLock);
    this.readLock = new MockToolkitReadLock(targetLock);
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ToolkitLock readLock() {
    return readLock;
  }

  @Override
  public ToolkitLock writeLock() {
    return writeLock;
  }

  private static class MockToolkitWriteLock extends ReentrantReadWriteLock.WriteLock implements ToolkitLock {

    private final Condition singleCondition;

    protected MockToolkitWriteLock(final ReentrantReadWriteLock lock) {
      super(lock);
      this.singleCondition = super.newCondition();
    }

    @Override
    public Condition getCondition() {
      return singleCondition;
    }

    @Override
    public ToolkitLockType getLockType() {
      return ToolkitLockType.WRITE;
    }

    @Override
    public String getName() {
      return "mockToolkitWriteLock";
    }
  }

  private static class MockToolkitReadLock extends ReentrantReadWriteLock.ReadLock implements ToolkitLock {

    protected MockToolkitReadLock(final ReentrantReadWriteLock lock) {
      super(lock);
    }

    @Override
    public Condition getCondition() {
      throw new UnsupportedOperationException("Cannot create a condition for a read lock");
    }

    @Override
    public ToolkitLockType getLockType() {
      return ToolkitLockType.READ;
    }

    @Override
    public boolean isHeldByCurrentThread() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getName() {
      return "mockToolkitReadLock";
    }
  }

}
