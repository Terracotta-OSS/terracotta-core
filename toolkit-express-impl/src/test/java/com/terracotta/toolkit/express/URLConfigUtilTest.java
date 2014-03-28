package com.terracotta.toolkit.express;

import junit.framework.TestCase;

/**
 * @author Alex Snaps
 */
public class URLConfigUtilTest extends TestCase {

  public void testParsesUsername() {
    assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896"));
    assertEquals("alex", URLConfigUtil.getUsername("  alex@localhost:896"));
    assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896,alex@localhost:87645"));
    assertEquals("alex", URLConfigUtil.getUsername(" alex@localhost:896,  alex@localhost:87645"));
    assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896,localhost:87645"));
    assertEquals("alex", URLConfigUtil.getUsername("localhost:896,alex@localhost:87645"));
    try {
      assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896,john@localhost:87645"));
      fail();
    } catch (AssertionError e) {
      // Expected
    }
  }
}
