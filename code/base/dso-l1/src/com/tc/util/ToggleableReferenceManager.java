/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.util.ToggleableStrongReference;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * This class provides a factory/manager for ToggleableHardReference instances. In some scenarios, we want an easy way
 * to programatically create strong references to shared objects. The one motivation for this is that strongly
 * referenced shared objects (referred from non-shared) will never be flushed by the memory manager. <br>
 * <br>
 * Internally this class maintains a map from object IDs to toggleable references. The references in the map always
 * weakly references the peer pojo, and can toggle between having a strong reference or not. Since the internal map is
 * not a shared object and it strongly references the reference object (which can in sometimes strongly references the
 * pojo), one can ensure that a pojo is strongly reachable.<br>
 * <br>
 * The last interesting but it is that the reference map is automatically cleaned up when the pojo become weakly
 * referenced<br>
 * <br>
 * The correct use of toggleable references is from within a method of the target pojo (since you can be guaranteed that
 * "this" is strongly reachable at the time the reference is created)
 */
public class ToggleableReferenceManager {

  private static TCLogger              logger   = TCLogging.getLogger(ToggleableReferenceManager.class);

  private final Map                    refs     = new ConcurrentHashMap(64);
  private final ReferenceQueue         refQueue = new ReferenceQueue();
  private final QueueProcessor         queueProcessor;
  private final SynchronizedInt        cleared  = new SynchronizedInt(0);
  private volatile ClientObjectManager objManager;

  public ToggleableReferenceManager() {
    queueProcessor = new QueueProcessor(this, refQueue);
  }

  public void start() {
    queueProcessor.start();
  }

  public void setObjectManager(ClientObjectManager clientObjManager) {
    this.objManager = clientObjManager;
  }

  public ToggleableStrongReference getOrCreateFor(ObjectID id, Object peer) {
    if (id == null || id.isNull()) { throw new NullPointerException("null ObjectID"); }
    if (peer == null) { throw new NullPointerException("null peer object"); }

    SometimesStrongAlwaysWeakReference rv = (SometimesStrongAlwaysWeakReference) refs.get(id);
    if (rv == null) {
      rv = new SometimesStrongAlwaysWeakReference(id, peer, objManager, refQueue);
      refs.put(id, rv);
    }
    return rv;
  }

  // for tests
  int size() {
    return refs.size();
  }

  // for tests
  int clearCount() {
    return cleared.get();
  }

  private void remove(ObjectID id) {
    if (id == null) throw new AssertionError();
    Object removed = refs.remove(id);
    if (removed != null) {
      cleared.increment();
    }
  }

  private static class SometimesStrongAlwaysWeakReference extends WeakReference implements ToggleableStrongReference {
    private final ObjectID            id;
    private Object                    strongReference;
    private final ClientObjectManager objManager;

    public SometimesStrongAlwaysWeakReference(ObjectID id, Object peer, ClientObjectManager objManager,
                                              ReferenceQueue refQueue) {
      super(peer, refQueue);
      this.objManager = objManager;
      this.id = id;
    }

    public void strongRef() {
      final Object peer;
      try {
        peer = objManager.lookupObject(id);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
      if (peer == null) { throw new AssertionError("null peer for " + id); }
      this.strongReference = peer;
    }

    public void clearStrongRef() {
      this.strongReference = null;
    }

    public String toString() {
      return getClass().getName() + strongReference == null ? "" : " (strongRef to "
                                                                   + strongReference.getClass().getName() + ")";
    }
  }

  private static class QueueProcessor extends Thread {
    private final ReferenceQueue             queue;
    private final ToggleableReferenceManager mgr;

    QueueProcessor(ToggleableReferenceManager mgr, ReferenceQueue queue) {
      this.mgr = mgr;
      this.queue = queue;
      setDaemon(true);
      setName(getClass().getName());
    }

    public void run() {
      try {
        while (true) {
          SometimesStrongAlwaysWeakReference ref = (SometimesStrongAlwaysWeakReference) queue.remove();
          mgr.remove(ref.id);
        }
      } catch (Throwable t) {
        logger.error("unhandled exception processing queue", t);
      }
    }
  }

}
