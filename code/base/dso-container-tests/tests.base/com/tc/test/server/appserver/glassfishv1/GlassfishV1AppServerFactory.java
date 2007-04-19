/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.glassfishv1;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.war.DtdWar;
import com.tc.test.server.appserver.war.War;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * This class creates specific implementations of return values for the given methods. To obtain an instance you must
 * call {@link NewAppServerFactory.createFactoryFromProperties()}.
 */
public final class GlassfishV1AppServerFactory extends NewAppServerFactory {

  // This class may only be instantiated by its parent which contains the ProtectedKey
  public GlassfishV1AppServerFactory(ProtectedKey protectedKey, TestConfigObject config) {
    super(protectedKey, config);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new GlassfishV1AppServerParameters(instanceName, props, config.sessionClasspath());
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new GlassfishV1AppServer((GlassfishV1AppServerInstallation) installation);
  }

  public AppServerInstallation createInstallation(URL host, File serverDir, File workingDir) throws Exception {
    GlassfishV1AppServerInstallation install = new GlassfishV1AppServerInstallation(host, serverDir, workingDir, config
        .appserverMajorVersion(), config.appserverMinorVersion());

    File installDir = install.serverBaseDir();

    // execute the equivalent of "ant -f setup.xml" if config directory doesn't exist
    if (!new File(installDir, "config").exists()) {
      File antScript = new File(installDir, "setup.xml");
      if (!antScript.isFile() || !antScript.canRead()) { throw new RuntimeException("missing ant script "
                                                                                    + antScript.getAbsolutePath()); }
      modifySetupXml(antScript);

      Project p = new Project();
      DefaultLogger consoleLogger = new DefaultLogger();
      consoleLogger.setErrorPrintStream(System.err);
      consoleLogger.setOutputPrintStream(System.out);
      consoleLogger.setMessageOutputLevel(Project.MSG_DEBUG);
      p.addBuildListener(consoleLogger);

      p.setUserProperty("ant.file", antScript.getAbsolutePath());
      p.init();
      ProjectHelper helper = ProjectHelper.getProjectHelper();
      p.addReference("ant.projectHelper", helper);
      helper.parse(p, antScript);

      p.executeTarget(p.getDefaultTarget());
    }

    return install;
  }

  private void modifySetupXml(File antScript) throws Exception {
    // make the "create.domain" target a NOOP in glassfish setup
    // Do this for two reasons, (1) It crashes on windows with long pathnames, (2) speed things up a little

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(antScript);

    NodeList list = document.getElementsByTagName("target");

    int numTargets = list.getLength();

    Node createDomainTarget = null;
    for (int i = 0; i < numTargets; i++) {
      Node inspect = list.item(i);
      Node nameAttr = inspect.getAttributes().getNamedItem("name");
      if (nameAttr != null) {
        if ("create.domain".equals(nameAttr.getNodeValue())) {
          createDomainTarget = inspect;
          break;
        }
      }
    }

    if (createDomainTarget == null) { throw new RuntimeException("Cannot find target in " + antScript.getAbsolutePath()); }

    while (createDomainTarget.getChildNodes().getLength() > 0) {
      createDomainTarget.removeChild(createDomainTarget.getChildNodes().item(0));
    }

    // Also workaround bug with long pathnames (https://glassfish.dev.java.net/issues/show_bug.cgi?id=2849)
    NodeList chmodTasks = document.getElementsByTagName("chmod");
    for (int i = 0; i < chmodTasks.getLength(); i++) {
      Element chmod = (Element) chmodTasks.item(i);
      chmod.setAttribute("parallel", "false");
    }

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();

    StringWriter sw = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(sw));

    FileUtils.writeStringToFile(antScript, sw.toString(), "UTF-8");
  }

  public AppServerInstallation createInstallation(File home, File workingDir) throws Exception {
    return new GlassfishV1AppServerInstallation(home, workingDir, config.appserverMajorVersion(), config
        .appserverMinorVersion());
  }

  public War createWar(String appName) {
    return new DtdWar(appName);
  }

  public StandardTerracottaAppServerConfig createTcConfig(File baseDir) {
    return new GlassfishV1AppServerConfig(baseDir);
  }
}
