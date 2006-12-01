/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.exception.ImplementMe;
import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class OptionsTest extends TestCase {
  Map      defaults = new HashMap();
  Logger   logger   = new Logger();
  String[] keys     = new String[] { "key1", "key2", "key3" };
  String   input;
  Options  opts;

  protected void setUp() throws Exception {
    super.setUp();

    defaults.put("key1", Boolean.TRUE);
    defaults.put("key2", Boolean.TRUE);
    defaults.put("key3", Boolean.FALSE);
  }

  public void testExceptions() {
    try {
      new Options("", new String[] { "key1", "ALL" }, logger, defaults);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      new Options("", new String[] { "NONE" }, logger, defaults);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    defaults.clear();
    defaults.put("key1", "not an instance of java.lang.Boolean");

    try {
      new Options("", keys, logger, defaults);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void test() {
    input = "key1";
    opts = new Options(input, keys, logger, null);
    assertTrue(opts.getOption("key1"));
    assertFalse(opts.getOption("key2"));
    assertFalse(opts.getOption("key3"));
    assertEquals(0, logger.warnCount);

    String[] empty = new String[] { null, "", " ", " \t", "\t ", "\t", " ", "        \t ", " ,\t" };
    for (int i = 0; i < empty.length; i++) {
      input = empty[i];
      opts = new Options(input, keys, logger, null);
      assertFalse(opts.getOption("key1"));
      assertFalse(opts.getOption("key2"));
      assertFalse(opts.getOption("key3"));
      assertEquals(0, logger.warnCount);
    }

    input = "this-aint-a-valid-option, key2";
    opts = new Options(input, keys, logger, null);
    assertEquals(1, logger.warnCount);
    assertFalse(opts.getOption("key1"));
    assertTrue(opts.getOption("key2"));
    assertFalse(opts.getOption("key3"));

    String[] all = new String[] { "ALL", "toString, ALL", "ALL, not-a-valid-option" };
    for (int i = 0; i < all.length; i++) {
      input = all[i];
      opts = new Options(input, keys, logger, null);
      assertTrue(opts.getOption("key1"));
      assertTrue(opts.getOption("key2"));
      assertTrue(opts.getOption("key3"));
    }
  }

  public void testNone() {
    input = "NONE, key2";
    opts = new Options(input, keys, logger, defaults);
    assertEquals(0, logger.warnCount);
    assertFalse(opts.getOption("key1"));
    assertTrue(opts.getOption("key2"));
    assertFalse(opts.getOption("key3"));

    input = "key1, key2,key3, NONE";
    opts = new Options(input, keys, logger, defaults);
    assertEquals(0, logger.warnCount);
    assertFalse(opts.getOption("key1"));
    assertFalse(opts.getOption("key2"));
    assertFalse(opts.getOption("key3"));
  }

  public void testNegation() {
    input = "key1, -key2";
    opts = new Options(input, keys, logger, defaults);
    assertEquals(0, logger.warnCount);
    assertTrue(opts.getOption("key1"));
    assertFalse(opts.getOption("key2")); // default is true, but we negated it in the the input
    assertFalse(opts.getOption("key3"));
  }

  public void testAllPlusNegation() {
    input = "ALL, -key2";
    opts = new Options(input, keys, logger, defaults);
    assertEquals(0, logger.warnCount);
    assertTrue(opts.getOption("key1"));
    assertFalse(opts.getOption("key2"));
    assertTrue(opts.getOption("key3"));
  }

  public void testDefaults() {
    opts = new Options(null, keys, logger, defaults);
    assertEquals(0, logger.warnCount);
    assertTrue(opts.getOption("key1"));
    assertTrue(opts.getOption("key2"));
    assertFalse(opts.getOption("key3"));
  }

  private static class Logger implements TCLogger {

    int warnCount;

    public void debug(Object message) {
      throw new ImplementMe();
    }

    public void debug(Object message, Throwable t) {
      throw new ImplementMe();
    }

    public void error(Object message) {
      throw new ImplementMe();
    }

    public void error(Object message, Throwable t) {
      throw new ImplementMe();
    }

    public void fatal(Object message) {
      throw new ImplementMe();
    }

    public void fatal(Object message, Throwable t) {
      throw new ImplementMe();
    }

    public void info(Object message) {
      throw new ImplementMe();
    }

    public void info(Object message, Throwable t) {
      throw new ImplementMe();
    }

    public void warn(Object message) {
      this.warnCount++;
    }

    public void warn(Object message, Throwable t) {
      throw new ImplementMe();
    }

    public void log(LogLevel level, Object message) {
      throw new ImplementMe();
    }

    public void log(LogLevel level, Object message, Throwable t) {
      throw new ImplementMe();
    }

    public boolean isDebugEnabled() {
      return false;
    }

    public boolean isInfoEnabled() {
      return false;
    }

    public void setLevel(LogLevel level) {
      throw new ImplementMe();
    }

    public LogLevel getLevel() {
      throw new ImplementMe();
    }
  }
}
