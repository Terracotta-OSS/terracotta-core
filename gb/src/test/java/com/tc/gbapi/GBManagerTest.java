package com.tc.gbapi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.tc.gbapi.impl.GBOnHeapMapImpl;

import java.util.concurrent.ExecutionException;

/**
 * @author Alex Snaps
 */
public class GBManagerTest {

  private GBManager manager;

  @Before
  public void setup() {
    manager = new GBManager(null, null);
  }

  @Test(expected = IllegalStateException.class)
  public void testCantAttachMapIfNotStarted() {
    manager.attachMap("whatever!", new GBOnHeapMapImpl<Object, Object>());
  }

  @Test(expected = IllegalStateException.class)
  public void testCantAccessMapIfNotStarted() {
    manager.getMap("whatever!", Object.class, Object.class);
  }

  @Test
  public void testReturnNullWhenNotAttached() throws ExecutionException, InterruptedException {
    manager.start().get();
    manager.getMap("wahtever!", Object.class, Object.class);
  }
}
