/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;

import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.LogManager;

public class DSOVerifierCDV424Test extends DSOVerifierTest {

  private final PortChooser pc = new PortChooser();

  protected String getMainClass() {
    return getClass().getName();
  }

  protected Collection<String> getExtraJvmArgs() {
    Collection<String> rv = new ArrayList<String>();
    rv.add("-Dcom.sun.management.jmxremote");
    rv.add("-Dcom.sun.management.jmxremote.authenticate=false");
    rv.add("-Dcom.sun.management.jmxremote.ssl=false");
    rv.add("-Dcom.sun.management.jmxremote.port=" + pc.chooseRandomPort());
    rv.add("-Djava.util.logging.manager=" + MyLogManager.class.getName());
    return rv;
  }

  public static class MyLogManager extends LogManager {
    //
  }

  public static void main(String[] args) {
    Class<? extends LogManager> c = LogManager.getLogManager().getClass();
    if (c != MyLogManager.class) { throw new RuntimeException("Wrong log manager class: " + c.getName()); }
    DSOVerifier.main(args);
  }

}
