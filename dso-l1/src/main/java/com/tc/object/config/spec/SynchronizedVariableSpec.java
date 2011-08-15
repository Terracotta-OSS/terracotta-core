/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.spec;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedVariable;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.Visitable;

public class SynchronizedVariableSpec implements Visitable {

  public ConfigVisitor visit(ConfigVisitor visitor, DSOApplicationConfig config) {
    String classname = SynchronizedVariable.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");
    return visitor;
  }

}
