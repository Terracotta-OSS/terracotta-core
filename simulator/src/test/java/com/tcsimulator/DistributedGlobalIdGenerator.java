/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.GlobalIdGenerator;

import java.util.ArrayList;
import java.util.List;

public class DistributedGlobalIdGenerator implements GlobalIdGenerator {

  private final List currentId = new ArrayList();

  public long nextId() {
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
      return newId.longValue();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper cfg) {
    String classname = DistributedGlobalIdGenerator.class.getName();
    TransparencyClassSpec spec = cfg.getOrCreateSpec(classname);
    spec.addRoot("DistributedGlobalIdGeneratorcurrentId", classname + ".currentId");
    cfg.addWriteAutolock("long " + classname + ".nextId()");
  }

  public static void visitDSOApplicationConfig(com.tc.object.config.ConfigVisitor visitor,
                                               com.tc.object.config.DSOApplicationConfig config) {
    String classname = DistributedGlobalIdGenerator.class.getName();
    config.addIncludePattern(classname);
    config.addRoot("DistributedGlobalIdGeneratorcurrentId", classname + ".currentId");
    config.addWriteAutolock("* " + classname + ".*(..)");
  }

}