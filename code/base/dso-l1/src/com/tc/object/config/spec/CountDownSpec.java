/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
