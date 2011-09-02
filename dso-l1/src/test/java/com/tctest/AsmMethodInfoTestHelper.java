/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author steve
 */
public class AsmMethodInfoTestHelper {
  AsmMethodInfoTestHelper(int countStart) {
    this.count = countStart;
    try {
      myRoot = new HashMap();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Map myRoot;     // = new HashMap();
  private long       count;

  @SuppressWarnings("unused")
  private int        commits = 0;

  public long test4(int i, Object foo) {
    synchronized (myRoot) {
      long start = System.currentTimeMillis();
      commits++;
      int s = myRoot.size();
      long c = count++;
      if (myRoot.containsKey(new Long(c))) {
        Assert.eval(false);
      }
      myRoot.put(new Long(c), new TestObj(new TestObj(null)));
      if (myRoot.size() != s + 1) System.out.println("Wrong size!:" + s + " new size:" + myRoot.size());
      Assert.eval(myRoot.size() == s + 1);
      // System.out.println("^^^TOTAL SIZE ADD:" + myRoot.size() + "^^^:" + this);
      return System.currentTimeMillis() - start;
    }
  }

  public long test5(int i, Object foo) {
    synchronized (myRoot) {
      long start = System.currentTimeMillis();
      commits++;
      int s = myRoot.size();
      myRoot.remove(new Long(count - 1));
      if (myRoot.size() != s - 1) System.out.println("Wrong size!:" + s + " new size:" + myRoot.size());
      Assert.eval(myRoot.size() == s - 1);
      // System.out.println("^^^TOTAL SIZE REMOVE:" + myRoot.size() + "^^^:" + this);
      return System.currentTimeMillis() - start;
    }
  }

  public static class TestObj {
    private TestObj       obj;
    private final String  string  = "Steve";
    private final int     integer = 22;
    private final boolean bool    = false;
    private final Map     map     = new HashMap();

    private TestObj() {
      //
    }

    public TestObj(TestObj obj) {
      this.obj = obj;
      for (int i = 0; i < 30; i++) {
        map.put(new Long(i), new TestObj());
      }
    }

    public Object getObject() {
      return this.obj;
    }

    public boolean check() {
      return string.equals("Steve") && Integer.valueOf(integer).equals(Integer.valueOf(22))
             && Boolean.valueOf(bool) == Boolean.FALSE;
    }
  }
}
