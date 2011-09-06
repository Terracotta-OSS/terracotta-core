/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.reflect.ConstructorInfo;

/**
 * Implementation of ConstructorInfo
 */
public class AsmConstructorInfo extends AsmMethodInfo implements ConstructorInfo {

  public AsmConstructorInfo(ClassInfoFactory classInfoFactory, int modifiers, String className, String methodName, String desc, String[] exceptions) {
    super(classInfoFactory, modifiers, className, methodName, desc, exceptions);
  }

  //public String getName() { return "new"; }

  public String[] getParameterNames() {
    return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getSignature() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
