/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

public class MutateValidateArrayTestApp extends AbstractMutateValidateTransparentApp {

  private String[]     myArrayTestRoot;
  private List         validationArray;
  private int          iterationCount1;
  private final String appId;

  public MutateValidateArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    myArrayTestRoot = new String[] { "hee", "hoo", "haa" };
    iterationCount1 = 300;
    validationArray = new ArrayList();
  }

  protected void mutate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount1; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);
        debugPrintln("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
      }
    }
  }

  protected void validate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount1 * getParticipantCount(); i++) {
        debugPrintln("****** appId[" + appId + "]:   index=[" + i + "]");
        debugPrintln("***** " + validationArray.get(i));

        boolean val = myArrayTestRoot[(i + 1) % myArrayTestRoot.length].equals(validationArray.get(i));
        if (!val) {
          notifyError("Expecting <" + myArrayTestRoot[(i + 1) % myArrayTestRoot.length] + "> but got <"
                      + validationArray.get(i) + ">");
        }
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MutateValidateArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myArrayTestRoot", "myArrayTestRoot");
    spec.addRoot("validationArray", "validationArray");
  }

}
