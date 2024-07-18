/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

import java.util.Iterator;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class SimpleRingBufferTest {
  
  public SimpleRingBufferTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of put method, of class SimpleRingBuffer.
   */
  @Test
  public void testFullPut() {
    SimpleRingBuffer<Integer> ring = new SimpleRingBuffer<>(2500);
    for (int x=0;x<5000;x++) {
      ring.put(x);
    }
    ring.stream().forEach(System.out::println);
  }

  /**
   */
  @Test
   public void testShortPut() {
    SimpleRingBuffer<Integer> ring = new SimpleRingBuffer<>(2500);
    for (int x=0;x<1000;x++) {
      ring.put(x);
    }
    ring.stream().forEach(System.out::println);
  } 
}
