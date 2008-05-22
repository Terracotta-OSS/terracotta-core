/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.glassfish;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.StandardAppServerParameters;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public abstract class AbstractGlassfishAppServerFactory extends AppServerFactory {

  // This class may only be instantiated by its parent which contains the ProtectedKey
  public AbstractGlassfishAppServerFactory(ProtectedKey protectedKey) {
    super(protectedKey);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new StandardAppServerParameters(instanceName, props);
  }

  public abstract AppServer createAppServer(AppServerInstallation installation);

  private void doSetup(GlassfishAppServerInstallation install) throws IOException, Exception {
    File installDir = install.serverInstallDirectory();
    File configDir = new File(installDir, "config");
    File domainsDir = new File(installDir, "domains");

    // These directories should be cleaned for each run since there is VM specific information baked into files here
    FileUtils.deleteDirectory(configDir);
    FileUtils.deleteDirectory(domainsDir);

    // execute the equivalent of "ant -f setup.xml"
    File antScript = new File(installDir, "setup.xml");
    if (!antScript.isFile() || !antScript.canRead()) { throw new RuntimeException("missing ant script "
                                                                                  + antScript.getAbsolutePath()); }
    modifySetupXml(antScript);

    Project p = new Project();
    DefaultLogger consoleLogger = new DefaultLogger();
    consoleLogger.setErrorPrintStream(System.err);
    consoleLogger.setOutputPrintStream(System.out);
    consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
    // consoleLogger.setMessageOutputLevel(Project.MSG_DEBUG);
    p.addBuildListener(consoleLogger);

    p.setUserProperty("ant.file", antScript.getAbsolutePath());
    p.init();
    ProjectHelper helper = ProjectHelper.getProjectHelper();
    p.addReference("ant.projectHelper", helper);
    helper.parse(p, antScript);

    p.executeTarget(p.getDefaultTarget());
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

  public AppServerInstallation createInstallation(File home, File workingDir, AppServerInfo appServerInfo)
      throws Exception {
    GlassfishAppServerInstallation install = new GlassfishAppServerInstallation(home, workingDir, appServerInfo);
    doSetup(install);
    return install;
  }
}
