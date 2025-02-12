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
