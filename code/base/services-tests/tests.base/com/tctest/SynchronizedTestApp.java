/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class SynchronizedTestApp extends AbstractTransparentApp 
{
  private final MapContainer mapContainer = new MapContainer();
  private Object lock = new Object();
  
  public SynchronizedTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) 
  {
    super(appId, cfg, listenerProvider);
  }
  
  private static class MapContainer
  {
    private Map map = new HashMap();
    
    public synchronized void put(Object key, Object value)
    {
      putItem(key, value);
    }
    
    private void putItem(Object key, Object value)
    {
      map.put(key, value);
    }
    public synchronized Object get(Object key)
    {
      return getItem(key);
    }
    
    private Object getItem(Object key)
    {
      return map.get(key);
    }
    
    public void unsafePut(Object key, Object value)
    {
      map.put(key, value);
    }
  }
  
  public void run() 
  {        
    testUnsynchronizedMethod();
    testLocalSynchronized();
  }
  
  private void testUnsynchronizedMethod()
  {
    Object key = new Object();
    Object value = new Object();
    
    mapContainer.put(key, value);
    if (mapContainer.get(key) != value) {
      notifyError("Wrong Value!");
    }
  }
  
  private void testLocalSynchronized()
  {
    // test to see if we can get a local lock inside a tc lock and make sure
    // it doesn't get confused
    synchronized (mapContainer) {
      synchronized (lock) {
        mapContainer.unsafePut(new Object(), new Object());
      }
    }
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) 
  {
    String testClass = SynchronizedTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");
   
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("mapContainer", "mapContainer");
  }
}
