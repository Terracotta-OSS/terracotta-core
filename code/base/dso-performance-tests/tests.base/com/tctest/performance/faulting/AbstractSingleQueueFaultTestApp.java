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

public abstract class AbstractSingleQueueFaultTestApp extends AbstractTransparentApp {

  private static final int    DURATION       = 60 * 10;
  private static final int    INIT_LOAD      = 200;
  private static final int    MAX_LOAD       = 2000;
  private static final int    PERCENT_UNIQUE = 100;

  private final CyclicBarrier barrier;
  private int                 writerCounter;
  private final List          sharedResults;
  private boolean             killWriters;

  private boolean             isLocalWriter, isMasterNode;
  private final int           writers;
  private final List          results;
  private Measurement[]       workQueueWaitTimes;
  private Metronome[]         compiledResults;

  public AbstractSingleQueueFaultTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    writers = getIntensity();
    results = new LinkedList();
    barrier = new CyclicBarrier(getParticipantCount());
    sharedResults = new ArrayList();
  }

  protected static TransparencyClassSpec visitConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    String className = AbstractSingleQueueFaultTestApp.class.getName();
    spec = config.getOrCreateSpec(className);

    config.addWriteAutolock("* " + className + ".run()");
    config.addWriteAutolock("* " + className + ".doWriter()");
    config.addReadAutolock("* " + className + ".killWriters()");
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    spec.addRoot("isReader", "isReader");
    spec.addRoot("killWriters", "killWriters");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("sharedResults", "sharedResults");
    spec.addRoot("writerCounter", "writerCounter");

    return spec;
  }

  protected static void visitConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(CyclicBarrier.class.getName());
    String className = AbstractSingleQueueFaultTestApp.class.getName();
    config.addIncludePattern(className);

    config.addWriteAutolock("* " + className + ".run()");
    config.addWriteAutolock("* " + className + ".doWriter()");
    config.addReadAutolock("* " + className + ".killWriters()");
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    config.addRoot("isReader", className + ".isReader");
    config.addRoot("killWriters", className + ".killWriters");
    config.addRoot("barrier", className + ".barrier");
    config.addRoot("sharedResults", className + ".sharedResults");
    config.addRoot("writerCounter", className + ".writerCounter");
  }

  public void run() {
    synchronized (barrier) {
      if (++writerCounter <= writers) {
        isLocalWriter = true;
      }
    }
    try {

      barrier.await();

      if (isLocalWriter) doWriter();
      else doReader();

      barrier.await();

      if (isMasterNode) {
        System.err.println("DURATION COMPLETE - TIMESTAMP " + System.currentTimeMillis());
        System.err.println("Compliling Shared Results");
      }

      synchronized (sharedResults) {
        sharedResults.addAll(results);
      }

      barrier.await();

    } catch (Throwable t) {
      notifyError(t);
    }

    if (isMasterNode) {
      synchronized (sharedResults) {
        compiledResults = (Metronome[]) sharedResults.toArray(new Metronome[0]);
        Arrays.sort(sharedResults.toArray(compiledResults));
      }
      writeData();
    }
  }

  private void doWriter() throws Throwable {
    Thread producer = new Thread() {
      public void run() {
        int init = INIT_LOAD / (getParticipantCount() - 1);
        int max = MAX_LOAD / (getParticipantCount() - 1);
        SimulatedType sInt = SimulatedTypeFactory.create(new Integer(0));
        LoadGenerator loadGenerator = new LinearTransitionLoadGenerator();
        loadGenerator.start(DURATION, init, max, sInt, PERCENT_UNIQUE);
        try {
          int count = 0;
          while (true) {
            if (count > 100) {
              count = 0;
              if (killWriters()) return;
            }
            Object obj = loadGenerator.getNext();
            if (obj == null) {
              workQueueWaitTimes = loadGenerator.getWaitTimes();
              isMasterNode = true;
              break; // work complete
            }
            populate(obj);
          }
        } catch (WorkQueueOverflowException e) {
          System.err.println("UPPER BOUND REACHED");
          synchronized (this) {
            killWriters = true;
            isMasterNode = true;
          }
          workQueueWaitTimes = loadGenerator.getWaitTimes();
          return;
        } catch (InterruptedException e) {
          throw new RuntimeException(e); // unexpected
        }
      }
    };
    producer.setDaemon(true);
    producer.start();

    System.err.println("LOAD STARTED - DURATION " + DURATION + "sec (15sec prep) - TIMESTAMP "
                       + System.currentTimeMillis());

    // readers await at consumer.start()
    barrier.await();

    try {
      // wait for producer to complete
      producer.join();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void doReader() throws Throwable {
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

    // writer awaits at producer.start()
    barrier.await();
    consumer.start();
  }

  private synchronized boolean killWriters() {
    return killWriters;
  }

  protected abstract void populate(Object data) throws InterruptedException;

  protected abstract void retrieve() throws InterruptedException;

  protected abstract String title();

  protected List results() {
    return results;
  }

  protected List generateStatistics() {
    List measurementList = new ArrayList();
    Measurement[] stats = new Measurement[compiledResults.length];
    Metronome data;
    for (int i = 0; i < stats.length; i++) {
      data = compiledResults[i];
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
