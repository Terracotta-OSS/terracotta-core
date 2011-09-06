/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CyclicBarrier;

public class CalendarTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final BaseTime              base = new BaseTime();
    private final Map<String, Calendar> root = new HashMap<String, Calendar>();
    private final CyclicBarrier         barrier;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {
      int index = barrier.await();

      if (index == 0) {
        synchronized (barrier) {
          root.put("default", getDefault(base.time));
          root.put("buddhist", getBuddhist(base.time));
          root.put("zoneinfo", getCustomTimeZone(base.time));
          root.put("uncompleted", getUncompleted());
        }
      }

      barrier.await();

      if (index != 0) {
        Assert.assertEquals(getDefault(base.time), root.get("default"));
        Assert.assertEquals(getBuddhist(base.time), root.get("buddhist"));
        Assert.assertEquals(getCustomTimeZone(base.time), root.get("zoneinfo"));
      }

      barrier.await();

      if (index == 0) {
        synchronized (root) {
          root.put("default", mutate(root.get("default")));
          root.put("buddhist", mutate(root.get("buddhist")));
          root.put("zoneinfo", mutate(root.get("zoneinfo")));
        }
      }

      barrier.await();

      if (index != 0) {
        Assert.assertEquals(mutate(getDefault(base.time)), root.get("default"));
        Assert.assertEquals(mutate(getBuddhist(base.time)), root.get("buddhist"));
        Assert.assertEquals(mutate(getCustomTimeZone(base.time)), root.get("zoneinfo"));
      }
      
      /*******************************************************************************
       *  UNCOMMENT TO REPRODUCE CDV 606

      barrier.await();
      
      // JIRA issues CDV-606 UnlockedSharedObjectException
      // It should be safe to call unguarded read-only methods on safely published clustered object, as long
      // as no threads modify it after publication.  But get() method can modify internal state of Calendar instance
      // if .complete() hasn't been called on that instance...
      if (index != 0) {
        Calendar cal =  root.get("uncompleted");
        cal.get(Calendar.YEAR);
      }
      
      ********************************************************************************/
    }

    private static Calendar mutate(Calendar calendar) {
      synchronized (calendar) {
        calendar.set(2003, 12, 11, 9, 15, 43);
        calendar.get(Calendar.YEAR);//cause .complete() to be called internally
      }
      return calendar;
    }

    private static Calendar getCustomTimeZone(long time) {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
      cal.setTimeInMillis(time);
      TimeZone timeZone = cal.getTimeZone();
      Assert.eval(timeZone.getClass().getName(), timeZone.getClass().getName().equals("sun.util.calendar.ZoneInfo"));
      return cal;
    }

    private static Calendar getBuddhist(long time) {
      Calendar cal = Calendar.getInstance(new Locale("th", "TH"));
      cal.setTimeInMillis(time);
      Assert.eval(cal.getClass().getName(), cal.getClass().getName().equals("sun.util.BuddhistCalendar"));
      return cal;
    }

    private static Calendar getDefault(long time) {
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(time);
      Assert.eval(cal.getClass().getName(), cal instanceof GregorianCalendar);
      return cal;
    }
    
    private static Calendar getUncompleted(){
      Calendar cal = Calendar.getInstance();
      Assert.eval(cal.getClass().getName(), cal instanceof GregorianCalendar);
      return cal;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("root", "root");
      spec.addRoot("base", "base");
      spec.addRoot("barrier", "barrier");

      String expression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(expression);

      config.addIncludePattern(BaseTime.class.getName());
    }

  }

  private static class BaseTime {
    private final long time = System.currentTimeMillis();
  }

}
