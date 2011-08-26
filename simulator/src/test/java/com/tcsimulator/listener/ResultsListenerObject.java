/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.listener;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.listener.ResultsListener;

import java.util.ArrayList;
import java.util.List;

public final class ResultsListenerObject implements ResultsListener {

  boolean        startTimeout     = false;
  boolean        executionTimeout = false;
  private Object result;
  private List   errors;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper cfg) {
    String classname = ResultsListenerObject.class.getName();
    TransparencyClassSpec spec = cfg.getOrCreateSpec(classname);

    spec.addTransient("startTimeout");
    spec.addTransient("results");
    spec.addTransient("errors");
  }

  public synchronized void notifyStartTimeout() {
    startTimeout = true;
  }

  public synchronized void notifyExecutionTimeout() {
    executionTimeout = true;
  }

  public synchronized void notifyError(ErrorContext ectxt) {
    if (this.errors == null) this.errors = new ArrayList();
    this.errors.add(ectxt);
  }

  public synchronized void notifyResult(Object theResult) {
    this.result = theResult;
  }

  public synchronized void addErrorsTo(List addTo) {
    addTo.addAll(this.errors);
  }

  Object getResult() {
    return this.result;
  }
}