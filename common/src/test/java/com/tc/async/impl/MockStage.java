/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.text.PrettyPrinter;

/**
 * @author orion
 */
public class MockStage implements Stage {

  private final String  name;
  public final MockSink sink;

  public MockStage(String name) {
    this.name = name;
    this.sink = new MockSink();
  }

  @Override
  public void destroy() {
    //
  }

  @Override
  public synchronized Sink getSink() {
    return sink;
  }

  @Override
  public void start(ConfigurationContext context) {
    //
  }

  public void turnTracingOn() {
    //
  }

  public void turnTracingOff() {
    //
  }

  @Override
  public String toString() {
    return "MockStage(" + name + ")";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return null;
  }

}