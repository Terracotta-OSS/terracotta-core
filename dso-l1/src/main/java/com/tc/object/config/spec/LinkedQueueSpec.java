/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.spec;

import EDU.oswego.cs.dl.util.concurrent.LinkedNode;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.Visitable;

public class LinkedQueueSpec implements Visitable {

  public ConfigVisitor visit(ConfigVisitor visitor, DSOApplicationConfig config) {
    String linkedQueueClassname = LinkedQueue.class.getName();
    config.addIncludePattern(linkedQueueClassname);

    String linkedNodeClassname = LinkedNode.class.getName();
    config.addIncludePattern(linkedNodeClassname);

    // LinkedQueue config
    String linkedQueueExpression = "* " + linkedQueueClassname + ".*(..)";
    config.addWriteAutolock(linkedQueueExpression);

    return visitor;
  }

}
