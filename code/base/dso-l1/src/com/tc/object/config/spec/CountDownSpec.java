/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.spec;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;

public class CountDownSpec {

  public void visit(ConfigVisitor visitor, DSOApplicationConfig config) {
    String countdownClassname = CountDown.class.getName();
    config.addIncludePattern(countdownClassname);
    config.addWriteAutolock("* " + countdownClassname + ".*(..)");    
  }

}
