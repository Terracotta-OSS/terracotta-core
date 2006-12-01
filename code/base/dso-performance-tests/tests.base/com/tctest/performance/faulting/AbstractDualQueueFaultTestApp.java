/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.faulting;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.TempDirectoryHelper;
import com.tctest.performance.generate.load.LinearTransitionLoadGenerator;
import com.tctest.performance.generate.load.LoadGenerator;
import com.tctest.performance.generate.load.Measurement;
import com.tctest.performance.generate.load.Metronome;
import com.tctest.performance.generate.load.WorkQueueOverflowException;
import com.tctest.performance.results.PerformanceMeasurementMarshaller;
import com.tctest.performance.simulate.type.SimulatedType;
import com.tctest.performance.simulate.type.SimulatedTypeFactory;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractDualQueueFaultTestApp extends AbstractTransparentApp {

  private final List          results1;
  private final List          results2;
  private final List          sharedResults1;
  private final List          sharedResults2;
  private Metronome[]         compiledResults1;
  private Metronome[]         compiledResults2;
  private Measurement[]       workQueueWaitTimes1;
  private Measurement[]       workQueueWaitTimes2;
  private static final int    DURATION       = 60 * 10;
  private static final int    INIT_LOAD      = 500;
  private static final int    MAX_LOAD       = 2000;
  private static final int    PERCENT_UNIQUE = 100;
  private final CyclicBarrier barrier;
  private boolean             isReader;
  private boolean             isLocalWriter;
  private volatile boolean    escape;

  public AbstractDualQueueFaultTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    results1 = new LinkedList();
    results2 = new LinkedList();
    barrier = new CyclicBarrier(getParticipantCount());
    sharedResults1 = new ArrayList();
    sharedResults2 = new ArrayList();
  }

  protected static TransparencyClassSpec visitConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    String className = AbstractDualQueueFaultTestApp.class.getName();
    spec = config.getOrCreateSpec(className);

    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    spec.addRoot("isReader", "isReader");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("sharedResults1", "sharedResults1");
    spec.addRoot("sharedResults2", "sharedResults2");

    return spec;
  }

  protected static void visitConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(CyclicBarrier.class.getName());
    String className = AbstractDualQueueFaultTestApp.class.getName();
    config.addIncludePattern(className);

    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    config.addRoot("isReader", className + ".isReader");
    config.addRoot("barrier", className + ".barrier");
    config.addRoot("sharedResults1", className + ".sharedResults1");
    config.addRoot("sharedResults2", className + ".sharedResults2");
  }

  // Abstraction is not worth the effort

  public void run() {
    synchronized (barrier) {
      if (!isReader) {
        isReader = true;
        isLocalWriter = true;
      }
    }
    try {
      barrier.await();

      if (isLocalWriter) doWriter();
      else doReader();

      barrier.await();

      System.err.println("DURATION COMPLETE");
      System.err.println("Compliling Shared Results");

      synchronized (sharedResults1) {
        sharedResults1.addAll(results1);
      }
      synchronized (sharedResults2) {
        sharedResults2.addAll(results2);
      }

      barrier.await();

    } catch (Throwable t) {
      notifyError(t);
    }

    if (isLocalWriter) {
      synchronized (sharedResults1) {
        compiledResults1 = (Metronome[]) sharedResults1.toArray(new Metronome[0]);
        Arrays.sort(sharedResults1.toArray(compiledResults1));
      }
      synchronized (sharedResults2) {
        compiledResults2 = (Metronome[]) sharedResults2.toArray(new Metronome[0]);
        Arrays.sort(sharedResults2.toArray(compiledResults2));
      }
      writeData();
    }
  }

  private void doWriter() throws Throwable {
    Thread producer1 = new Thread() {
      public void run() {
        SimulatedType sInt = SimulatedTypeFactory.create(new Integer(0));
        LoadGenerator loadGenerator = new LinearTransitionLoadGenerator();
        loadGenerator.start(DURATION, INIT_LOAD, MAX_LOAD, sInt, PERCENT_UNIQUE);
        try {
          while (!escape) {
            Object obj = loadGenerator.getNext();
            if (obj == null) {
              workQueueWaitTimes1 = loadGenerator.getWaitTimes();
              break; // work complete
            }
            populate1(obj);
          }
        } catch (WorkQueueOverflowException e) {
          System.err.println("UPPER BOUND REACHED");
          workQueueWaitTimes1 = loadGenerator.getWaitTimes();
          escape = true;
          return;

        } catch (InterruptedException e) {
          throw new RuntimeException(e); // unexpected
        }
      }
    };
    producer1.setDaemon(true);

    Thread producer2 = new Thread() {
      public void run() {
        SimulatedType sInt = SimulatedTypeFactory.create(new Integer(0));
        LoadGenerator loadGenerator = new LinearTransitionLoadGenerator();
        loadGenerator.start(DURATION, INIT_LOAD, MAX_LOAD, sInt, PERCENT_UNIQUE);
        try {
          while (!escape) {
            Object obj = loadGenerator.getNext();
            if (obj == null) {
              workQueueWaitTimes2 = loadGenerator.getWaitTimes();
              break; // work complete
            }
            populate2(obj);
          }
        } catch (WorkQueueOverflowException e) {
          System.err.println("UPPER BOUND REACHED");
          workQueueWaitTimes2 = loadGenerator.getWaitTimes();
          escape = true;
          return;

        } catch (InterruptedException e) {
          throw new RuntimeException(e); // unexpected
        }
      }
    };
    producer2.setDaemon(true);

    producer1.start();
    producer2.start();
    System.err.println("LOAD STARTED");

    // readers await at consumer.start()
    barrier.await();

    try {
      // wait for producers to complete
      producer1.join();
      producer2.join();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void doReader() throws Throwable {
    Thread consumer1 = new Thread() {
      public void run() {
        try {
          while (true) {
            retrieve1();
          }
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    };
    consumer1.setDaemon(true);

    Thread consumer2 = new Thread() {
      public void run() {
        try {
          while (true) {
            retrieve2();
          }
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    };
    consumer2.setDaemon(true);

    // writer awaits at producer.start()
    barrier.await();
    consumer1.start();
    consumer2.start();
  }

  protected abstract void populate1(Object data) throws InterruptedException;

  protected abstract void populate2(Object data) throws InterruptedException;

  protected abstract void retrieve1() throws InterruptedException;

  protected abstract void retrieve2() throws InterruptedException;

  protected abstract String title();

  protected List results1() {
    return results1;
  }

  protected List results2() {
    return results2;
  }

  protected List generateStatistics() {
    List measurementList = new ArrayList();
    Metronome data;

    Measurement[] stats1 = new Measurement[compiledResults1.length];
    for (int i = 0; i < stats1.length; i++) {
      data = compiledResults1[i];
      stats1[i] = new Measurement(data.load, data.endtime - data.starttime);
    }
    Measurement[] stats2 = new Measurement[compiledResults2.length];
    for (int i = 0; i < stats2.length; i++) {
      data = compiledResults2[i];
      stats2[i] = new Measurement(data.load, data.endtime - data.starttime);
    }

    measurementList.add(workQueueWaitTimes1);
    measurementList.add(workQueueWaitTimes2);
    measurementList.add(stats1);
    measurementList.add(stats2);

    return measurementList;
  }

  protected void writeData() {
    try {
      TempDirectoryHelper helper = new TempDirectoryHelper(getClass());
      File dataDir = helper.getDirectory();
      File output = new File(dataDir + File.separator + "results.data");
      output.createNewFile();
      System.err.println("WROTE RESULT DATA TO: " + output);

      PerformanceMeasurementMarshaller.Header header = PerformanceMeasurementMarshaller.createHeader();
      header.title = title();
      header.xLabel = INIT_LOAD + " to " + MAX_LOAD + " Objects/sec.";
      header.yLabel = "Time spent in queue (Milliseconds)";
      header.duration = DURATION;

      String[] lineDescriptions = new String[] { "Work Queue1 Wait Time", "Work Queue2 Wait Time",
          "Time Spent in Shared Queue - duration: " + header.duration + " sec. | " + header.xLabel };

      PerformanceMeasurementMarshaller.marshall(generateStatistics(), header, output, lineDescriptions);

    } catch (Throwable t) {
      notifyError(t);
    }
  }
}
