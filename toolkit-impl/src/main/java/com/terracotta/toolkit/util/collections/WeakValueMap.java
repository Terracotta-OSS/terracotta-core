/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util.collections;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WeakValueMap<V> {
  private final ReferenceQueue                     referenceQueue = new ReferenceQueue();
  private final Map<String, NamedWeakReference<V>> internalMap    = new HashMap<String, NamedWeakReference<V>>();

  WeakValueMap() {
    // make constructor package protected
  }

  public synchronized V get(String name) {
    cleanupReferenceQueue();

    NamedWeakReference<V> weakReference = internalMap.get(name);
    if (weakReference == null) { return null; }

    return weakReference.get();
  }

  public synchronized V put(String name, V value) {
    return put(name, value, null);
  }

  public synchronized V put(String name, V value, WeakValueGCCallback weakValueGCCallback) {
    cleanupReferenceQueue();

    NamedWeakReference<V> reference = new NamedWeakReference(name, value, referenceQueue, weakValueGCCallback);
    NamedWeakReference<V> oldReference = internalMap.put(name, reference);
    return oldReference == null ? null : oldReference.get();
  }

  public synchronized V remove(String name) {
    cleanupReferenceQueue();

    NamedWeakReference<V> oldReference = internalMap.remove(name);
    return oldReference == null ? null : oldReference.get();
  }

  void cleanupReferenceQueue() {
    while (true) {
      Object gcdObject = referenceQueue.poll();
      if (gcdObject == null) { return; }

      NamedWeakReference<V> weakReference = (NamedWeakReference) gcdObject;
      internalMap.remove(weakReference.getName());

      gcCallbackIfNecessary(weakReference);
    }
  }

  private void gcCallbackIfNecessary(NamedWeakReference<V> weakReference) {
    WeakValueGCCallback callback = weakReference.getGcCallback();
    if (callback != null) {
      callback.callback();
    }
  }

  private static class NamedWeakReference<V> extends WeakReference<V> {
    private final String              name;
    private final WeakValueGCCallback gcCallback;

    public NamedWeakReference(String name, V reference, ReferenceQueue referenceQueue, WeakValueGCCallback gcCallback) {
      super(reference, referenceQueue);
      this.name = name;
      this.gcCallback = gcCallback;
    }

    public String getName() {
      return name;
    }

    public WeakValueGCCallback getGcCallback() {
      return gcCallback;
    }
  }
}
