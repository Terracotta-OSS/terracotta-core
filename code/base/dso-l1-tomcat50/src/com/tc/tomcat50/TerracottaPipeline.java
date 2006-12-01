/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat50;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardPipeline;

import com.tc.tomcat50.session.SessionValve50;

public class TerracottaPipeline extends StandardPipeline {

  private final SessionValve50 tcValve;

  public TerracottaPipeline(Container container) {
    super(container);
    this.tcValve = new SessionValve50();
    super.addValve(this.tcValve);
  }

  public void removeValve(Valve valve) {
    if (valve == tcValve) { throw new IllegalArgumentException("Cannot remove the terracotta session valve"); }
    super.removeValve(valve);
  }

  public Valve[] getValves() {
    Valve[] rv = super.getValves();
    if (super.valves == rv) {
      // make defensive copy
      rv = (Valve[]) rv.clone();
    }
    return rv;
  }

}
