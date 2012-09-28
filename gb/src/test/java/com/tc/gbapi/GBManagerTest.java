package com.tc.gbapi;

import org.junit.Before;
import org.junit.Test;

import com.tc.gbapi.impl.GBOnHeapMapImpl;

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
}
