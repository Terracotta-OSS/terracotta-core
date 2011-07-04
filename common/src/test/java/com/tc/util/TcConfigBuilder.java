package com.tc.util;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.Loader;
import com.terracottatech.config.AppGroup;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.Include;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Module;
import com.terracottatech.config.Property;
import com.terracottatech.config.QualifiedClassName;
import com.terracottatech.config.Root;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TcConfigBuilder {

  private final TcConfigDocument tcConfigDocument;
  private final TcConfig         tcConfig;
  private XmlOptions             xmlOptions;
  private File                   tcConfigFile = new File("tc-config.xml");

  public File getTcConfigFile() {
    return tcConfigFile;
  }

  public void setTcConfigFile(File tcConfigFile) {
    this.tcConfigFile = tcConfigFile;
  }

  public TcConfigBuilder() {
    this("tcconfigbuilder-default.xml");
  }

  public TcConfigBuilder(String resourcePath) {
    InputStream in = TcConfigBuilder.class.getResourceAsStream(resourcePath);
    if (in == null) { throw new IllegalArgumentException("no resource available for " + resourcePath); }

    try {
      tcConfigDocument = new Loader().parse(in);
      tcConfig = tcConfigDocument.getTcConfig();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TcConfigBuilder(File file) {
    try {
      tcConfigDocument = new Loader().parse(file);
      tcConfig = tcConfigDocument.getTcConfig();
      tcConfigFile = file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private TcConfigBuilder(TcConfigDocument tcd) {
    tcConfigDocument = tcd;
    tcConfig = tcConfigDocument.getTcConfig();
  }

  public Server[] getServers() {
    return tcConfig.getServers().getServerArray();
  }

  public InputStream newInputStream() {
    return tcConfigDocument.newInputStream(getXmlOptions());
  }

  public void setDsoHost(String host) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setHost(host);
  }

  public String getDsoHost() {
    ensureServers();
    return tcConfig.getServers().getServerArray(0).getHost();
  }

  public void setDsoPort(int portNo) {
    setDsoPort(0, portNo);
  }

  public void setDsoPort(int serverIndex, int portNo) {
    ensureServers();
    Server server = tcConfig.getServers().getServerArray(serverIndex);
    Assert.assertNotNull(server);
    if (!server.isSetDsoPort()) {
      server.addNewDsoPort();
    }
    server.getDsoPort().setIntValue(portNo);
  }

  public void setGroupPort(int portNo) {
    setGroupPort(0, portNo);
  }

  public void setGroupPort(int serverIndex, int portNo) {
    ensureServers();
    Server server = tcConfig.getServers().getServerArray(serverIndex);
    Assert.assertNotNull(server);
    if (!server.isSetL2GroupPort()) {
      server.addNewL2GroupPort();
    }
    server.getL2GroupPort().setIntValue(portNo);
  }

  public int getDsoPort() {
    return getDsoPort(0);
  }

  public int getDsoPort(int serverIndex) {
    ensureServers();
    Assert.assertNotNull(tcConfig.getServers().getServerArray(serverIndex));
    return tcConfig.getServers().getServerArray(serverIndex).getDsoPort().getIntValue();
  }

  public int getGroupPort() {
    return getGroupPort(0);
  }

  public int getGroupPort(int serverIndex) {
    ensureServers();
    Assert.assertNotNull(tcConfig.getServers().getServerArray(serverIndex));
    return tcConfig.getServers().getServerArray(serverIndex).getL2GroupPort().getIntValue();
  }

  public void setJmxPort(int portNo) {
    setJmxPort(0, portNo);
  }

  public void setJmxPort(int serverIndex, int portNo) {
    ensureServers();
    Server server = tcConfig.getServers().getServerArray(serverIndex);
    Assert.assertNotNull(server);
    if (!server.isSetJmxPort()) {
      server.addNewJmxPort();
    }
    server.getJmxPort().setIntValue(portNo);
  }

  public int getJmxPort() {
    return getJmxPort(0);
  }

  public int getJmxPort(int serverIndex) {
    ensureServers();
    Assert.assertNotNull(tcConfig.getServers().getServerArray(serverIndex));
    return tcConfig.getServers().getServerArray(serverIndex).getJmxPort().getIntValue();
  }

  public void addProperty(String key, String value) {
    ensureTcProprties();
    Property tcProps = tcConfig.getTcProperties().addNewProperty();
    tcProps.setName(key);
    tcProps.setValue(value);
  }

  public void setServerLogs(String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setLogs(path);
  }

  public void setServerData(String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setData(path);
  }

  public void setServerStatistics(String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(0).setStatistics(path);
  }

  public void setServerLogs(int index, String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(index).setLogs(path);
  }

  public void setServerData(int index, String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(index).setData(path);
  }

  public void setServerStatistics(int index, String path) {
    ensureServers();
    tcConfig.getServers().getServerArray(index).setStatistics(path);
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

  public void addModule(String name, String groupId, String version) {
    ensureModules();
    Module newModule = tcConfig.getClients().getModules().insertNewModule(0);
    newModule.setName(name);
    newModule.setGroupId(groupId);
    newModule.setVersion(version);
  }

  public void addModule(String name, String version) {
    addModule(name, "org.terracotta.modules", version);
  }

  public void addRepository(String location) {
    ensureModules();
    tcConfig.getClients().getModules().addRepository(location);
  }

  public void addAppGroup(String name, String[] namedClassLoaders, String[] webAppNames) {
    ensureAppGroups();
    AppGroup ag = tcConfig.getApplication().getDso().getAppGroups().addNewAppGroup();
    ag.setName(name);
    if (namedClassLoaders != null) {
      ag.setNamedClassloaderArray(namedClassLoaders);
    }
    if (webAppNames != null) {
      ag.setWebApplicationArray(webAppNames);
    }
  }

  /**
   * Adds web-application with default values for synchronous-write (false) and session-locking (true)
   */
  public void addWebApplication(String appName) {
    addWebApplication(appName, null, null, null);
  }

  /**
   * Adds web-application with default value for synchronous-write (false) and session-locking = true
   */
  public void addWebApplicationWithSessionLocking(String appName) {
    addWebApplication(appName, null, true, null);
  }

  /**
   * Adds web-application with default value for synchronous-write (false) and session-locking = false
   */
  public void addWebApplicationWithoutSessionLocking(String appName) {
    addWebApplication(appName, null, false, null);
  }

  /**
   * Adds web-application with synchronous-write = true and default value for session-locking (true)
   */
  public void addWebApplicationWithSynchronousWrite(String appName) {
    addWebApplication(appName, true, null, null);
  }

  /**
   * Adds web-application with synchronous-write = false and default value for session-locking (true)
   */
  public void addWebApplicationWithoutSynchronousWrite(String appName) {
    addWebApplication(appName, false, null, null);
  }

  /**
   * Adds web-application with specified serialization setting
   */
  public void addWebApplicationSerialized(String appName, boolean serialized) {
    addWebApplication(appName, null, null, serialized);
  }

  public void addWebApplication(String appName, Boolean synchWrite, Boolean sessionLocking, Boolean serialization) {
    ensureWebApplications();
    WebApplication wa = tcConfig.getApplication().getDso().getWebApplications().insertNewWebApplication(0);
    wa.setStringValue(appName);

    // null means to not set the attributed at all (falls back to schema defaults)
    if (synchWrite != null) {
      wa.setSynchronousWrite(synchWrite);
    }

    if (sessionLocking != null) {
      wa.setSessionLocking(sessionLocking);
    }

    if (serialization != null) {
      wa.setSerialization(serialization);
    }
  }

  public void randomizePorts() {
    PortChooser pc = new PortChooser();
    Server[] servers = getServers();
    for (Server server : servers) {
      if (!server.isSetDsoPort()) server.addNewDsoPort();
      server.getDsoPort().setIntValue(pc.chooseRandomPort());
      
      if(!server.isSetJmxPort()) server.addNewJmxPort();
      server.getJmxPort().setIntValue(pc.chooseRandomPort());
      
      if(!server.isSetL2GroupPort()) server.addNewL2GroupPort();
      server.getL2GroupPort().setIntValue(pc.chooseRandomPort());
    }
  }

  @Override
  public String toString() {
    return tcConfigDocument.toString();
  }

  public void saveToFile() throws IOException {
    InputStream is = null;
    FileOutputStream fos = null;
    try {
      is = tcConfigDocument.newInputStream(getXmlOptions());
      fos = new FileOutputStream(tcConfigFile);
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

  private void ensureTcProprties() {
    if (!tcConfig.isSetTcProperties()) {
      tcConfig.addNewTcProperties();
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

  private void ensureAppGroups() {
    ensureDso();
    if (!tcConfig.getApplication().getDso().isSetAppGroups()) {
      tcConfig.getApplication().getDso().addNewAppGroups();
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
}
