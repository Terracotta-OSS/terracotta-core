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
package com.tc.object;

import java.io.PrintWriter;
import java.io.StringWriter;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.exception.ConnectionClosedException;

/**
 *
 */
public class ExceptionUtilsTest {

  public ExceptionUtilsTest() {
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
   * Test of convert method, of class ExceptionUtils.
   */
  @Test
  public void testConvertRuntime() throws Exception {
    System.out.println("convert");
    RuntimeException exception = new RuntimeException();
    try {
      ExceptionUtils.throwEntityException(exception);
      throw new AssertionError();
    } catch (Exception t) {
      try (StringWriter b = new StringWriter();
      PrintWriter w = new PrintWriter(b)) {
        t.printStackTrace(w);
        w.flush();
        assertThat(b.toString(), containsString(""));
      }
    }
  }

  @Test
  public void testConvertConnectionClosed() throws Exception {
    System.out.println("convert");
    ConnectionClosedException exception = new ConnectionClosedException(false, "message not sent");
    try {
      ExceptionUtils.throwEntityException(exception);
      throw new AssertionError();
    } catch (Exception t) {
      try (StringWriter b = new StringWriter();
      PrintWriter w = new PrintWriter(b)) {
        t.printStackTrace(w);
        w.flush();
        assertThat(b.toString(), not(containsString("ExceptionUtils.")));
      }
    }
  }
}
