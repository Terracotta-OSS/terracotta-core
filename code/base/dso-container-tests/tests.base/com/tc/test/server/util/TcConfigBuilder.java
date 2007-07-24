package com.tc.test.server.util;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.Loader;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.Include;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Module;
import com.terracottatech.config.QualifiedClassName;
import com.terracottatech.config.Root;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TcConfigBuilder {
  private TcConfigDocument tcConfigDocument;
  private TcConfig         tcConfig;
  private XmlOptions       xmlOptions;

  public TcConfigBuilder() {
    this("default-tc-config.xml");
  }

  public TcConfigBuilder(String resourcePath) {
    try {
      tcConfigDocument = new Loader().parse(getClass().getResourceAsStream(resourcePath));
      tcConfig = tcConfigDocument.getTcConfig();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TcConfigBuilder(File file) {
    try {
      tcConfigDocument = new Loader().parse(file);
      tcConfig = tcConfigDocument.getTcConfig();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private TcConfigBuilder(TcConfigDocument tcd) {
    tcConfigDocument = tcd;
    tcConfig = tcConfigDocument.getTcConfig();
  }

  public void setDsoPort(int portNo) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setDsoPort(portNo);
  }

  public int getDsoPort() {
    ensureServers();
    return tcConfig.getServers().getServerArray(0).getDsoPort();
  }

  public void setJmxPort(int portNo) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setJmxPort(portNo);
  }

  public int getJmxPort() {
    ensureServers();
    return tcConfig.getServers().getServerArray(0).getJmxPort();
  }

  public void setServerLogs(String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setLogs(path);
  }

  public void setServerData(String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setData(path);
  }

  public void setClientLogs(String path) {
    ensureClients();
    tcConfig.getClients().setLogs(path);
  }

  public void addAutoLock(String pattern, String lockLevel) {
    addAutoLock(pattern, lockLevel, false);
  }

  public void addAutoLock(String pattern, String lockLevel, boolean autoSynch) {
    ensureLocks();
    Autolock autoLock = tcConfig.getApplication().getDso().getLocks().insertNewAutolock(0);
    autoLock.setMethodExpression(pattern);
    autoLock.setLockLevel(LockLevel.Enum.forString(lockLevel));
    if (autoSynch) {
      autoLock.setAutoSynchronized(autoSynch);
    }
  }

  public void addRoot(String fieldName, String rootName) {
    ensureRoots();
    Root root = tcConfig.getApplication().getDso().getRoots().addNewRoot();
    root.setFieldName(fieldName);
    root.setRootName(rootName);
  }

  public void addInstrumentedClass(String pattern) {
    addInstrumentedClass(pattern, false);
  }

  public void addInstrumentedClass(String pattern, boolean honorTransient) {
    ensureInstrumentedClasses();
    Include include = tcConfig.getApplication().getDso().getInstrumentedClasses().insertNewInclude(0);
    include.setClassExpression(pattern);
    if (honorTransient) {
      include.setHonorTransient(honorTransient);
    }
  }

  public void addBootJarClass(String classname) {
    ensureBootJarClasses();
    QualifiedClassName qcn = tcConfig.getApplication().getDso().getAdditionalBootJarClasses().insertNewInclude(0);
    qcn.setStringValue(classname);
  }

  public void addExclude(String pattern) {
    ensureInstrumentedClasses();
    tcConfig.getApplication().getDso().getInstrumentedClasses().addExclude(pattern);
  }

  public void addModule(String name, String version) {
    ensureModules();
    Module newModule = tcConfig.getClients().getModules().insertNewModule(0);
    newModule.setName(name);
    newModule.setVersion(version);
  }

  public void addWebApplication(String appName) {
    addWebApplication(appName, false);
  }

  public void addWebApplication(String appName, boolean synchWrite) {
    ensureWebApplications();
    WebApplication wa = tcConfig.getApplication().getDso().getWebApplications().insertNewWebApplication(0);
    wa.setStringValue(appName);
    if (synchWrite) {
      wa.setSynchronousWrite(synchWrite);
    }
  }

  public String toString() {
    return tcConfigDocument.toString();
  }

  public void saveToFile(File filename) throws IOException {
    InputStream is = null;
    FileOutputStream fos = null;
    try {
      is = tcConfigDocument.newInputStream(getXmlOptions());
      fos = new FileOutputStream(filename);
      IOUtils.copy(tcConfigDocument.newInputStream(getXmlOptions()), fos);
    } finally {
      IOUtils.closeQuietly(fos);
      IOUtils.closeQuietly(is);
    }
  }

  private XmlOptions getXmlOptions() {
    if (xmlOptions == null) {
      xmlOptions = new XmlOptions();
      xmlOptions.setLoadLineNumbers();
      xmlOptions.setSavePrettyPrint();
      xmlOptions.setSavePrettyPrintIndent(2);
    }
    return xmlOptions;
  }

  private void ensureServers() {
    if (!tcConfig.isSetServers()) {
      tcConfig.addNewServers();
    }
  }

  private void ensureClients() {
    if (!tcConfig.isSetClients()) {
      tcConfig.addNewClients();
    }
  }

  private void ensureModules() {
    ensureClients();
    if (!tcConfig.getClients().isSetModules()) {
      tcConfig.getClients().addNewModules();
    }
  }

  private void ensureApplication() {
    if (!tcConfig.isSetApplication()) {
      tcConfig.addNewApplication();
    }
  }

  private void ensureDso() {
    ensureApplication();
    if (!tcConfig.getApplication().isSetDso()) {
      tcConfig.getApplication().addNewDso();
    }
  }

  private void ensureLocks() {
    ensureDso();
    if (!tcConfig.getApplication().getDso().isSetLocks()) {
      tcConfig.getApplication().getDso().addNewLocks();
    }
  }

  private void ensureRoots() {
    ensureDso();
    if (!tcConfig.getApplication().getDso().isSetRoots()) {
      tcConfig.getApplication().getDso().addNewRoots();
    }
  }

  private void ensureInstrumentedClasses() {
    ensureDso();
    if (!tcConfig.getApplication().getDso().isSetInstrumentedClasses()) {
      tcConfig.getApplication().getDso().addNewInstrumentedClasses();
    }
  }

  private void ensureBootJarClasses() {
    ensureDso();
    if (!tcConfig.getApplication().getDso().isSetAdditionalBootJarClasses()) {
      tcConfig.getApplication().getDso().addNewAdditionalBootJarClasses();
    }
  }

  private void ensureWebApplications() {
    ensureDso();
    if (!tcConfig.getApplication().getDso().isSetWebApplications()) {
      tcConfig.getApplication().getDso().addNewWebApplications();
    }
  }

  public TcConfigBuilder copy() {
    try {
      TcConfigBuilder aCopy = new TcConfigBuilder(new Loader().parse(this.toString()));
      return aCopy;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    TcConfigBuilder tc = new TcConfigBuilder();
    tc.setDsoPort(3232);
    tc.addModule("tc", "1.2");
    tc.addModule("asdfa", "23432");
    tc.setServerData("c:/temp");
    tc.setClientLogs("c:/temp/logs");
    tc.addAutoLock("* com.tctest.*.*(..)", "write", true);
    tc.addAutoLock("* adfad..*()", "read");
    tc.addRoot("com.tc.Test.field", "myField");
    tc.addWebApplication("events", false);
    tc.addBootJarClass("java.lang.Local");
    TcConfigBuilder aCopy = tc.copy();
    aCopy.addModule("hung", "huynh");
    System.out.println(aCopy.toString());
  }

}
