package com.tc.object;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.util.AbortedOperationUtil;

/**
 * @author tim
 */
public class RemoteResourceManagerImpl implements RemoteResourceManager {
  private static final int                MAX_THROTTLE_MS          = 2000; // TODO: Make this configurable?

  private final AbortableOperationManager abortableOperationManager;
  private final RemoteTransactionManager  remoteTransactionManager;
  private volatile boolean                throttleStateInitialized = false;
  private volatile boolean                throwException           = false;
  private volatile long                   throttleTime             = 0;

  public RemoteResourceManagerImpl(RemoteTransactionManager mgr, AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
    this.remoteTransactionManager = mgr;
  }

  @Override
  public synchronized void handleThrottleMessage(final GroupID groupID, final boolean exception, final float throttle) {
    throwException = exception;
    throttleTime = (long) (throttle * MAX_THROTTLE_MS);
    if ( throttleTime > 0 ) {
      this.remoteTransactionManager.throttleProcessing(true);
    } else {
      this.remoteTransactionManager.throttleProcessing(false);
    }
    throttleStateInitialized = true;
    notifyAll();
  }

  @Override
  public void throttleIfMutationIfNecessary(final ObjectID parentObject) throws AbortedOperationException {
    if (!throttleStateInitialized) {
      synchronized (this) {
        boolean interrupted = false;
        while (!throttleStateInitialized) {
          try {
            wait();
          } catch (InterruptedException e) {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
            interrupted = true;
          }
        }
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (throwException) {
      throw new OutOfResourceException("Server is full.");
    } else if (throttleTime > 0) {
      try {
        Thread.sleep(throttleTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
