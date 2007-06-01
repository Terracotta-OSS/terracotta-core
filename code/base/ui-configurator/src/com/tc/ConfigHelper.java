/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.Loader;
import com.tc.config.schema.migrate.ConfigUpdateException;
import com.tc.object.tools.BootJarSignature;
import com.tc.pattern.PatternHelper;
import com.tc.servers.ServerSelection;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Application;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.WebApplications;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/*
 * TODO: factor out a BaseConfigurationHelper that is shared with the Eclipse plugin's
 * ConfigurationHelper and anyone else that needs basic logic about the config, such as
 * isTransient(String fieldName).
 */

public class ConfigHelper {
  private Loader              m_configLoader;
  private XmlOptions          m_xmlOptions;
  private File                m_configFile;
  private TcConfig            m_config;

  private static final String TC_INSTALL_DIR       = SessionIntegratorFrame.getTCInstallDir();

  private static final String TOMCAT_SANDBOX       = SessionIntegratorFrame.getSandBoxRoot();

  private static final String FS                   = System.getProperty("file.separator");

  private static final String CUSTOM_BOOT_JAR_PATH = TC_INSTALL_DIR + FS + "lib" + FS + "dso-boot" + FS
                                                     + getBootJarNameForThisVM();

  private static final int    DEFAULT_JMX_PORT     = 9520;

  public ConfigHelper() {
    super();
  }

  public ConfigHelper(ServerSelection selection) {
    super();

    String serverName = selection.getSelectedServer().getName();

    m_configLoader = new Loader();
    m_xmlOptions = createXmlOptions();
    m_configFile = new File(TOMCAT_SANDBOX + FS + serverName + FS + "tc-config.xml");

    testUpdateConfig();
  }

  private void testUpdateConfig() {
    try {
      if (m_configFile.exists()) {
        if (m_configLoader.testIsOld(m_configFile)) {
          m_configLoader.updateToCurrent(m_configFile);
        }
      }
    } catch (ConfigUpdateException cue) {
      // TODO: we need to handle this
    } catch (Exception e) {/**/
    }
  }

  public String getConfigFilePath() {
    return m_configFile.getAbsolutePath();
  }

  public File getConfigFile() {
    return m_configFile;
  }

  public TcConfig getConfig() {
    return m_config;
  }

  public void setConfig(TcConfig newConfig) {
    m_config = newConfig;
  }

  public TcConfig ensureConfig() {
    TcConfig config = getConfig();

    if (config == null) {
      try {
        config = load();
      } catch (Exception e) {
        m_config = TcConfig.Factory.newInstance();
      }
    }

    return config;
  }

  public int getJmxPort() {
    TcConfig config = ensureConfig();
    Servers servers = config.getServers();

    if (servers == null) {
      servers = config.addNewServers();
    }

    if (servers.sizeOfServerArray() == 0) {
      servers.addNewServer();
      save();
    }

    Server server = servers.getServerArray(0);
    int port = server.isSetJmxPort() ? server.getJmxPort() : DEFAULT_JMX_PORT;

    return port;
  }

  public DsoApplication getDsoApplication() {
    TcConfig config = getConfig();

    if (config != null) {
      Application app = config.getApplication();
      return app != null ? app.getDso() : null;
    }

    return null;
  }

  public DsoApplication ensureDsoApplication() {
    DsoApplication dsoApp = null;
    TcConfig config = ensureConfig();

    if (config != null) {
      Application app = config.getApplication();

      if (app == null) {
        app = config.addNewApplication();
      }

      if ((dsoApp = app.getDso()) == null) {
        dsoApp = app.addNewDso();
        dsoApp.addNewInstrumentedClasses();
      }
    }

    return dsoApp;
  }

  public TcConfig load() throws Exception {
    File configFile = getConfigFile();
    TcConfigDocument configDoc;

    configDoc = m_configLoader.parse(configFile, m_xmlOptions);
    m_config = configDoc.getTcConfig();

    return m_config;
  }

  public List validate(String xmlText) throws IOException, XmlException {
    TcConfigDocument configDoc = m_configLoader.parse(xmlText, m_xmlOptions);
    TcConfig config = configDoc.getTcConfig();
    List errors = new ArrayList();

    if (config != null) {
      m_xmlOptions.setErrorListener(errors);
      configDoc.validate(m_xmlOptions);
      m_xmlOptions.setErrorListener(null);
    }

    return errors;
  }

  public void save() {
    TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
    InputStream inStream = null;
    OutputStream outStream = null;

    try {
      if (m_config != null) {
        configDoc.setTcConfig(m_config);

        inStream = configDoc.newInputStream(getXmlOptions());
        outStream = new FileOutputStream(m_configFile);

        CopyUtils.copy(inStream, outStream);
      }
    } catch (Exception e) {
      openError("Error saving '" + m_configFile.getName() + "'", e);
    } finally {
      IOUtils.closeQuietly(inStream);
      IOUtils.closeQuietly(outStream);
    }
  }

