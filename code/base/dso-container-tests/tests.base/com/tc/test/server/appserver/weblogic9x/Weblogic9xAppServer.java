/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic9x;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.internal.AntContainerExecutorThread;
import org.codehaus.cargo.container.weblogic.WebLogic9xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.util.ReplaceLine;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * Weblogic9x AppServer implementation
 */
public final class Weblogic9xAppServer extends CargoAppServer {

  public Weblogic9xAppServer(Weblogic9xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "weblogic9x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new TCWebLogic9xInstalledLocalContainer(config);
  }

  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    // config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }

  private static class TCWebLogic9xInstalledLocalContainer extends WebLogic9xInstalledLocalContainer {

    public TCWebLogic9xInstalledLocalContainer(LocalConfiguration configuration) {
      super(configuration);
    }

    public void doStop(Java java) throws Exception {
      // Use the weblogic scripting interface to stop WLS 9.2. Sometimes we get RMI exceptions trying to stop WLS9.2
      // using the 8.1 stop method. The stop scripts supplied with version 9 use this scripting stuff so I doing the
      // same here.

      // Hack --> remove the extra build listener that gets added to the ant project
      Vector buildListeners = java.getProject().getBuildListeners();
      if (buildListeners.size() > 1) {
        BuildListener listener = (BuildListener) buildListeners.get(buildListeners.size() - 1);
        java.getProject().removeBuildListener(listener);
      }

      File serverDir = new File(getHome(), "server");
      Path classpath = java.createClasspath();
      classpath.createPathElement().setLocation(new File(serverDir, "lib/weblogic_sp.jar"));
      classpath.createPathElement().setLocation(new File(serverDir, "lib/weblogic.jar"));
      java.setClassname("weblogic.WLST");
      java.createArg().setValue(createShutdownScript().getAbsolutePath());
      AntContainerExecutorThread webLogicRunner = new AntContainerExecutorThread(java);
      webLogicRunner.start();
    }

    private File createShutdownScript() throws IOException {
      String port = getConfiguration().getPropertyValue("cargo.servlet.port");
      String user = getConfiguration().getPropertyValue("cargo.weblogic.administrator.user");
      String passwd = getConfiguration().getPropertyValue("cargo.weblogic.administrator.password");

      File tmp = File.createTempFile("wls92shutdown", ".py");
      tmp.deleteOnExit();

      FileOutputStream out = null;

      try {
        out = new FileOutputStream(tmp);

        String connect = "connect(url='t3://localhost:" + port + "',adminServerName='AdminServer',username='" + user
                         + "',password='" + passwd + "')\n";
        out.write(connect.getBytes("UTF-8"));
        out.write("shutdown(name='AdminServer',entityType='Server',force='true',block='true')\n".getBytes("UTF-8"));
        out.write("exit()\n".getBytes("UTF-8"));
        out.flush();
      } finally {
        if (out != null) {
          out.close();
        }
      }

      return tmp;
    }

    protected void setState(State state) {
      if (state.equals(State.STARTING)) {
        adjustConfig();
        setBeaHomeIfNeeded();
        prepareSecurityFile();
      }
    }

    private void adjustConfig() {
      ReplaceLine.Token[] tokens = new ReplaceLine.Token[1];
      tokens[0] = new ReplaceLine.Token(
                                        5,
                                        "(NativeIOEnabled=\"false\")",
                                        "NativeIOEnabled=\"false\" SocketReaderTimeoutMaxMillis=\"1000\" SocketReaderTimeoutMinMillis=\"1000\" StdoutDebugEnabled=\"true\" StdoutSeverityLevel=\"64\"");

      try {
        ReplaceLine.parseFile(tokens, new File(getConfiguration().getHome(), "/config/config.xml"));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    private void setBeaHomeIfNeeded() {
      File license = new File(getHome(), "license.bea");
      if (license.exists()) {
        this.setBeaHome(this.getHome());
      }
    }

    private void prepareSecurityFile() {
      if (Os.isLinux()) {
        try {
          String[] resources = new String[] { "security/SerializedSystemIni.dat" };
          for (int i = 0; i < resources.length; i++) {
            String resource = "linux/" + resources[i];
            File dest = new File(getConfiguration().getHome(), resources[i]);
            copyResource(resource, dest);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      }
    }

    private void copyResource(String name, File dest) throws IOException {
      dest.getParentFile().mkdirs();
      InputStream in = getClass().getResourceAsStream(name);
      FileOutputStream out = new FileOutputStream(dest);
      try {
        IOUtils.copy(in, out);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }
  }

}