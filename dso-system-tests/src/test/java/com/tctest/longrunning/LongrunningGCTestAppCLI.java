/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.longrunning;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.distrunner.ArgException;
import com.tc.simulator.distrunner.ArgParser;
import com.tc.simulator.distrunner.SpecFactoryImpl;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.MutationCompletionListener;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListener;
import com.tcsimulator.ApplicationConfigImpl;
import com.tcsimulator.listener.OutputListenerObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public class LongrunningGCTestAppCLI {
  private static final String  PARTICIPANT_COUNT_ARG = "participantcount";
  private static int           globalParticipantCount;
  private static int           intensity;
  private static CyclicBarrier barrier;

  public static void main(String[] args) throws ArgException, BrokenBarrierException, InterruptedException {
    parseArgs(args);
    barrier = new CyclicBarrier(globalParticipantCount);
    ArgParser parser = new ArgParser(args, new SpecFactoryImpl(), false, false);
    intensity = parser.getIntensity();

    String appId = "1";
    String applicatonClassname = LongrunningGCTestApp.class.getName();
    ApplicationConfig cfg = new ApplicationConfigImpl(applicatonClassname, intensity, globalParticipantCount);
    ListenerProvider provider = new TestAppApplicationListenerProvider(new OutputListenerObject(), null);
    LongrunningGCTestApp testApp = new LongrunningGCTestApp(appId, cfg, provider);

    System.out.println("Waiting for other clients to start...");
    barrier.barrier();
    System.out.println("Done waiting for all clients to start.");
    testApp.run();
  }

  private static void parseArgs(String[] args) throws ArgException {
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith(PARTICIPANT_COUNT_ARG)) {
        String[] nvPair = args[i].split("=");
        if (nvPair.length != 2) { throw new ArgException("Malformed argument: " + args[i]); }
        globalParticipantCount = Integer.parseInt(nvPair[1]);
      }
    }
    if (globalParticipantCount == 0) { throw new AssertionError("Participant count must be specified."); }
  }

  /* ======================================================================= */

  private static class TestAppApplicationListenerProvider implements ListenerProvider {
    private final OutputListener  outputListener;
    private final ResultsListener resultsListener;

    public TestAppApplicationListenerProvider(OutputListener ol, ResultsListener rl) {
      this.outputListener = ol;
      this.resultsListener = rl;
    }

    public OutputListener getOutputListener() {
      return outputListener;
    }

    public ResultsListener getResultsListener() {
      return resultsListener;
    }

    public StatsListener newStatsListener(Properties properties) {
      return new TestAppStatsListenerObject(properties);
    }

    public MutationCompletionListener getMutationCompletionListener() {
      throw new AssertionError("This method needs to be implemented");
    }

  }

  /* ======================================================================= */

  private static class TestAppStatsListenerObject implements StatsListener {
    private final String        label;
    private static final String TOKEN = "<app-perf>";

    public TestAppStatsListenerObject(Properties properties) {
      StringBuffer buf = new StringBuffer();
      for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Entry) i.next();
        buf.append(entry.getKey() + "=" + entry.getValue());
        if (i.hasNext()) {
          buf.append(",");
        }
      }
      this.label = buf.toString();
    }

    public void sample(long sampleValue, String desc) {
      System.out.println(TOKEN + label + ": " + sampleValue);
    }

  }

}
