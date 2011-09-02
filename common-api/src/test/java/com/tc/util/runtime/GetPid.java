/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.test.TestConfigObject;

import java.io.File;

public class GetPid {

  private static GetPid instance;

  public synchronized static GetPid getInstance() {
    if (instance == null) {
      String nativeLibPath = TestConfigObject.getInstance().executableSearchPath() + File.separator
                             + TestConfigObject.getInstance().nativeLibName();
      instance = new GetPid(nativeLibPath);
    }
    return instance;
  }

  private GetPid(String nativeLibPath) {
    System.load(nativeLibPath);
  }

  public native int getPid();

}