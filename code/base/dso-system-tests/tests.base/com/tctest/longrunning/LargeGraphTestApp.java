/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.exception.TCRuntimeException;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.container.ContainerBuilderConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.ResultsListener;
import com.tc.util.Assert;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LargeGraphTestApp implements Application, ApplicationConfigBuilder {

  private static final DateFormat df          = new SimpleDateFormat("yyyy-MM=dd HH:mm:ss,SSS Z");

  private final String            appId;
  private final ResultsListener   resultsListener;
  private final Random            random      = new Random();
  private int                     idCounter;

  private final Map               graph       = new HashMap();
  private final Collection        references;
  private final Map               referencesByNodeID;
  private final Map               nodesByNodeID;
  private final List              objectCount = new ArrayList(1);

  private ConfigVisitor           visitor;

  /**
   * This ctor is for when it's an ApplicationConfigBuilder
   */
  public LargeGraphTestApp(ContainerBuilderConfig containerBuilderConfig) {
    this("", null, null);
    visitor = new ConfigVisitor();
  }

  /**
   * This ctor is for when it's an Application
   */
  public LargeGraphTestApp(String appId, ApplicationConfig cfg, ListenerProvider listeners) {
    this.appId = appId;
    if (listeners != null) this.resultsListener = listeners.getResultsListener();
    else this.resultsListener = null;

    if (doVerify()) {
      referencesByNodeID = new HashMap();
      nodesByNodeID = new HashMap();
      references = new ArrayList();
    } else {
      referencesByNodeID = null;
      nodesByNodeID = null;
      references = null;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = LargeGraphTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);

    spec.addRoot("graph", testClassName + ".graph");
    spec.addRoot("objectCount", testClassName + ".objectCount");
    if (doVerify()) {
      spec.addRoot("references", testClassName + ".references");
      spec.addRoot("referencesByNodeID", testClassName + ".referencesByNodeID");
      spec.addRoot("nodesByNodeID", testClassName + ".nodesByNodeID");
    }

    config.addWriteAutolock("public void " + testClassName + ".growGraph(int, int)");
    config.addWriteAutolock("private void " + testClassName + ".incrementObjectCount()");
    config.addWriteAutolock("* " + testClassName + ".newGraphNode()");
    config.addReadAutolock("* " + testClassName + ".getObjectCount()");
    config.addReadAutolock("* " + testClassName + ".verifyGraph()");
    config.addReadAutolock("* " + testClassName + ".verifyReferences()");
    config.addReadAutolock("* " + testClassName + ".touchGraph()");

    spec.addTransient("resultsListener");
    spec.addTransient("outputListener");
    spec.addTransient("appId");
    spec.addTransient("random");
    spec.addTransient("idCounter");

    config.getOrCreateSpec(GraphNode.class.getName());
    config.getOrCreateSpec(NodeReference.class.getName());
  }

  public String getApplicationId() {
    return this.appId;
  }

  public boolean interpretResult(Object result) {
    return ((Boolean) result).booleanValue();
  }

  static boolean doVerify() {
    return false;
  }

  public void run() {
    try {
      int iteration = 0;
      while (true) {
        int batchSize = random.nextInt(1000);
        println("About to grow graph by " + batchSize + " nodes...");
        growGraph(batchSize, 50);
        if (random.nextInt(10) > 5) {
          touchGraph();
        }
        if (doVerify()) {
          verifyGraph();
          verifyReferences();
        }
        println("completed " + (++iteration) + " iteration(s); There are now " + getObjectCount() + " objects.");
      }
    } catch (Throwable t) {
      t.printStackTrace();
      resultsListener.notifyResult(Boolean.FALSE);
      throw new TCRuntimeException(t);
    }
  }

  private void println(Object o) {
    System.out.println(df.format(new Date()) + ": " + Thread.currentThread() + "[" + appId + "] " + o);
  }

  /**
   * Causes at least one second-level object to be read. requires read autolocks
   */
  public void touchGraph() throws Exception {
    println("About to touch graph...");
    synchronized (graph) {
      GraphNode node = (GraphNode) graph.get(appId);
      if (node != null) {
        println("root node has: " + node.getReferenceCount() + " references.");
        for (int i = 0; i < node.getReferenceCount(); i++) {
          GraphNode child = node.getReference(i);
          println("Child " + i + ": " + child);
        }
      } else {
        println("No starting node for: " + appId);
      }
    }
  }

  /**
   * Requires write autolocks.
   */
  public void growGraph(int theObjectCount, int bushyness) throws Exception {
    Assert.eval("Bushyness should be between zero and 100, inclusive.", bushyness <= 100 && bushyness >= 0);
    GraphNode node = null;
    synchronized (graph) {
      try {
        println("Growing graph: objectCount=" + theObjectCount + ", bushyness=" + bushyness + "...");
        node = (GraphNode) graph.get(appId);
        if (node == null) {
          node = newGraphNode();
          theObjectCount--;
          graph.put(appId, node);
          incrementObjectCount();
        }

        Collection newRoots = new ArrayList();
        newRoots.add(node);

        while (theObjectCount > 0) {
          newRoots = growGraph(theObjectCount, newRoots, bushyness);
          theObjectCount -= newRoots.size();
        }
      } catch (Throwable t) {
        t.printStackTrace();
        throw new TCRuntimeException(t);
      }
    }
    println("Done growing graph.");
  }

  private Collection growGraph(int theObjectCount, Collection roots, int bushyness) {
    Collection newRoots = new ArrayList();
    for (Iterator i = roots.iterator(); i.hasNext();) {
      GraphNode root = (GraphNode) i.next();
      boolean first = true;
      while (first || (beBushy(bushyness)) && theObjectCount > 0) {
        first = false;
        GraphNode newNode = newGraphNode();
        theObjectCount--;
        root.addReference(references, referencesByNodeID, newNode);
        incrementObjectCount();
        newRoots.add(newNode);
      }
      if (theObjectCount == 0) break;
    }
    return newRoots;
  }

  private boolean beBushy(int bushyness) {
    return random.nextInt(100) <= bushyness;
  }

  /**
   * I need write autolocks
   */
  private GraphNode newGraphNode() {
    String id = nextID();
    GraphNode node = new GraphNode(id);
    if (doVerify()) {
      List l = new ArrayList();
      synchronized (referencesByNodeID) {
        referencesByNodeID.put(id, l);
      }
      synchronized (nodesByNodeID) {
        nodesByNodeID.put(id, node);
      }
    }
    return node;
  }

  private synchronized String nextID() {
    return appId + ":" + idCounter++;
  }

  /**
   * I need read autolocks
   */
  public int getObjectCount() {
    synchronized (objectCount) {
      try {
        if (objectCount.size() > 0) {
          return ((Integer) objectCount.get(0)).intValue();
        } else {
          return 0;
        }
      } catch (Throwable t) {
        t.printStackTrace();
        throw new TCRuntimeException(t);
      }
    }
  }

  private void incrementObjectCount() {
    synchronized (objectCount) {
      try {
        Integer newValue = Integer.valueOf(getObjectCount() + 1);
        if (objectCount.size() == 0) {
          objectCount.add(newValue);
        } else {
          objectCount.set(0, newValue);
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  /**
   * I need read autolocks
   */
  public void verifyGraph() throws VerifyException {
    if (!doVerify()) return;
    Collection newRoots = null;
    synchronized (graph) {
      println("Starting to verify graph...");
      newRoots = graph.values();
      while (true) {
        newRoots = verifyGraph(newRoots);
        if (newRoots.size() == 0) {
          println("Done verifying graph.");
          return;
        }
      }
    }
  }

  private Collection verifyGraph(Collection roots) throws VerifyException {
    Collection newRoots = new ArrayList();
    for (Iterator i = roots.iterator(); i.hasNext();) {
      visitNode((GraphNode) i.next(), newRoots);
    }
    return newRoots;
  }

  private void visitNode(GraphNode node, Collection newRoots) throws VerifyException {
    List nodeReferences = (List) referencesByNodeID.get(node.getID());

    for (int i = 0; i < node.getReferenceCount(); i++) {
      GraphNode child = node.getReference(i);
      if (nodeReferences.size() < i + 1) { throw new VerifyException("There are not enough references for this node: "
                                                                     + child + "; nodeReferences.size(): "
                                                                     + nodeReferences.size()); }
      NodeReference reference = (NodeReference) nodeReferences.get(i);
      Assert.eval(node.getID().equals(reference.getReferrerID()));
      Assert.assertNotNull("Child at " + i + " for node: " + node + " was null!", child);
      Assert.assertNotNull("Reference at " + i + " for node: " + node + " was null!", reference);
      if (!child.getID().equals(reference.getReferredID())) {
        String message = "Child id: " + child.getID() + " not equal to reference.getReferredID(): "
                         + reference.getReferredID();
        throw new VerifyException(message);
      }
      newRoots.add(child);
    }
  }

  /**
   * I need read autolocks
   */
  public void verifyReferences() throws VerifyException {
    if (!doVerify()) return;
    synchronized (graph) {
      println("Starting to verify references...");
      for (Iterator i = references.iterator(); i.hasNext();) {
        NodeReference reference = (NodeReference) i.next();
        GraphNode referrer = (GraphNode) nodesByNodeID.get(reference.getReferrerID());
        GraphNode referred = referrer.getReference(reference.index());
        if (!reference.getReferredID().equals(referred.getID())) {
          String message = "reference.getRefferedID(): " + reference.getReferredID()
                           + " is not equal to referred.getID(): " + referred.getID();
          throw new VerifyException(message);
        }
      }
      println("Done verifying references...");
    }
  }

  public static class GraphNode {
    private final String id;

    private GraphNode[]  children = new GraphNode[0];

    public GraphNode(String id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return "GraphNode[id=" + id + ", children=" + enumerateChildren(new StringBuffer());
    }

    private StringBuffer enumerateChildren(StringBuffer buf) {
      buf.append("[");
      if (children == null) {
        buf.append("null");
      } else {
        for (int i = 0; i < children.length; i++) {
          if (i > 0) {
            buf.append(",");
          }
          if (children[i] != null) buf.append(children[i].getID());
          else buf.append("null");
        }
      }
      buf.append("]");
      return buf;
    }

    public String getID() {
      return this.id;
    }

    public synchronized void addReference(Collection references, Map index, GraphNode referred) {

      List l = new ArrayList(Arrays.asList(children));
      l.add(referred);
      children = new GraphNode[l.size()];
      // XXX: Put this back when the System.arraycopy(...) bug is fixed. --Orion 3/11/2005
      // l.toArray(children);
      for (int i = 0; i < children.length; i++) {
        children[i] = (GraphNode) l.get(i);
      }
      if (doVerify()) {
        NodeReference rv = new NodeReference(this, referred, children.length - 1);
        references.add(rv);
        l = (List) index.get(getID());
        l.add(rv);
      }
    }

    public synchronized int getReferenceCount() {
      return children.length;
    }

    public synchronized GraphNode getReference(int index) {
      return children[index];
    }

  }

  public static class VerifyException extends Exception {
    VerifyException(String m) {
      super(m);
    }
  }

  public static class NodeReference {
    private final String referrer;
    private final String referred;
    private final int    index;

    public NodeReference(GraphNode referrer, GraphNode referred, int index) {
      this.index = index;
      this.referrer = referrer.getID();
      this.referred = referred.getID();
    }

    public String getReferrerID() {
      return referrer;
    }

    public int index() {
      return this.index;
    }

    public String getReferredID() {
      return referred;
    }

  }

  /*********************************************************************************************************************
   * ApplicationConfigBuilder interface
   */
  public void visitClassLoaderConfig(DSOClientConfigHelper config) {
    this.visitor.visit(config, getClass());
  }

  public ApplicationConfig newApplicationConfig() {
    return new ApplicationConfig() {

      public String getApplicationClassname() {
        return LargeGraphTestApp.class.getName();
      }

      public void setAttribute(String key, String value) {
        //
      }

      public String getAttribute(String key) {
        return null;
      }

      public int getIntensity() {
        throw new AssertionError();
      }

      public int getGlobalParticipantCount() {
        throw new AssertionError();
      }

      public ApplicationConfig copy() {
        throw new AssertionError();
      }

      public ServerControl getServerControl() {
        throw new AssertionError();
      }

      public int getValidatorCount() {
        throw new AssertionError();
      }

      public int getGlobalValidatorCount() {
        throw new AssertionError();
      }

      public TCPProxy[] getProxies() {
        throw new AssertionError();
      }

      public ServerControl[] getServerControls() {
        throw new AssertionError();
      }

      public Object getAttributeObject(String key) {
        throw new AssertionError();
      }
    };
  }
}