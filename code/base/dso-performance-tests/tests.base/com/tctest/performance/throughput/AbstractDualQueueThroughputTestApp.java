/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.throughput;

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
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractDualQueueThroughputTestApp extends AbstractTransparentApp {

  private final List       results1;
  private final List       results2;
  private Measurement[]    workQueueWaitTimes1;
  private Measurement[]    workQueueWaitTimes2;
  private static final int DURATION       = 60 * 10;
  private static final int INIT_LOAD      = 400;
  private static final int MAX_LOAD       = 2000;
  private static final int PERCENT_UNIQUE = 100;
  private volatile boolean escape;

  public AbstractDualQueueThroughputTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    results1 = new LinkedList();
    results2 = new LinkedList();
  }

  public void run() {

    // Abstraction is not worth the effort

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

    System.err.println("LOAD STARTED");

    producer1.start();
    producer2.start();
    consumer1.start();
    consumer2.start();

    try {
      producer1.join();
      producer2.join();
    } catch (Throwable t) {
      notifyError(t);
    }

    System.err.println("DURATION COMPLETE");

    writeData();
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

    Measurement[] stats1 = new Measurement[results1.size()];
    for (int i = 0; i < stats1.length; i++) {
      data = (Metronome) results1.get(i);
      stats1[i] = new Measurement(data.load, data.endtime - data.starttime);
    }
    Measurement[] stats2 = new Measurement[results2.size()];
    for (int i = 0; i < stats2.length; i++) {
      data = (Metronome) results2.get(i);
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
          "Time Spent in Shared Queue2 - duration: " + header.duration + " sec. | " + header.xLabel,
          "Time Spent in Shared Queue1 - duration: " + header.duration + " sec. | " + header.xLabel };

      PerformanceMeasurementMarshaller.marshall(generateStatistics(), header, output, lineDescriptions);

    } catch (Throwable t) {
      notifyError(t);
    }
  }
}
