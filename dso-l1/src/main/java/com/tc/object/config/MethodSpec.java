/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

public class MethodSpec {
  public final static int ALWAYS_LOG = 1;

  private final String    name;
  private final int       instrumentationType;

  public MethodSpec(String name, int instrumentationType) {
    this.name = name;
    this.instrumentationType = instrumentationType;
  }

  public String getName() {
    return name;
  }

  public int getInstrumentationType() {
    return instrumentationType;
  }
}
