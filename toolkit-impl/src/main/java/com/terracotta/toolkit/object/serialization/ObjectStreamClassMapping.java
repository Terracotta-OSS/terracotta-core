/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.abortable.AbortableOperationManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.platform.PlatformService;
import com.tc.util.runtime.Vm;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

public class ObjectStreamClassMapping {
  // package protected used in tests
  static final String                             SERIALIZER_ADD_MAPPING_THREAD = "Serializer Add Mapping Thread";
  private static final TCLogger                   LOGGER                        = TCLogging
                                                                                    .getLogger(ObjectStreamClassMapping.class);
  private static final Field                      SUPER_DESC;
  private static final Method                     IS_SERIALIZABLE;
  private static final String                     CHARSET                       = "ISO-8859-1";
  private static final String                     NEXT_MAPPING                  = "nextMapping";
  private static final String                     LOCK_NAME                     = "lock-for-" + NEXT_MAPPING;
  private final SerializerMap                     serializerMap;
  private final ReferenceQueue<ObjectStreamClass> oscSoftQueue;
  private final Map<Integer, CachedOscReference>  localCache;
  private final ToolkitLockImpl                   lock;
  private static final ExecutorService            executor                      = Executors
                                                                                    .newSingleThreadExecutor(new ThreadFactory() {

                                                                                      @Override
                                                                                      public Thread newThread(Runnable runnable) {
                                                                                        Thread t = new Thread(runnable,
                                                                                                              SERIALIZER_ADD_MAPPING_THREAD);
                                                                                        t.setDaemon(true);
                                                                                        return t;
                                                                                      }
                                                                                    });
  private final AbortableOperationManager         abortableOperationManager;

  static {
    Field superDesc = null;
    Method isSerializable = null;
    if (Vm.isJRockit()) {
      if (Boolean.getBoolean(ObjectStreamClassMapping.class.getName() + ".disablePruning")) {
        LOGGER
            .warn("JRockit ObjectStreamClass pruning work-around explicitly disabled.  Invalid caching behavior may result.");
      } else {
        try {
          superDesc = ObjectStreamClass.class.getDeclaredField("superDesc");
          isSerializable = ObjectStreamClass.class.getDeclaredMethod("isSerializable");
          superDesc.setAccessible(true);
          isSerializable.setAccessible(true);
          LOGGER.info("JRockit ObjectStreamClass pruning work-around enabled.");
        } catch (Throwable ex) {
          LOGGER
              .error("JRockit ObjectStreamClass pruning work-around could not be enabled.  Invalid caching behavior may result.",
                     ex);
          superDesc = null;
          isSerializable = null;
        }
      }
    }
    SUPER_DESC = superDesc;
    IS_SERIALIZABLE = isSerializable;
  }

  public ObjectStreamClassMapping(PlatformService platformService, SerializerMap serializerMap) {
    /**
     * For each ObjectStreamClass, this map contains two entries 1. <key, int> and 2. <int, byte []> where key is String
     * representation of SerializableDataKey.
     */
    this.serializerMap = serializerMap;
    this.oscSoftQueue = new ReferenceQueue<ObjectStreamClass>();
    this.localCache = new ConcurrentHashMap<Integer, CachedOscReference>();
    this.lock = new ToolkitLockImpl(platformService, LOCK_NAME, ToolkitLockTypeInternal.WRITE);
    platformService.registerBeforeShutdownHook(new Runnable() {
      @Override
      public void run() {
        executor.shutdown();
      }
    });
    this.abortableOperationManager = platformService.getAbortableOperationManager();

  }

  /**
   * this method has to be called from lock
   * 
   * @param key2
   */
  private Integer addMapping(ObjectStreamClass desc, SerializableDataKey key) {
    Integer value = getAndIncrement();
    put(String.valueOf(value), key.getSerializedOsc());
    put(key.getStringForm(), value);
    // eagerly put in local cache too from mutating node
    localCache.put(value, new CachedOscReference(value, desc, oscSoftQueue));
    return value;
  }

  private Integer getAndIncrement() {
    Integer oldMapping = (Integer) serializerMap.get(NEXT_MAPPING);
    if (oldMapping == null) {
      oldMapping = Integer.valueOf(0);
    }
    serializerMap.put(NEXT_MAPPING, Integer.valueOf(oldMapping.intValue() + 1));
    return oldMapping;
  }

