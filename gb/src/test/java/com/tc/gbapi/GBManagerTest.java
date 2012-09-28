package com.tc.gbapi;

import org.junit.Before;
import org.junit.Ignore;
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

  @Ignore
  @Test(expected = IllegalStateException.class)
  public void testCantAttachMapIfNotStarted() {
    manager.attachMap("whatever!", new GBOnHeapMapImpl<Object, Object>());
  }

  @Ignore
  @Test(expected = IllegalStateException.class)
  public void testCantAccessMapIfNotStarted() {
    manager.getMap("whatever!", Object.class, Object.class);
  }
}
