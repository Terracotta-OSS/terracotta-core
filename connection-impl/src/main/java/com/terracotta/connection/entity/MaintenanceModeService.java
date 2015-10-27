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

import org.terracotta.connection.entity.Entity;

import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.EntityLockID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public class MaintenanceModeService {
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ClientLockManager clientLockManager;

  public MaintenanceModeService(ClientLockManager clientLockManager) {
    this.clientLockManager = clientLockManager;
  }

  private static void await(Future<?> future) {
    boolean interrupted = false;
    while (true) {
      try {
        future.get();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  public <T extends Entity> void readLockEntity(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        lock(lockID, LockLevel.READ);
      }
    }));
  }

  public <T extends Entity> void readUnlockEntity(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        unlock(lockID, LockLevel.READ);
      }
    }));
  }

  public <T extends Entity> void enterMaintenanceMode(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        lock(lockID, LockLevel.WRITE);
      }
    }));
  }

  public <T extends Entity> void exitMaintenanceMode(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        unlock(lockID, LockLevel.WRITE);
      }
    }));
  }

  private void lock(LockID id, LockLevel level) {
    this.clientLockManager.lock(id, level);
  }

  private void unlock(LockID id, LockLevel level) {
    this.clientLockManager.unlock(id, level);
  }
}
