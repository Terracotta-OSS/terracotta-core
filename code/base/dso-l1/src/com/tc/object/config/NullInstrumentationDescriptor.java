/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config;

public class NullInstrumentationDescriptor implements InstrumentationDescriptor {

  public String getOnLoadMethodIfDefined() {
    return null;
  }

  public String getOnLoadScriptIfDefined() {
    return null;
  }

  public boolean isCallConstructorOnLoad() {
    return false;
  }

  public boolean isHonorTransient() {
    return false;
  }
  
  public boolean isHonorVolatile() {
    return false;
  }

  public boolean matches(String fullName) {
    throw new AssertionError();
  }

  public boolean isInclude() {
    return false;
  }
  
  public boolean isExclude() {
    return false;
  }

}
