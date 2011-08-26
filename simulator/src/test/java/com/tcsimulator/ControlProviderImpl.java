/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.control.Control;
import com.tc.simulator.crasher.ControlProvider;

import java.util.HashMap;
import java.util.Map;

public class ControlProviderImpl implements ControlProvider {

  private final Map controls;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String classname = ControlProviderImpl.class.getName();
    config.addIncludePattern(classname);
    config.addAutolock("* " + classname + ".*(..)", ConfigLockLevel.WRITE);
    config.addRoot(new Root(classname, "controls", classname + ".controls"), true);
    ControlImpl.visitL1DSOConfig(visitor, config);
  }

  public static void visitDSOApplicationConfig(com.tc.object.config.ConfigVisitor visitor,
                                               com.tc.object.config.DSOApplicationConfig config) {
    String classname = ControlProviderImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");
    config.addRoot("controls", classname + ".controls");
    visitor.visitDSOApplicationConfig(config, ControlImpl.class);
  }

  public ControlProviderImpl() {
    controls = new HashMap();
  }

  public Control getOrCreateControlByName(String name, int parties) {
    synchronized (controls) {
      Control rv = (Control) controls.get(name);
      if (rv == null) {
        rv = new ControlImpl(parties);
        controls.put(name, rv);
      }
      return rv;
    }
  }

}
