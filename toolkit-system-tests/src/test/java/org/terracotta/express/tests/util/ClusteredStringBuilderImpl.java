/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.store.ToolkitStore;

public class ClusteredStringBuilderImpl implements ClusteredStringBuilder {
  private static final String                EMPTY_STRING = "";
  private final String                       name;
  private final ToolkitStore<String, String> buckets;

  public ClusteredStringBuilderImpl(String name, ToolkitStore<String, String> rootMap) {
    this.name = name;
    this.buckets = rootMap;
    String value = this.buckets.get(name);
    if (value == null) {
      buckets.putIfAbsent(name, EMPTY_STRING);
    }
  }

  private String getContent() {
    String content = buckets.get(name);
    if (content == null) { throw new IllegalStateException("ClusteredTextBucket " + name
                                                           + " has already been deregistered and no longer exist"); }
    return content;
  }

  private void updateContent(String updatedValue) {
    buckets.putNoReturn(name, updatedValue);
  }

  @Override
  public int length() {
    return getContent().length();
  }

  @Override
  public char charAt(int index) {
    return getContent().charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return getContent().subSequence(start, end);
  }

  @Override
  public ClusteredStringBuilder append(CharSequence csq) {
    ToolkitLock lock = buckets.createLockForKey(name).writeLock();
    lock.lock();
    try {
      updateContent(getContent() + csq);
      return this;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ClusteredStringBuilder append(CharSequence csq, int start, int end) {
    ToolkitLock lock = buckets.createLockForKey(name).writeLock();
    lock.lock();
    try {
      updateContent(getContent() + csq.subSequence(start, end));
      return this;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ClusteredStringBuilder append(char c) {
    ToolkitLock lock = buckets.createLockForKey(name).writeLock();
    lock.lock();
    try {
      updateContent(getContent() + c);
      return this;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    return getContent();
  }
}
