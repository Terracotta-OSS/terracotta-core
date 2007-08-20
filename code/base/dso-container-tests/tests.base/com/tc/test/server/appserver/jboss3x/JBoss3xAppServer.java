/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss3x;

import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.jboss.JBoss3xInstalledLocalContainer;
import org.codehaus.cargo.container.property.GeneralPropertySet;

import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.util.AppServerUtil;
import com.tc.util.PortChooser;
import com.tc.util.ReplaceLine;

import java.io.File;
import java.io.IOException;

/**
 * JBoss3x AppServer implementation
 */
public final class JBoss3xAppServer extends CargoAppServer {

  public JBoss3xAppServer(JBoss3xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "jboss3x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new TCJBoss3xInstalledLocalContainer(config);
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }

  private static class TCJBoss3xInstalledLocalContainer extends JBoss3xInstalledLocalContainer {
    private final PortChooser pc = new PortChooser();

    public TCJBoss3xInstalledLocalContainer(LocalConfiguration configuration) {
      super(configuration);
    }

    protected void doStart(Java java) throws Exception {
      assignPorts();
      super.doStart(java);
    }

    private void assignPorts() throws IOException {
      ReplaceLine.Token[] tokens = new ReplaceLine.Token[13];
      int rmiPort = pc.chooseRandomPort();
      int rmiObjPort = new PortChooser().chooseRandomPort();

      tokens[0] = new ReplaceLine.Token(14, "(RmiPort\">[0-9]+)", "RmiPort\">" + rmiPort);
      tokens[1] = new ReplaceLine.Token(50, "(port=\"[0-9]+)", "port=\"" + rmiPort);
      tokens[2] = new ReplaceLine.Token(24, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[3] = new ReplaceLine.Token(32, "(port=\"[0-9]+)", "port=\"" + rmiObjPort);
      tokens[4] = new ReplaceLine.Token(64, "(port=\"[0-9]+)", "port=\"" + rmiObjPort);
      tokens[5] = new ReplaceLine.Token(40, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[6] = new ReplaceLine.Token(94, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[7] = new ReplaceLine.Token(101, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[8] = new ReplaceLine.Token(112, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[9] = new ReplaceLine.Token(57, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[10] = new ReplaceLine.Token(74, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
      tokens[11] = new ReplaceLine.Token(177, "(select=\"[^\"]+\")", "select=\"" + pc.chooseRandomPort() + "\"");
      tokens[12] = new ReplaceLine.Token(178, "(select=\"[^\"]+\")", "select=\"" + pc.chooseRandomPort() + "\"");

      ReplaceLine.parseFile(tokens, new File(getConfiguration().getHome(), "conf/cargo-binding.xml"));
    }

  }

}
