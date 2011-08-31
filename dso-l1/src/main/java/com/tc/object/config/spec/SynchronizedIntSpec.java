/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.spec;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.Visitable;

public class SynchronizedIntSpec implements Visitable {

  public ConfigVisitor visit(ConfigVisitor visitor, DSOApplicationConfig config) {
    new SynchronizedVariableSpec().visit(visitor, config);
    String classname = SynchronizedInt.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");
    return visitor;
  }

}
