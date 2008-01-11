/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcclient.ehcache.TimeExpiryMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * This is an Terracotta implementation of a time based MemoryStore which use the time based eviction policy.
 *
 */
public class TimeExpiryMemoryStore extends MemoryStore {
  private static final Log LOG = LogFactory.getLog(TimeExpiryMemoryStore.class.getName());

  public TimeExpiryMemoryStore(Ehcache cache, Store diskStore) {
    super(cache, (DiskStore) diskStore);

    try {
      map = loadMapInstance(cache.getName());
      ((SpoolingTimeExpiryMap)map).initialize();
    } catch (CacheException e) {
      LOG.error(cache.getName() + "Cache: Cannot start TimeExpiryMemoryStore. Initial cause was " + e.getMessage(), e);
    }
  }

  private long getThreadIntervalSeconds(long threadIntervalSec, long timeToIdleSec, long timeToLiveSec) {
    if (timeToIdleSec <= 0) {
      return timeToLiveSec;
    } else if (timeToLiveSec <= 0) {
      return timeToIdleSec;
    } else if (timeToLiveSec <= timeToIdleSec) {
      return timeToLiveSec;
    } else if (timeToIdleSec < threadIntervalSec) { return timeToIdleSec; }
    return threadIntervalSec;
  }

  private Map loadMapInstance(String cacheName) throws CacheException {
    try {
      Class.forName("com.tcclient.ehcache.TimeExpiryMap");
            
      long threadIntervalSec = -1;
      long timeToIdleSec = -1;
      long timeToLiveSec = -1;
//      try {
//        Method method = cache.getClass().getMethod("getCacheConfiguration", new Class[0]);
//        
//        // Use ehcache 1.3.x call - cache.getCacheConfiguration()...
//        long[] ehcache13Params = getConfigWithReflection(method);
//        if(ehcache13Params != null) {
//          threadIntervalSec = ehcache13Params[0];
//          timeToIdleSec = ehcache13Params[1];
//          timeToLiveSec = ehcache13Params[2];
//        }
//      } catch(NoSuchMethodException e) {
//        // drop into default config block below
//      } 
      
      if(threadIntervalSec == -1) {
        // Use ehcache 1.2.x methods
        threadIntervalSec = cache.getDiskExpiryThreadIntervalSeconds();
        timeToIdleSec = cache.getTimeToIdleSeconds();
        timeToLiveSec = cache.getTimeToLiveSeconds();
      }

      threadIntervalSec = getThreadIntervalSeconds(threadIntervalSec, timeToIdleSec, timeToLiveSec);

      Map candidateMap = new SpoolingTimeExpiryMap(threadIntervalSec, timeToIdleSec, timeToLiveSec, cacheName);
      if (LOG.isDebugEnabled()) {
        LOG.debug(cache.getName() + " Cache: Using SpoolingTimeExpiryMap implementation");
      }
      return candidateMap;
    } catch (Exception e) {
      // Give up
      e.printStackTrace(System.err);
      throw new CacheException(cache.getName() + "Cache: Cannot find com.tcclient.ehcache.TimeExpiryMap.");
    }
  }
  
  private long[] getConfigWithReflection(Method getConfigMethod) throws CacheException {
    try {
      Object config = getConfigMethod.invoke(cache, new Object[0]);
      if(config == null) { 
        return null;
      }

      Class cacheConfigClass = Class.forName("net.sf.ehcache.config.CacheConfiguration", true, cache.getClass().getClassLoader());
      
      String[] methods = new String[] {"getDiskExpiryThreadIntervalSeconds",
                                       "getTimeToIdleSeconds",
                                       "getTimeToLiveSeconds" };
      long[] values = new long[methods.length];
      for(int i=0; i<methods.length; i++) {
        Method configMethod = cacheConfigClass.getMethod(methods[i], null);
        Long value = (Long)configMethod.invoke(config, null);
        values[i] = value.longValue();    
      }
  
if(values[0] != cache.getDiskExpiryThreadIntervalSeconds()) System.out.println("disk: " + values[0] + " ex:" + cache.getDiskExpiryThreadIntervalSeconds());      
if(values[1] != cache.getTimeToIdleSeconds()) System.out.println("idle: " + values[1] + " ex:" + cache.getTimeToIdleSeconds());      
if(values[2] != cache.getTimeToLiveSeconds()) System.out.println("live: " + values[2] + " ex:" + cache.getTimeToLiveSeconds());      
      
      return values;
      
    } catch(Exception e) {
      // Check if a normal CacheException occurs and just throw it
      if(e instanceof InvocationTargetException) {
        InvocationTargetException ite = (InvocationTargetException) e;
        if(ite.getCause() instanceof CacheException) {
          throw (CacheException) ite.getCause();
        } // else fall through 
      }
      
      // None of these should happen - if they do, it's bad
      throw new CacheException("Unexpected exception obtaining cache configuration via reflection: " + e.getMessage(), e);
    }
  }

  public final synchronized void putData(Element element) throws CacheException {
    if (element != null) {
        ((SpoolingTimeExpiryMap)map).putData(element.getObjectKey(), element);
        doPut(element);
    }
  }

  public final void stopTimeMonitoring() {
    if(map != null) {
      ((SpoolingTimeExpiryMap) map).stopTimeMonitoring();
    }
  }

  public final void evictExpiredElements() {
    ((SpoolingTimeExpiryMap) map).evictExpiredElements();
  }

  public final synchronized int getHitCount() {
    return ((SpoolingTimeExpiryMap) map).getHitCount();
  }

  public final synchronized int getMissCountExpired() {
    return ((SpoolingTimeExpiryMap) map).getMissCountExpired();
  }

  public final synchronized int getMissCountNotFound() {
    return ((SpoolingTimeExpiryMap) map).getMissCountNotFound();
  }

  public final synchronized boolean isExpired(final Object key) {
    return ((SpoolingTimeExpiryMap) map).isExpired(key);
  }

  public final synchronized void clearStatistics() {
    ((SpoolingTimeExpiryMap) map).clearStatistics();
  }
  
  public final class SpoolingTimeExpiryMap extends TimeExpiryMap {

    public SpoolingTimeExpiryMap(long timeToIdleSec, long maxIdleSec, long timeToLiveSec, String cacheName) {
      super(timeToIdleSec, maxIdleSec, timeToLiveSec, cacheName);
    }
    
// Notification are not supported yet
//    protected final synchronized void processExpired(Object key, Object value) {
//      // If cache is null, the cache has been disposed and the invalidator thread will be stopping soon.
//
//      if (cache == null) { return; }
//      // Already removed from the map at this point
//      Element element = (Element) value;
//      // When max size is 0
//      if (element == null) { return; }
//
//      // check for expiry before going to the trouble of spooling
//      if (element.isExpired()) {
//        notifyExpiry(element);
//      } else {
//        evict(element);
//      }
//    }

    public final void evictExpiredElements() {
      timeExpiryDataStore.evictExpiredElements();
    }
  }

}