  public String getConfigText() {
    TcConfig config = getConfig();
    InputStream inStream = null;
    String text = null;

    try {
      if (config != null) {
        TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();

        configDoc.setTcConfig(m_config);

        inStream = configDoc.newInputStream(getXmlOptions());
        text = IOUtils.toString(inStream);
      }
    } catch (Exception e) {
      openError("Error getting config text", e);
    } finally {
      IOUtils.closeQuietly(inStream);
    }

    return text;
  }

  public void save(String xmlText) {
    TcConfigDocument configDoc = null;
    InputStream inStream = null;
    OutputStream outStream = null;

    try {
      configDoc = m_configLoader.parse(xmlText, m_xmlOptions);
      m_config = configDoc.getTcConfig();

      if (m_config != null) {
        inStream = configDoc.newInputStream(getXmlOptions());
        outStream = new FileOutputStream(m_configFile);

        CopyUtils.copy(inStream, outStream);
      }
    } catch (Exception e) {
      openError("Error saving '" + m_configFile.getName() + "'", e);
    } finally {
      IOUtils.closeQuietly(inStream);
      IOUtils.closeQuietly(outStream);
    }
  }

  public void saveAs(File file, String xmlText) {
    TcConfigDocument configDoc = null;
    InputStream inStream = null;
    OutputStream outStream = null;

    try {
      configDoc = m_configLoader.parse(xmlText, m_xmlOptions);
      m_config = configDoc.getTcConfig();

      if (m_config != null) {
        inStream = configDoc.newInputStream(getXmlOptions());
        outStream = new FileOutputStream(file);

        CopyUtils.copy(inStream, outStream);
      }
    } catch (Exception e) {
      openError("Error saving '" + file.getName() + "'", e);
    } finally {
      IOUtils.closeQuietly(inStream);
      IOUtils.closeQuietly(outStream);
    }
  }

