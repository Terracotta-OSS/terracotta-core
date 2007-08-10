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

public class ArrayMutateValidateTestApp extends AbstractMutateValidateTransparentApp {

  private String[]     myArrayTestRoot;
  private List         validationArray;
  private int          iterationCount1;
  private int          iterationCount2;
  private int          iterationCount3;
  private final String appId;

  public ArrayMutateValidateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    myArrayTestRoot = new String[] { "hee", "hoo", "haa" };
    iterationCount1 = 9;
    iterationCount2 = 9;
    iterationCount3 = 9;
    validationArray = new ArrayList();
  }

  protected void mutate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount1; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);

        Thread.sleep(1000);

        if (i % 3 == 0) {
          debugPrintln("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
        }
      }
    }
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount2; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);

        Thread.sleep(1000);

        if (i % 3 == 0) {
          debugPrintln("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
        }
      }
    }
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount3; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);

        Thread.sleep(1000);

        if (i % 3 == 0) {
          debugPrintln("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
        }
      }
    }
  }

  protected void validate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < (iterationCount1 + iterationCount2 + iterationCount3) * getParticipantCount(); i++) {

        if (i % 100 == 0) {
          debugPrintln("****** appId[" + appId + "]:   index=[" + i + "]");
          debugPrintln("***** " + validationArray.get(i));
        }

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
