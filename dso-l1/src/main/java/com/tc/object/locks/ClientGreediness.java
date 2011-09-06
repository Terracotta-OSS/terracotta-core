/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;

enum ClientGreediness {
  GARBAGE {
    @Override
    boolean canAward(final LockLevel level) throws GarbageLockException {
      throw GarbageLockException.GARBAGE_LOCK_EXCEPTION;
    }

    @Override
    boolean isFree() {
      return true;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    boolean isGarbage() {
      return true;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) throws GarbageLockException {
      throw GarbageLockException.GARBAGE_LOCK_EXCEPTION;
    }

    @Override
    ClientGreediness awarded(final ServerLockLevel level) throws GarbageLockException {
      throw GarbageLockException.GARBAGE_LOCK_EXCEPTION;
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recallCommitted() {
      return this;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      throw new AssertionError("Garbage locks have no exchange context representation.");
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  FREE {
    @Override
    boolean canAward(final LockLevel level) {
      return false;
    }

    @Override
    boolean isFree() {
      return true;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness awarded(final ServerLockLevel level) {
      switch (level) {
        case READ:
          return GREEDY_READ;
        case WRITE:
          return GREEDY_WRITE;
      }
      throw new AssertionError("Trying to award unknown ServerLockLevel " + level);
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return GARBAGE;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return null;
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  GREEDY_READ {
    @Override
    boolean canAward(final LockLevel level) {
      return level.isRead();
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return true;
    }

    @Override
    boolean flushOnUnlock() {
      return false;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      switch (level) {
        case READ:
          return this;
        case WRITE:
          return RECALLED_READ;
      }
      throw new AssertionError("Trying to request unknown ServerLockLevel " + level);
    }

    @Override
    ClientGreediness awarded(final ServerLockLevel level) {
      switch (level) {
        case READ:
          return GREEDY_READ;
        case WRITE:
          return GREEDY_WRITE;
      }
      throw new AssertionError("Trying to award unknown ServerLockLevel " + level);
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return RECALLED_READ;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_READ);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  GREEDY_WRITE {
    @Override
    boolean canAward(final LockLevel level) {
      return true;
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return true;
    }

    @Override
    boolean flushOnUnlock() {
      return false;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      if ((lease > 0) && (clientLock.pendingCount() > 0)) {
        return GREEDY_WRITE;
      } else {
        switch (level) {
          case READ:
            return RECALLED_WRITE_FOR_READ;
          case WRITE:
            return RECALLED_WRITE;
        }
        throw new AssertionError("Trying to recall for unknown ServerLockLevel " + level);
      }
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_WRITE);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  RECALLED_READ {
    @Override
    boolean canAward(final LockLevel level) {
      return false;
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return true;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this; // lock is being recalled - we'll get the per thread awards from the server later
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recallInProgress() {
      return READ_RECALL_IN_PROGRESS;
    }

    @Override
    ClientGreediness recallCommitted() {
      return FREE;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_READ);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  RECALLED_WRITE {
    @Override
    boolean canAward(final LockLevel level) {
      return false;
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return true;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this; // lock is being recalled - we'll get the per thread awards from the server later
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return this;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    ClientGreediness recallInProgress() {
      return WRITE_RECALL_IN_PROGRESS;
    }

    @Override
    ClientGreediness recallCommitted() {
      return FREE;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_WRITE);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  READ_RECALL_IN_PROGRESS {
    @Override
    boolean canAward(final LockLevel level) {
      return false;
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return true;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recallCommitted() {
      return FREE;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_READ);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  WRITE_RECALL_IN_PROGRESS {
    @Override
    boolean canAward(final LockLevel level) {
      return false;
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return true;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recallCommitted() {
      return FREE;
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_WRITE);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.WRITE;
    }
  },

  RECALLED_WRITE_FOR_READ {
    @Override
    boolean canAward(final LockLevel level) {
      return level.isRead();
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return true;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return false;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness recallInProgress() {
      return WRITE_RECALL_FOR_READ_IN_PROGRESS;
    }

    @Override
    ClientGreediness recallCommitted() {
      return GREEDY_READ;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      switch (level) {
        case READ:
          return this;
        case WRITE:
          return RECALLED_WRITE;
      }
      throw new AssertionError("Trying to recall for unknown ServerLockLevel " + level);
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_WRITE);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.READ;
    }
  },

  WRITE_RECALL_FOR_READ_IN_PROGRESS {
    @Override
    boolean canAward(final LockLevel level) {
      return level.isRead();
    }

    @Override
    boolean isFree() {
      return false;
    }

    @Override
    boolean isRecalled() {
      return false;
    }

    @Override
    boolean isGreedy() {
      return false;
    }

    @Override
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    boolean isRecallInProgress() {
      return true;
    }

    @Override
    boolean isGarbage() {
      return false;
    }

    @Override
    ClientGreediness recallCommitted() {
      return GREEDY_READ;
    }

    @Override
    ClientGreediness requested(final ServerLockLevel level) {
      return this;
    }

    @Override
    ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
      switch (level) {
        case READ:
          return this;
        case WRITE:
          return WRITE_RECALL_IN_PROGRESS;
      }
      throw new AssertionError("Trying to recall for unknown ServerLockLevel " + level);
    }

    @Override
    ClientGreediness markAsGarbage() {
      return this;
    }

    @Override
    ClientServerExchangeLockContext toContext(final LockID lock, final ClientID client) {
      return new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID,
                                                 ServerLockContext.State.GREEDY_HOLDER_WRITE);
    }

    @Override
    public ServerLockLevel getFlushLevel() {
      return ServerLockLevel.READ;
    }
  };

  abstract boolean canAward(LockLevel level) throws GarbageLockException;

  abstract boolean isFree();

  abstract boolean isRecalled();

  abstract boolean isGreedy();

  abstract boolean flushOnUnlock();

  abstract boolean isRecallInProgress();

  abstract boolean isGarbage();

  abstract ServerLockLevel getFlushLevel();

  /**
   * @throws GarbageLockException thrown if in a garbage state
   */
  ClientGreediness requested(final ServerLockLevel level) throws GarbageLockException {
    throw new AssertionError("request level while in unexpected state (" + this + ")");
  }

  /**
   * @throws GarbageLockException thrown if in a garbage state
   */
  ClientGreediness awarded(final ServerLockLevel level) throws GarbageLockException {
    throw new AssertionError("award while in unexpected state (" + this + ")");
  }

  ClientGreediness recalled(final ClientLock clientLock, final int lease, final ServerLockLevel level) {
    throw new AssertionError("recalled while in unexpected state (" + this + ")");
  }

  ClientGreediness recallInProgress() {
    throw new AssertionError("recall in progress while in unexpected state (" + this + ")");
  }

  ClientGreediness recallCommitted() {
    throw new AssertionError("recall committed while in unexpected state (" + this + ")");
  }

  ClientGreediness markAsGarbage() {
    throw new AssertionError("marking as garbage while in unexpected state (" + this + ")");
  }

  abstract ClientServerExchangeLockContext toContext(LockID lock, ClientID client);
}