  // TODO: we are using SerializableDataKey always and not using ComparisonSerializableDataKey to probe this map
  // this is because keys in map are String and there is no benefit to compare ComparisonSerializableDataKey vs String
  // This will be slow, we can optimize it in two ways :
  // 1. Have a local cache CHM of hashCode of ObjectStreamClass -> List<CO> where CO = kclass and mapping
  // 2. Modify our serializerMap to have complex object as keys rather than only String as keys.
  public int getMappingFor(ObjectStreamClass descParam) throws IOException {
    final ObjectStreamClass desc = prune(descParam);
    final SerializableDataKey key = new SerializableDataKey(desc);

    Integer value = (Integer) serializerMap.localGet(key.getStringForm());
    if (value != null) { return value.intValue(); }
    // Add Mapping in a new Thread in order to not break the user transaction.

    try {
      Future<Integer> future = executor.submit(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
          lock.lock();
          try {
            Integer rv = (Integer) serializerMap.localGet(key.getStringForm());
            if (rv != null) { return rv; }
            rv = addMapping(desc, key);
            return rv;
          } finally {
            lock.unlock();
          }
        }
      });
      return future.get();
    } catch (RejectedExecutionException e) {
      throw new ToolkitRuntimeException(e);
    } catch (InterruptedException e) {
      if (abortableOperationManager.isAborted()) {
        throw new ToolkitAbortableOperationException();
      } else {
        throw new ToolkitRuntimeException(e);
      }
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RejoinException) { throw (RejoinException) e.getCause(); }
      throw new ToolkitRuntimeException(e);
    }
  }

  ObjectStreamClass localGetObjectStreamClassFor(int mapping) {
    SoftReference<ObjectStreamClass> oscRef = localCache.get(mapping);
    if (oscRef == null) {
      return null;
    } else {
      return oscRef.get();
    }
  }

  public ObjectStreamClass getObjectStreamClassFor(int mapping) throws ClassNotFoundException {
    processOscQueue();
    ObjectStreamClass osc = localGetObjectStreamClassFor(mapping);
    if (osc == null) {
      byte[] serializedOsc = (byte[]) serializerMap.get(String.valueOf(mapping));
      if (serializedOsc == null) { throw new AssertionError("missing reverse mapping for " + mapping); }

      try {
        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(serializedOsc)) {

          @Override
          protected Class<?> resolveClass(ObjectStreamClass o) {
            // We don't want our cached OSC instances referencing user classes as we could cause a perm-gen leak.
            return null;
          }
        };
        try {
          osc = (ObjectStreamClass) oin.readObject();
          localCache.put(mapping, new CachedOscReference(mapping, osc, oscSoftQueue));
        } finally {
          oin.close();
        }
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
    return osc;
  }

  private void put(String key, Serializable value) {
    Object prev = serializerMap.put(key, value);
    if (prev != null) {
      // this shouldn't ever happen
      throw new AssertionError("replaced mapping for key (" + key + "), old value = " + prev + ", new value = " + value);
    }

  }

  private void processOscQueue() {
    while (true) {
      CachedOscReference ref = (CachedOscReference) oscSoftQueue.poll();
      if (ref == null) {
        break;
      } else {
        localCache.remove(ref.getKey());
      }
    }
  }

  private static class SerializableDataKey {
    private final byte[] serializedOsc;
    private final String stringForm;

    public SerializableDataKey(ObjectStreamClass desc) throws IOException {
      this.serializedOsc = getSerializedForm(desc);
      this.stringForm = new String(serializedOsc, Charset.forName(CHARSET));
    }

    public String getStringForm() {
      return stringForm;
    }

    public byte[] getSerializedOsc() {
      return serializedOsc;
    }
  }

  private static class CachedOscReference extends SoftReference<ObjectStreamClass> {

    private final int key;

    public CachedOscReference(int key, ObjectStreamClass osc, ReferenceQueue<ObjectStreamClass> queue) {
      super(osc, queue);
      this.key = key;
    }

    public int getKey() {
      return key;
    }
  }

  private static byte[] getSerializedForm(ObjectStreamClass desc) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ObjectOutputStream oout = new ObjectOutputStream(bout);
    try {
      oout.writeObject(desc);
    } finally {
      oout.close();
    }
    return bout.toByteArray();
  }

  private static ObjectStreamClass prune(ObjectStreamClass desc) {
    if (IS_SERIALIZABLE == null || SUPER_DESC == null) {
      return desc;
    } else {
      try {
        for (ObjectStreamClass osc = desc; osc != null; osc = (ObjectStreamClass) SUPER_DESC.get(osc)) {
          ObjectStreamClass superDesc = (ObjectStreamClass) SUPER_DESC.get(osc);
          if (superDesc != null && !((Boolean) IS_SERIALIZABLE.invoke(superDesc)).booleanValue()) {
            SUPER_DESC.set(osc, null);
          }
        }
        return desc;
      } catch (Throwable t) {
        LOGGER.warn("JRockit ObjectStreamClass pruning work-around failed.", t);
        return desc;
      }
    }
  }
}
