/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;

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

  public boolean matches(ClassInfo classInfo) {
    throw new AssertionError();
  }

  public boolean isInclude() {
    return false;
  }
  
  public boolean isExclude() {
    return false;
  }

}
