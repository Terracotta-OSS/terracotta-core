/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.StatsListener;

import java.io.CharArrayWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class LongrunningGCTestApp implements Application {

  private final TCLogger       logger           = new TCLogger() {

                                                  PrintStream out = System.out;

                                                  DateFormat  df  = DateFormat.getDateTimeInstance();

                                                  private void println(String header, Object message) {
                                                    out.println(format(header + message));
                                                  }

                                                  private void println(String header, Object message, Throwable t) {
                                                    out.println(format(header + message, t));
                                                  }

                                                  private String format(Object message) {
                                                    return df.format(new java.util.Date()) + message;
                                                  }

                                                  private String format(Object message, Throwable t) {
                                                    CharArrayWriter chaw = new CharArrayWriter();
                                                    PrintWriter ps = new PrintWriter(chaw);
                                                    t.printStackTrace(ps);
                                                    ps.flush();

                                                    return format(message + chaw.toString());
                                                  }

                                                  public void debug(Object message) {
                                                    println(" DEBUG ", message);
                                                  }

                                                  public void debug(Object message, Throwable t) {
                                                    println(" DEBUG ", message, t);
                                                  }

                                                  public void error(Object message) {
                                                    println(" ERROR ", message);
                                                  }

                                                  public void error(Object message, Throwable t) {
                                                    println(" ERROR ", message, t);
                                                  }

                                                  public void fatal(Object message) {
                                                    println(" FATAL ", message);
                                                  }

                                                  public void fatal(Object message, Throwable t) {
                                                    println(" FATAL ", message, t);
                                                  }

                                                  public void info(Object message) {
                                                    println(" INFO ", message);
                                                  }

                                                  public void info(Object message, Throwable t) {
                                                    println(" INFO ", message, t);
                                                  }

                                                  public void warn(Object message) {
                                                    println(" WARN ", message);
                                                  }

                                                  public void warn(Object message, Throwable t) {
                                                    println(" WARN ", message, t);
                                                  }

                                                  public boolean isDebugEnabled() {
                                                    return true;
                                                  }

                                                  public boolean isInfoEnabled() {
                                                    return true;
                                                  }

                                                  public void setLevel(LogLevel level) {
                                                    return;
                                                  }

                                                  public LogLevel getLevel() {
                                                    return null;
                                                  }

                                                  public String getName() {
                                                    return getClass().getName();
                                                  }

                                                };

  private static final long    TEST_DURATION    = 1000 * 60 * 60 * 24 * 7;
  private final Date           endDate;

  private final String         appId;
  private final StatsListener  statsListener;
  private final List           list             = new ArrayList();
  private transient final List unmanaged        = new ArrayList();
  private transient final List agedUnmanaged    = new ArrayList();
  private long                 loopSleepTime    = 0;

  private final boolean        useUnmanaged     = true;
  private final boolean        useAgedUnmanaged = true;
  private final boolean        beRandom         = true;
  private final boolean        doComplex        = true;

  public LongrunningGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listeners) {
    this.appId = appId;
    Properties properties = new Properties();
    properties.setProperty("sample_name", "100 iterations");
    this.statsListener = listeners.newStatsListener(properties);
    if (cfg instanceof LongrunningGCTestAppConfig) {
      loopSleepTime = ((LongrunningGCTestAppConfig) cfg).getLoopSleepTime();
    }
    endDate = new Date(System.currentTimeMillis() + TEST_DURATION);
  }

  public String getApplicationId() {
    return appId;
  }

  public boolean interpretResult(Object result) {
    return true;
  }

  public void run() {
    println("starting " + getClass());
    println("Will run until " + endDate);
    if (doComplex) doComplex();
    else doSimple();
    println("All done.");
  }

  private boolean shouldContinue() {
    return System.currentTimeMillis() < endDate.getTime();
  }

  private void doSimple() {
    println("doSimple...");
    int count = 0;
    NumberFormat nf = NumberFormat.getInstance();
    long t0 = System.currentTimeMillis();
    while (shouldContinue()) {

      if (count % 200 == 0) {
        synchronized (list) {
          println("clearing list (" + list.size() + ")");
          list.clear();
        }
      }

      Tree tree = new Tree();
      tree.makeTree(2, 2);
      synchronized (list) {
        if (count % 50 == 0) {
          println("adding to list (" + list.size() + ")");
        }
        list.add(tree);
      }

      if (count % 100 == 0) {
        long delta = System.currentTimeMillis() - t0;
        println("count=" + nf.format(count) + ", time=" + nf.format(delta) + " ms.");
        t0 = System.currentTimeMillis();
      }

      count++;
    }
  }

  private void doComplex() {
    println("doComplex...");
    println("useUnmanaged    : " + useUnmanaged);
    println("useAgedUnmanaged: " + useAgedUnmanaged);
    println("beRandom        : " + beRandom);
    println("loopSleepTime   : " + loopSleepTime);
    NumberFormat nf = NumberFormat.getInstance();
    boolean migratedAgedUnmanaged = false;
    boolean clearedUnmanagedAndList = false;
    boolean addedUnmanagedToList = false;
    Random random = new Random(System.currentTimeMillis() + System.identityHashCode(new Object()));
    int count = 0;
    long t0 = System.currentTimeMillis();
    while (shouldContinue()) {
      if (count % 100 == 0) {
        long delta = System.currentTimeMillis() - t0;
        println("count=" + nf.format(count) + ", time=" + nf.format(delta) + " ms.");
        this.statsListener.sample(delta, "");
        println("migratedAgedUnmanaged  : " + migratedAgedUnmanaged);
        println("clearedUnmanagedAndList: " + clearedUnmanagedAndList);
        println("addedUnmanagedToList   : " + addedUnmanagedToList);

        t0 = System.currentTimeMillis();
        migratedAgedUnmanaged = false;
        clearedUnmanagedAndList = false;
        addedUnmanagedToList = false;
      }

      if (useAgedUnmanaged && count % random(random, 1001) == 0) {
        // keep a stash of objects around for a little longer to be added back to
        // the managed list later.
        synchronized (agedUnmanaged) {
          synchronized (list) {
            println("adding agedUnmanaged (" + agedUnmanaged.size() + ") to list (" + list.size() + ")...");
            list.addAll(agedUnmanaged);
          }
          println("Clearing agedUnmanaged: " + agedUnmanaged.size());
          agedUnmanaged.clear();
          synchronized (list) {
            if (list.size() > 0) {
              println("adding list element 0 to agedUnmanaged...");
              agedUnmanaged.add(list.get(0));
            }
          }
        }
        migratedAgedUnmanaged = true;
      }

      if (count % random(random, 500) == 0) {
        synchronized (list) {
          println("Clearing unmanaged: " + unmanaged.size());
          unmanaged.clear();
          println("Clearing managed: " + list.size());
          list.clear();
        }
        clearedUnmanagedAndList = true;
      }
      // move the items from the unmanaged set to the managed list to make sure that
      // managed objects referenced from an unmanaged graph don't get garbage collected.
      if (useUnmanaged && count % random(random, 250) == 0) {
        synchronized (list) {
          println("Adding elements from unmanaged (" + unmanaged.size() + ") to managed list (" + list.size() + ")...");
          list.addAll(unmanaged);
        }
        addedUnmanagedToList = true;
      }

      Tree tree = new Tree();
      tree.makeTree(2, 2);

      synchronized (list) {
        list.add(tree);
        if (useUnmanaged) {
          unmanaged.add(tree);
        }
      }
      count++;

      sleep(loopSleepTime);
    }
  }

  public int random(Random random, int max) {
    if (!beRandom) return max / 2;
    int rv;
    while ((rv = random.nextInt(max)) == 0) {
      //
    }
    return rv;
  }

  private void println(Object o) {
    logger.info("appId[" + appId + "]: " + o);
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = LongrunningGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
    spec.addRoot("list", testClassName + ".list");
    String methodExpression = "* " + testClassName + ".*(..)";
    System.err.println("Adding autolock for: " + methodExpression);
    config.addWriteAutolock(methodExpression);

    spec.addTransient("unmanaged");
    spec.addTransient("agedUnmanaged");

    config.addIncludePattern(Tree.class.getName());
    config.addExcludePattern(LongrunningGCTestAppConfig.class.getName());
    config.addExcludePattern(LongrunningGCTestAppConfigObject.class.getName());
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String classname = LongrunningGCTestApp.class.getName();
    config.addRoot("LongrunningGCTestAppList", classname + ".list");
    config.addWriteAutolock("* " + classname + ".*(..)");

    config.addIncludePattern(LongrunningGCTestApp.class.getName(), true);
    config.addIncludePattern(Tree.class.getName());
  }

}
