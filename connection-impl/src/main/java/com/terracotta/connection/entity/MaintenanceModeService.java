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
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import com.tc.util.Assert;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author twu
 */
public class MaintenanceModeService {
  private final ExecutorService executorService = new DirectExecutor();
  private final ClientLockManager clientLockManager;
 
  private class DirectExecutor extends AbstractExecutorService {
    
    @Override
    public void shutdown() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void execute(Runnable command) {
      command.run();
    }
    
  }
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

  public <T extends Entity> boolean tryEnterMaintenanceMode(final Class<T> c, final String name) {
    TryLockRunnable runnable = new TryLockRunnable(c, name);
    await(executorService.submit(runnable));
    return runnable.didLock();
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

  private boolean tryLock(LockID id, LockLevel level) {
    return this.clientLockManager.tryLock(id, level);
  }

  private void unlock(LockID id, LockLevel level) {
    this.clientLockManager.unlock(id, level);
  }


  /**
   * We create this explicit runnable for tryLock since we want to know whether the lock did happen.
   */
  private class TryLockRunnable implements Runnable {
    private final Class<?> clazz;
    private final String name;
    private boolean didRun = false;
    private boolean didLock = false;
    
    public TryLockRunnable(Class<?> clazz, String name) {
      this.clazz = clazz;
      this.name = name;
    }

    /**
     * Checks if the lock was successful and also asserts that the attempt was at least made.
     * @return True if the lock was successful
     */
    public boolean didLock() {
      Assert.assertTrue(this.didRun);
      return this.didLock;
    }
    
    @Override
    public void run() {
      LockID lockID = new EntityLockID(clazz.getName(), name);
      this.didLock = tryLock(lockID, LockLevel.WRITE);
      this.didRun = true;
    }
  }
}
