/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.stringification;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link PrettyPrintUtils}.
 */
public class PrettyPrintUtilsTest extends TCTestCase {

  public void testPluralize() throws Exception {
    assertEquals("foos", PrettyPrintUtils.pluralize("foo", -20));
    assertEquals("foos", PrettyPrintUtils.pluralize("foo", -1));
    assertEquals("foos", PrettyPrintUtils.pluralize("foo", 0));
    assertEquals("foo", PrettyPrintUtils.pluralize("foo", 1));
    assertEquals("foos", PrettyPrintUtils.pluralize("foo", 2));
    assertEquals("foos", PrettyPrintUtils.pluralize("foo", 20));

    assertEquals("grasses", PrettyPrintUtils.pluralize("grass", -20));
    assertEquals("grasses", PrettyPrintUtils.pluralize("grass", -1));
    assertEquals("grasses", PrettyPrintUtils.pluralize("grass", 0));
    assertEquals("grass", PrettyPrintUtils.pluralize("grass", 1));
    assertEquals("grasses", PrettyPrintUtils.pluralize("grass", 2));
    assertEquals("grasses", PrettyPrintUtils.pluralize("grass", 20));
  }

  public void testQuantity() throws Exception {
    assertEquals("-20 foos", PrettyPrintUtils.quantity("foo", -20));
    assertEquals("-1 foos", PrettyPrintUtils.quantity("foo", -1));
    assertEquals("0 foos", PrettyPrintUtils.quantity("foo", 0));
    assertEquals("1 foo", PrettyPrintUtils.quantity("foo", 1));
    assertEquals("2 foos", PrettyPrintUtils.quantity("foo", 2));
    assertEquals("20 foos", PrettyPrintUtils.quantity("foo", 20));

    assertEquals("-20 grasses", PrettyPrintUtils.quantity("grass", -20));
    assertEquals("-1 grasses", PrettyPrintUtils.quantity("grass", -1));
    assertEquals("0 grasses", PrettyPrintUtils.quantity("grass", 0));
    assertEquals("1 grass", PrettyPrintUtils.quantity("grass", 1));
    assertEquals("2 grasses", PrettyPrintUtils.quantity("grass", 2));
    assertEquals("20 grasses", PrettyPrintUtils.quantity("grass", 20));
  }

  public void testPercentage() throws Exception {
    assertEquals("0.0000000000%", PrettyPrintUtils.percentage(0.0, 10));
    assertEquals("0.00%", PrettyPrintUtils.percentage(0.0, 2));
    assertEquals("0.0%", PrettyPrintUtils.percentage(0.0, 1));
    assertEquals("0%", PrettyPrintUtils.percentage(0.0, 0));
    assertEquals("12.34%", PrettyPrintUtils.percentage(0.12341234, 2));
    assertEquals("12.35%", PrettyPrintUtils.percentage(0.12351234, 2));
    assertEquals("12.35%", PrettyPrintUtils.percentage(0.12349, 2));
    assertEquals("12.35%", PrettyPrintUtils.percentage(0.1235, 2));
    assertEquals("12%", PrettyPrintUtils.percentage(0.12345234, 0));
    assertEquals("13%", PrettyPrintUtils.percentage(0.126, 0));
    assertEquals("9%", PrettyPrintUtils.percentage(0.09, 0));
    assertEquals("9.02%", PrettyPrintUtils.percentage(0.0902, 2));
    assertEquals("-1%", PrettyPrintUtils.percentage(-0.01, 0));
    assertEquals("-1.01%", PrettyPrintUtils.percentage(-0.0101, 2));
    assertEquals("-1.48%", PrettyPrintUtils.percentage(-0.0148, 2));
    assertEquals("-1%", PrettyPrintUtils.percentage(-0.0148, 0));
    assertEquals("-1%", PrettyPrintUtils.percentage(-0.015, 0));
    assertEquals("-2%", PrettyPrintUtils.percentage(-0.016, 0));
    assertEquals("-2%", PrettyPrintUtils.percentage(-0.019, 0));
    assertEquals("-2%", PrettyPrintUtils.percentage(-0.025, 0));
  }

}
