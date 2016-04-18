/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.terracotta.connection.entity;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import org.terracotta.connection.entity.Entity;

import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.EntityLockID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;


public class MaintenanceModeService {
  private static TCLogger LOGGER = TCLogging.getLogger(MaintenanceModeService.class);
  private final ClientLockManager clientLockManager;

  public MaintenanceModeService(ClientLockManager clientLockManager) {
    this.clientLockManager = clientLockManager;
  }

  public <T extends Entity> void readLockEntity(final Class<T> c, final String name) {
    LockID lockID = new EntityLockID(c.getName(), name);
    lock(lockID, LockLevel.READ);
  }

  public <T extends Entity> void readUnlockEntity(final Class<T> c, final String name) {
    LockID lockID = new EntityLockID(c.getName(), name);
    unlock(lockID, LockLevel.READ);
  }

  public <T extends Entity> void enterMaintenanceMode(final Class<T> c, final String name) {
    LockID lockID = new EntityLockID(c.getName(), name);
    lock(lockID, LockLevel.WRITE);
  }

  public <T extends Entity> boolean tryEnterMaintenanceMode(final Class<T> c, final String name) {
    LockID lockID = new EntityLockID(c.getName(), name);
    return tryLock(lockID, LockLevel.WRITE);
  }

  public <T extends Entity> void exitMaintenanceMode(final Class<T> c, final String name) {
    LockID lockID = new EntityLockID(c.getName(), name);
    unlock(lockID, LockLevel.WRITE);
  }

  private void lock(LockID id, LockLevel level) {
    long time = System.nanoTime();
    this.clientLockManager.lock(id, level);
    if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("time to lock:" + id + " " + level + " " + (System.nanoTime() - time) + " nanos");
    }
  }

  private boolean tryLock(LockID id, LockLevel level) {
    long time = System.nanoTime();
    try {
      return this.clientLockManager.tryLock(id, level);
    } finally {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("time to try lock:" + id + " " + level + " " + (System.nanoTime() - time) + " nanos");
      }
    }
  }

  private void unlock(LockID id, LockLevel level) {
    long time = System.nanoTime();
    this.clientLockManager.unlock(id, level);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("time to unlock:" + id + " " + level + " " + (System.nanoTime() - time) + " nanos");
      }
  }
}
