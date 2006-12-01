/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.util.Set;

import junit.framework.TestCase;

/**
 * Test case for Type
 */
public class TypeTest extends TestCase {

  private Type type;

  protected void setUp() throws Exception {
    super.setUp();
    this.type = new Type();
  }

  public void testGetSetName() {
    assertTrue(type.getName() == null);
    type.setName("MyClassName");
    assertNotNull(type.getName());
    
    try {
      type.setName(null);
      fail("Should have thrown an exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
    
    try {
      type.setName("");
      fail("Should have thrown an exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testAddTransient() {
    assertNotNull(type.getTransients());
    assertEquals(0, type.getTransients().size());
    String transientName = "transient1";
    type.addTransient(transientName);
    assertEquals(1, type.getTransients().size());
    assertTrue(type.getTransients().contains(transientName));
    
    type.addTransient(transientName);
    assertEquals(1, type.getTransients().size());
    
    try {
      type.addTransient(null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    
    try {
      type.addTransient("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    
    try {
      type.addTransient("   ");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testAddTransients() {
    String[] transients = new String[] { "test1", "test2" };
    
    assertEquals(0, type.getTransients().size());
    type.addTransients(transients);
    assertEquals(transients.length, type.getTransients().size());
    for (int i=0; i<transients.length; i++) {
      assertTrue(type.getTransients().contains(transients[i]));
    }
  }

  public void testGetTransients() {
    type.addTransient("test");
    Set s1 = type.getTransients();
    type.addTransient("test1");
    Set s2 = type.getTransients();
    
    assertEquals(1, s1.size());
    assertEquals(2, s2.size());
  }

  public void testCommit() {
    // once the type has been committed, you shouldn't be able to alter it.
    type.setName("test");
    type.addTransient("test1");
    type.commit();
    try {
      type.setName("test");
      fail("Should throw an exception");
    } catch (IllegalStateException e) {
      // expected
    }
    
    try {
      type.addTransient("test1");
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      // expected
    }
    
    try {
      type.addTransients(new String[] {"test"});
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      // expected
    }
  }
  
  public void testValidate() {
    type.setName("Test");
    type.validate();

    type = new Type();
    try {
      type.validate();
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

}
