/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;

import java.util.ArrayList;
import java.util.List;

public class GlobalVmNameGenerator {

  private static final String VM_NAME_PREFIX = "vm";
  private List currentId = new ArrayList();

  public String nextVmName() {
    synchronized (currentId) {
      Long newId;
      if (currentId.size() == 0) {
        newId = new Long(0);
        currentId.add(newId);
      } else {
        Long id = (Long) currentId.get(0);
        newId = new Long(id.longValue() + 1);
        currentId.set(0, newId);
      }

      return GlobalVmNameGenerator.VM_NAME_PREFIX + newId.longValue();
    }
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String classname = GlobalVmNameGenerator.class.getName();
    config.addIncludePattern(classname);
    config.addRoot("currentId", classname + ".currentId");
    config.addWriteAutolock("* " + classname + ".*(..)");
  }
  
}
