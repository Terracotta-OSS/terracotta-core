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

public abstract class AbstractSingleQueueThroughputTestApp extends AbstractTransparentApp {

  private final List       results;
  private Measurement[]    workQueueWaitTimes;
  private static final int DURATION       = 60 * 10;
  private static final int INIT_LOAD      = 600;
  private static final int MAX_LOAD       = 3000;
  private static final int PERCENT_UNIQUE = 100;

  public AbstractSingleQueueThroughputTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    results = new LinkedList();
  }

  public void run() {
    Thread producer = new Thread() {
      public void run() {
        SimulatedType sInt = SimulatedTypeFactory.create(new Integer(0));
        LoadGenerator loadGenerator = new LinearTransitionLoadGenerator();
        loadGenerator.start(DURATION, INIT_LOAD, MAX_LOAD, sInt, PERCENT_UNIQUE);
        try {
          while (true) {
            Object obj = loadGenerator.getNext();
            if (obj == null) {
              workQueueWaitTimes = loadGenerator.getWaitTimes();
              break; // work complete
            }
            populate(obj);
          }
        } catch (WorkQueueOverflowException e) {
          System.err.println("UPPER BOUND REACHED");
          workQueueWaitTimes = loadGenerator.getWaitTimes();
          return;

        } catch (InterruptedException e) {
          throw new RuntimeException(e); // unexpected
        }
      }
    };
    producer.setDaemon(true);

    Thread consumer = new Thread() {
      public void run() {
        try {
          while (true) {
            retrieve();
          }
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    };
    consumer.setDaemon(true);

    System.err.println("LOAD STARTED");

    producer.start();
    consumer.start();

    try {
      producer.join();
    } catch (Throwable t) {
      notifyError(t);
    }

    System.err.println("DURATION COMPLETE");

    writeData();
  }

  protected abstract void populate(Object data) throws InterruptedException;

  protected abstract void retrieve() throws InterruptedException;

  protected abstract String title();

  protected List results() {
    return results;
  }

  protected List generateStatistics() {
    List measurementList = new ArrayList();
    Measurement[] stats = new Measurement[results.size()];
    Metronome data;
    for (int i = 0; i < stats.length; i++) {
      data = (Metronome) results.get(i);
      stats[i] = new Measurement(data.load, data.endtime - data.starttime);
    }
    measurementList.add(workQueueWaitTimes);
    measurementList.add(stats);

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

      String[] lineDescriptions = new String[] { "Work Queue Wait Time",
          "Time Spent in Shared Queue - duration: " + header.duration + " sec. | " + header.xLabel };

      PerformanceMeasurementMarshaller.marshall(generateStatistics(), header, output, lineDescriptions);

    } catch (Throwable t) {
      notifyError(t);
    }
  }
}
