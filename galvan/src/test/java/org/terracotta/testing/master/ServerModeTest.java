/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mscott2
 */
public class ServerModeTest {

  public ServerModeTest() {
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
   * Test of values method, of class ServerMode.
   */
  @Test
  public void testSort() {
    System.out.println("values");
    List<ServerMode> modes = new ArrayList<>(EnumSet.allOf(ServerMode.class));
    Collections.reverse(modes);
    modes.stream().sorted((a,b)->{
      int comp = a.ordinal()-b.ordinal();
      System.out.println(a + ":" + a.ordinal() + " " + b + ":" + b.ordinal());
      return comp;
    }).forEach(System.out::println);
  }

}
