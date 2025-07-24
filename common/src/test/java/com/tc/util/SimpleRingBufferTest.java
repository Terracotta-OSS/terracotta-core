/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
   * Test with partial buffer fill
   */
  @Test
  public void testShortPut() {
    SimpleRingBuffer<Integer> ring = new SimpleRingBuffer<>(2500);
    for (int x=0;x<1000;x++) {
      ring.put(x);
    }
    ring.stream().forEach(System.out::println);
  }
  
  /**
   * Test empty buffer iteration
   */
  @Test
  public void testEmptyBuffer() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(5);
    int count = 0;
    for (String s : buffer) {
      count++;
    }
    assertEquals("Empty buffer should have no elements", 0, count);
  }
  
  /**
   * Test partially filled buffer iteration
   */
  @Test
  public void testPartiallyFilledBuffer() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(5);
    buffer.put("A");
    buffer.put("B");
    buffer.put("C");
    
    // Buffer has 3 elements, not wrapped around
    StringBuilder result = new StringBuilder();
    for (String s : buffer) {
      result.append(s);
    }
    
    assertEquals("Partially filled buffer should iterate in insertion order", "ABC", result.toString());
  }
  
  /**
   * Test full buffer without wraparound
   */
  @Test
  public void testFullBufferNoWraparound() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
    buffer.put("A");
    buffer.put("B");
    buffer.put("C");
    
    // Buffer is full but hasn't wrapped around
    StringBuilder result = new StringBuilder();
    for (String s : buffer) {
      result.append(s);
    }
    
    assertEquals("Full buffer should iterate in insertion order", "ABC", result.toString());
  }
  
  /**
   * Test iteration order with wraparound
   */
  @Test
  public void testWraparoundIteration() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
    buffer.put("A");
    buffer.put("B");
    buffer.put("C");
    buffer.put("D"); // Overwrites A
    
    // Expected order: B, C, D (oldest to newest)
    StringBuilder result = new StringBuilder();
    for (String s : buffer) {
      result.append(s);
    }
    
    assertEquals("Iteration should be in order from oldest to newest", "BCD", result.toString());
  }
  
  /**
   * Test multiple wraparounds
   */
  @Test
  public void testMultipleWraparounds() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
    buffer.put("A");
    buffer.put("B");
    buffer.put("C");
    buffer.put("D"); // Overwrites A
    buffer.put("E"); // Overwrites B
    buffer.put("F"); // Overwrites C
    
    // Expected order: D, E, F (oldest to newest)
    StringBuilder result = new StringBuilder();
    for (String s : buffer) {
      result.append(s);
    }
    
    assertEquals("Iteration after multiple wraparounds", "DEF", result.toString());
  }
  
  /**
   * Test null rejection
   */
  @Test(expected = NullPointerException.class)
  public void testNullRejection() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
    buffer.put(null); // Should throw NullPointerException
  }
  
  /**
   * Test buffer with exactly one element
   */
  @Test
  public void testSingleElement() {
    SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(1);
    buffer.put("A");
    buffer.put("B"); // Overwrites A
    
    StringBuilder result = new StringBuilder();
    for (String s : buffer) {
      result.append(s);
    }
    
    assertEquals("Single element buffer should contain only the newest element", "B", result.toString());
  }
}
