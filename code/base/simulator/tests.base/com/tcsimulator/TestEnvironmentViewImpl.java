/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tcsimulator.distrunner.ServerSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TestEnvironmentViewImpl implements TestEnvironmentView {
  private ServerViewImpl serverView;
  private Collection     clientViews;
  private final int      intensity;

  /*
   * Copies of specs are pass in.
   */
  public TestEnvironmentViewImpl(ServerSpec serverSpec, Collection clientSpecs, int intensity) {
    this.intensity = intensity;
    this.clientViews = new ArrayList();
    update(serverSpec, clientSpecs);
  }

  public synchronized void update(ServerSpec serverSpec, Collection clientSpecs) {
    update(serverSpec);
    update(clientSpecs);
  }

  public synchronized void update(ServerSpec serverSpec) {
    this.serverView = new ServerViewImpl(serverSpec);
  }

  public synchronized void update(Collection clientSpecs) {
    for (Iterator i = clientSpecs.iterator(); i.hasNext();) {
      ClientSpec cSpec = (ClientSpec) i.next();
      ClientView cView = new ClientViewImpl(cSpec);
      this.clientViews.add(cView);
    }
  }

  public synchronized void setServerRunning(int val) {
    this.serverView.setServerRunning(val);
  }

  public synchronized ServerView getServerView() {
    return this.serverView.copy();
  }

  public synchronized Collection getClientViews() {
    return new ArrayList(this.clientViews);
  }

  public int getIntensity() {
    return this.intensity;
  }

}