  public void openError(final String msg, final Throwable t) {
    if (SwingUtilities.isEventDispatchThread()) {
      ErrorDialog d = new ErrorDialog(msg, t);
      d.setVisible(true);
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ErrorDialog d = new ErrorDialog(msg, t);
          d.setVisible(true);
        }
      });
    }
  }

  public static String getCustomBootJarPath() {
    return CUSTOM_BOOT_JAR_PATH;
  }

  public static File getCustomBootJarFile() {
    return new File(getCustomBootJarPath());
  }

  public boolean ensureWebApplication(String name) {
    DsoApplication dsoApp = ensureDsoApplication();

    if (dsoApp != null) {
      WebApplications apps = dsoApp.getWebApplications();

      if (apps == null) {
        apps = dsoApp.addNewWebApplications();
      }

      WebApplication[] webApps = apps.getWebApplicationArray();
      for (int i = 0; i < webApps.length; i++) {
        if (webApps[i].getStringValue().equals(name)) { return false; }
      }

      WebApplication webApp = apps.addNewWebApplication();
      webApp.setStringValue(name);

      return true;
    }

    return false;
  }

  public boolean removeWebApplication(String name) {
    DsoApplication dsoApp = ensureDsoApplication();

    if (dsoApp != null) {
      WebApplications apps = dsoApp.getWebApplications();

      if (apps != null) {
        WebApplication[] appNames = apps.getWebApplicationArray();

        for (int i = 0; i < appNames.length; i++) {
          if (appNames[i].getStringValue().equals(name)) {
            apps.removeWebApplication(i);
            return true;
          }
        }
      }
    }

    return false;
  }

  public InstrumentedClasses ensureInstrumentedClasses() {
    DsoApplication dsoApp = ensureDsoApplication();
    if (!dsoApp.isSetInstrumentedClasses()) {
      dsoApp.addNewInstrumentedClasses();
    }
    return dsoApp.getInstrumentedClasses();
  }

  public boolean isAdaptable(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    int size = classes.sizeOfIncludeArray();
    Include include;
    String expr;

    for (int i = 0; i < size; i++) {
      include = classes.getIncludeArray(i);
      expr = include.getClassExpression();

      if (PatternHelper.getHelper().matchesClass(expr, classExpr)) { return true; }
    }

    return false;
  }

  public void ensureAdaptable(String classExpr) {
    if (!isAdaptable(classExpr)) {
      ensureInstrumentedClasses().addNewInclude().setClassExpression(classExpr);
    }
  }

  public Include includeRuleFor(String classExpr) {
    TcConfig config = getConfig();

    if (config != null) {
      InstrumentedClasses classes = ensureInstrumentedClasses();

      if (classes != null) {
        XmlObject[] objects = classes.selectPath("*");

        if (objects != null && objects.length > 0) {
          for (int i = objects.length - 1; i >= 0; i--) {
            XmlObject object = objects[i];

            if (object instanceof Include) {
              String expr = ((Include) object).getClassExpression();
              if (expr != null && expr.equals(classExpr)) { return (Include) object; }
            }
          }
        }
      }
    }

    return null;
  }

  public Include ensureIncludeRuleFor(String classExpr) {
    Include include = includeRuleFor(classExpr);

    if (include == null) {
      include = ensureInstrumentedClasses().addNewInclude();
      include.setClassExpression(classExpr);
    }

    return include;
  }

  public void ensureNotAdaptable(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    int size = classes.sizeOfIncludeArray();
    Include include;
    String expr;

    for (int i = size - 1; i >= 0; i--) {
      include = classes.getIncludeArray(i);
      expr = include.getClassExpression();

      if (PatternHelper.getHelper().matchesClass(expr, classExpr)) {
        classes.removeInclude(i);
      }
    }
  }

  public TransientFields ensureTransientFields() {
    DsoApplication dsoApp = ensureDsoApplication();
    if (!dsoApp.isSetTransientFields()) {
      dsoApp.addNewTransientFields();
    }
    return dsoApp.getTransientFields();
  }

  private TransientFields getTransientFields() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getTransientFields() : null;
  }

  public boolean isTransient(String fieldName) {
    TcConfig config = getConfig();

    if (config != null) {
      TransientFields transients = getTransientFields();

      if (transients != null) {
        for (int i = 0; i < transients.sizeOfFieldNameArray(); i++) {
          if (fieldName.equals(transients.getFieldNameArray(i))) { return true; }
        }
      }
    }

    return false;
  }

  public void ensureTransient(String fieldName) {
    if (!isTransient(fieldName)) {
      ensureTransientFields().addFieldName(fieldName);
    }
  }

  public void ensureNotTransient(String fieldName) {
    TransientFields transients = ensureTransientFields();
    int count = transients.sizeOfFieldNameArray();

    for (int i = 0; i < count; i++) {
      if (fieldName.equals(transients.getFieldNameArray(i))) {
        transients.removeFieldName(i);
        return;
      }
    }
  }

  public AdditionalBootJarClasses ensureAdditionalBootJarClasses() {
    DsoApplication dsoApp = ensureDsoApplication();
    if (!dsoApp.isSetAdditionalBootJarClasses()) {
      dsoApp.addNewAdditionalBootJarClasses();
    }
    return dsoApp.getAdditionalBootJarClasses();
  }

  private AdditionalBootJarClasses getAdditionalBootJarClasses() {
    DsoApplication dsoApp = getDsoApplication();
    return dsoApp != null ? dsoApp.getAdditionalBootJarClasses() : null;
  }

  public boolean isBootJarClass(String className) {
    AdditionalBootJarClasses classes = getAdditionalBootJarClasses();

    if (classes != null) {
      String[] includes = classes.getIncludeArray();

      for (int i = 0; i < includes.length; i++) {
        if (PatternHelper.getHelper().matchesClass(includes[i], className)) { return true; }
      }
    }

    return false;
  }

  public void ensureBootJarClass(String typeName) {
    if (!isBootJarClass(typeName)) {
      ensureAdditionalBootJarClasses().addInclude(typeName);
    }
  }

  public void ensureNotBootJarClass(String typeName) {
    AdditionalBootJarClasses bootClasses = getAdditionalBootJarClasses();

    if (bootClasses != null) {
      int count = bootClasses.sizeOfIncludeArray();

      for (int i = 0; i < count; i++) {
        if (typeName.equals(bootClasses.getIncludeArray(i))) {
          bootClasses.removeInclude(i);
          return;
        }
      }
    }
  }

  private XmlOptions createXmlOptions() {
    XmlOptions opts = new XmlOptions();

    opts.setLoadLineNumbers();
    opts.setSavePrettyPrint();
    opts.setSavePrettyPrintIndent(2);
    opts.remove(XmlOptions.LOAD_STRIP_WHITESPACE);
    opts.remove(XmlOptions.LOAD_STRIP_COMMENTS);
    opts.remove(XmlOptions.VALIDATE_ON_SET);

    return opts;
  }

  public XmlOptions getXmlOptions() {
    return m_xmlOptions;
  }

  private static String getBootJarNameForThisVM() {
    try {
      return BootJarSignature.getBootJarNameForThisVM();
    } catch (Exception e) {
      return "dso-boot.jar";
    }
  }
}
