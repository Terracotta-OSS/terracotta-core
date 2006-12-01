/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load.webapp;

import org.apache.commons.lang.SerializationUtils;

import com.tctest.performance.http.load.AbstractHttpLoadTest;
import com.tctest.performance.sampledata.OrganicObjectGraph;
import com.tctest.performance.sampledata.OrganicObjectGraphManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class ValidateHttpFieldReplicationTest extends AbstractHttpLoadTest {

  private static final String VALIDATE = "validate";
  private boolean             validate;

  protected ValidateHttpFieldReplicationTest(String[] args) {
    super(setArgs(args));
    if (args.length == 2 && args[0].equals(VALIDATE)) validate = true;
  }

  protected abstract int changes();

  protected abstract int graphSize();

  protected final void execute() throws Exception {
    if (!validate) super.execute();
    else validateObjectGraphs(resultsDir());
  }

  private static String[] setArgs(String[] args) {
    if (args.length == 2 && args[0].equals(VALIDATE)) return new String[] { "0", args[1] };
    return args;
  }

  protected final void validateArgs(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage:");
      System.out.println("  [<duration in seconds> | validate] <working dir path> [report]");
      System.exit(0);
    }
  }

  private final void validateObjectGraphs(File resultsDir) throws Exception, FileNotFoundException {
    if (!resultsDir.exists()) {
      System.err.println("No Data Available to Validate");
      return;
    }
    String[] files = resultsDir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith("obj");
      }
    });
    OrganicObjectGraph[] graphArray;
    OrganicObjectGraph graph;
    String hostKey;
    Map hostGraphs = new HashMap();
    List graphs;

    // deserialize map of graphs keyed by host
    for (int i = 0; i < files.length; i++) {
      String[] parts = files[i].split("_");
      if ((parts.length == 3) && parts[1].equals("graph")) {
        System.out.println("  loading: " + files[i]);
        FileInputStream in = new FileInputStream(resultsDir() + File.separator + files[i]);
        graph = (OrganicObjectGraph) SerializationUtils.deserialize(in);
        System.out.println("  sequence #: " + graph.sequenceNumber());
        System.out.println("  change iteration count #: " + graph.changeIterationCount());
        System.out.println("  envkey: " + graph.envKey());
        System.out.println("--");
        hostKey = graph.envKey();
        if (!hostGraphs.containsKey(hostKey)) {
          graphs = new ArrayList();
          graphs.add(graph);
          hostGraphs.put(hostKey, graphs);
        } else {
          graphs = (List) hostGraphs.get(hostKey);
          graphs.add(graph);
        }
      }
    }
    System.out.println("");
    
    Iterator iter = hostGraphs.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      graphs = (ArrayList) entry.getValue();
      graphArray = new OrganicObjectGraph[graphs.size()];

      // sort graphs by sequence number
      for (int i = 0; i < graphArray.length; i++) {
        graph = (OrganicObjectGraph) graphs.get(i);
        graphArray[graph.sequenceNumber()] = graph; 
      }
      // validate graphs per host
      if (!OrganicObjectGraphManager.validate(graphArray, graphSize(), changes())) {
        System.out.println("\nGraph Validation: Failed");
        return;
      }
    }
    System.out.println("\nGraph Validation: Passed");
  }
}
