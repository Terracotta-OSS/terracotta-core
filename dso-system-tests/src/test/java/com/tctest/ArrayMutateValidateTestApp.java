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

import java.util.ArrayList;
import java.util.List;

public class ArrayMutateValidateTestApp extends AbstractMutateValidateTransparentApp {

  private final String[] myArrayTestRoot;
  private final List     validationArray;
  private final int      iterationCount1;
  private final int      iterationCount2;
  private final int      iterationCount3;

  public ArrayMutateValidateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    myArrayTestRoot = new String[] { "hee", "hoo", "haa" };
    iterationCount1 = 9;
    iterationCount2 = 9;
    iterationCount3 = 9;
    validationArray = new ArrayList();
  }

  protected void mutate() throws Throwable {
    doMutate(iterationCount1);
    doMutate(iterationCount2);
    doMutate(iterationCount3);
  }

  private void doMutate(int iterationCount) {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);
      }
    }
  }

  protected void validate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < (iterationCount1 + iterationCount2 + iterationCount3) * getParticipantCount(); i++) {

        boolean val = myArrayTestRoot[(i + 1) % myArrayTestRoot.length].equals(validationArray.get(i));
        if (!val) {
          notifyError("Expecting <" + myArrayTestRoot[(i + 1) % myArrayTestRoot.length] + "> but got <"
                      + validationArray.get(i) + ">");
        }
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ArrayMutateValidateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myArrayTestRoot", "myArrayTestRoot");
    spec.addRoot("validationArray", "validationArray");
  }

}
