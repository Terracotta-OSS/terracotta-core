/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.Map;

/**
 * An app that throws an exception in a lock method and makes sure things still work ok
 */
public class TransparencyExceptionTestApp extends ClientBase {
  private final Map myRoot;
  private boolean   fail = true;

  public TransparencyExceptionTestApp(String[] args) {
    super(args);
    this.myRoot = getClusteringToolkit().getMap("my map", null, null);
  }

  public static void main(String[] args) {
    new TransparencyExceptionTestApp(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    test();
    fail = false;
    test();
  }

  public void test() {
    try {
      test1();
    } catch (AssertionError e) {
      if (fail) {
        System.out.println("SUCCESS");
      } else {
        throw new AssertionError("Failed !!");
      }
      return;
    }
    if (fail) {
      throw new AssertionError("Failed !!");
    } else {
      System.out.println("SUCCESS");
    }
  }

  public void test1() {
    myRoot.put(Long.valueOf(1), Long.valueOf(1));
    if (fail) throw new AssertionError("Testing one two three");
  }

}
