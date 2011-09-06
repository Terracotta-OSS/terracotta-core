/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tcsimulator.distrunner.ServerSpec;

import java.util.Collection;

public interface TestEnvironmentView {

  public ServerView getServerView();

  public Collection getClientViews();

  public void update(ServerSpec serverSpec, Collection clientSpecs);

  public void update(ServerSpec serverSpec);

  public void update(Collection clientSpecs);

  public int getIntensity();

}
