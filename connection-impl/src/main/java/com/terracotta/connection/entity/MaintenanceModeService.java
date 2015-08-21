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

  public <T extends Entity> void readLockEntity(Class<T> c, String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        lock(lockID, LockLevel.READ);
      }
    }));
  }

  public <T extends Entity> void readUnlockEntity(Class<T> c, String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        unlock(lockID, LockLevel.READ);
      }
    }));
  }

  public <T extends Entity> void enterMaintenanceMode(Class<T> c, String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        lock(lockID, LockLevel.WRITE);
      }
    }));
  }

  public <T extends Entity> void exitMaintenanceMode(Class<T> c, String name) {
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
