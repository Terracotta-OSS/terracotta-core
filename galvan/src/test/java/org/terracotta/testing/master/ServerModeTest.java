/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
