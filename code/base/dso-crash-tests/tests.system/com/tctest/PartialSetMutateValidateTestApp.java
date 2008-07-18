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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class PartialSetMutateValidateTestApp extends AbstractMutateValidateTransparentApp {
  private final Set        mySetRoot;
  private final Set        myTreeSetRoot;
  private final static int VALIDATOR_NUMBERS_ADDED = 500;

  public PartialSetMutateValidateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    mySetRoot = new HashSet();
    addElementsToSet(VALIDATOR_NUMBERS_ADDED);
    myTreeSetRoot = new TreeSet();
  }

  private void addElementsToSet(int numbersToAdd) {
    synchronized (mySetRoot) {
      if (mySetRoot.size() == 0) {
        for (int i = 0; i < numbersToAdd; i++)
          mySetRoot.add(new PartialSetNode(i));
      }
    }
  }

  protected void mutate() throws Throwable {
    // add elements to the tree
    addToTreeSet();
  }

  private void addToTreeSet() {
    synchronized (myTreeSetRoot) {
      Iterator iter = mySetRoot.iterator();
      while(iter.hasNext())
        myTreeSetRoot.add(iter.next());
    }
  }

  protected void validate() throws Throwable {
    synchronized (myTreeSetRoot) {
      Assert.assertEquals(VALIDATOR_NUMBERS_ADDED, mySetRoot.size());
      Assert.assertEquals(VALIDATOR_NUMBERS_ADDED, myTreeSetRoot.size());
      Assert.assertTrue(mySetRoot.containsAll(myTreeSetRoot));
        
      int count = 0;
      for (Iterator iter = myTreeSetRoot.iterator(); iter.hasNext(); ) {
        PartialSetNode node = (PartialSetNode)iter.next();
        Assert.assertEquals(node.getNumber(), count);
        count++;
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = PartialSetMutateValidateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("mySetRoot", "mySetRoot");
    spec.addRoot("myTreeSetRoot", "myTreeSetRoot");
    config.getOrCreateSpec(PartialSetNode.class.getName());
  }

}
