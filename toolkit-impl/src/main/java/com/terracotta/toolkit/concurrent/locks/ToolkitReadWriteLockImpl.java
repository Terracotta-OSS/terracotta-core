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
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;

public class ToolkitReadWriteLockImpl implements ToolkitReadWriteLock {
  private final String                      name;
  private final UnnamedToolkitReadWriteLock delegate;

  public ToolkitReadWriteLockImpl(PlatformService platformService, String name) {
    this.name = name;
    this.delegate = ToolkitLockingApi.createUnnamedReadWriteLock(ToolkitObjectType.READ_WRITE_LOCK, name,
                                                                 platformService, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ToolkitLock readLock() {
    return delegate.readLock();
  }

  @Override
  public ToolkitLock writeLock() {
    return delegate.writeLock();
  }
}
