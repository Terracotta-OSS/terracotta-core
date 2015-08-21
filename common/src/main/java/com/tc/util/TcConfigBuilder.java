package com.tc.util;

import org.apache.commons.io.IOUtils;
import org.terracotta.config.BindPort;
import org.terracotta.config.Property;
import org.terracotta.config.Server;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.TcProperties;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class TcConfigBuilder {

  private final TcConfiguration tcConfigDocument;
  private final TcConfig tcConfig;
  private File tcConfigFile = new File("tc-config.xml");

  public File getTcConfigFile() {
    return tcConfigFile;
  }

  public void setTcConfigFile(File tcConfigFile) {
    this.tcConfigFile = tcConfigFile;
  }

  public TcConfigBuilder() {
    this("tcconfigbuilder-default.xml");
  }

  public TcConfigBuilder(String resourcePath, String workDir) {
    InputStream in = TcConfigBuilder.class.getResourceAsStream(resourcePath);
    if (in == null) { throw new IllegalArgumentException("no resource available for " + resourcePath); }

    try {
      tcConfigDocument = TCConfigurationParser.parse(in, new ArrayList<>(), workDir);
      tcConfig = tcConfigDocument.getPlatformConfiguration();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TcConfigBuilder(String resourcePath) {
    URL in = TcConfigBuilder.class.getResource(resourcePath);
    if (in == null) { throw new IllegalArgumentException("no resource available for " + resourcePath); }

    try {
      File file = new File(in.getPath());
      tcConfigDocument = TCConfigurationParser.parse(file);
      tcConfig = tcConfigDocument.getPlatformConfiguration();
      tcConfigFile = file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TcConfigBuilder(File file) {
    try {
      tcConfigDocument = TCConfigurationParser.parse(file);
      tcConfig = tcConfigDocument.getPlatformConfiguration();
      tcConfigFile = file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TcConfigBuilder(TcConfiguration tcd) {
    tcConfigDocument = tcd;
    tcConfig = tcConfigDocument.getPlatformConfiguration();
  }

  public Server[] getServers() {
    return tcConfig.getServers().getServer().toArray(new Server[tcConfig.getServers().getServer().size()]);
  }

  public Server getServer(int index) {
    return getServers()[index];
  }

  public InputStream newInputStream() {
    return new ByteArrayInputStream(tcConfigDocument.toString().getBytes());
  }

  public void setTsaPort(int portNo) {
    setTsaPort(0, portNo);
  }

  public void setTsaPort(int serverIndex, int portNo) {
    Server server = getServer(serverIndex);
    Assert.assertNotNull(server);
    if (server.getTsaPort() == null) {
      server.setTsaPort(new BindPort());
    }
    server.getTsaPort().setValue(portNo);
  }

  public void setGroupPort(int portNo) {
    setGroupPort(0, portNo);
  }

  public void setGroupPort(int serverIndex, int portNo) {
    Server server = getServer(serverIndex);
    Assert.assertNotNull(server);
    if (server.getTsaGroupPort() == null) {
      server.setTsaGroupPort(new BindPort());
    }
    server.getTsaGroupPort().setValue(portNo);
  }

  public int getTsaPort() {
    return getTsaPort(0);
  }

  public int getTsaPort(int serverIndex) {
    Assert.assertNotNull(getServer(serverIndex));
    return getServer(serverIndex).getTsaPort().getValue();
  }

  public int getManagementPort() {
    return getManagementPort(0);
  }

  public int getManagementPort(int serverIndex) {
    Assert.assertNotNull(getServer(serverIndex));
    return getServer(serverIndex).getManagementPort().getValue();
  }

  public int getGroupPort() {
    return getGroupPort(0);
  }

  public int getGroupPort(int serverIndex) {
    Assert.assertNotNull(getServer(serverIndex));
    return getServer(serverIndex).getTsaGroupPort().getValue();
  }

  public void setManagementPort(int portNo) {
    setManagementPort(0, portNo);
  }

  public void setManagementPort(int serverIndex, int portNo) {
    Server server = getServer(serverIndex);
    Assert.assertNotNull(server);
    if (server.getManagementPort() == null) {
      server.setManagementPort(new BindPort());
    }
    server.getManagementPort().setValue(portNo);
  }


  public void addProperty(String key, String value) {
    Property tcProp = new Property();
    tcProp.setName(key);
    tcProp.setValue(value);
    if(tcConfig.getTcProperties() == null) {
      tcConfig.setTcProperties(new TcProperties());
    }
    tcConfig.getTcProperties().getProperty().add(tcProp);
  }


  public void randomizePorts() {
    PortChooser pc = new PortChooser();
    Server[] servers = getServers();
    for (Server server : servers) {
      if (server.getTsaPort() == null) server.setTsaPort(new BindPort());
      server.getTsaPort().setValue(pc.chooseRandomPort());
      if (server.getTsaGroupPort() == null) server.setTsaGroupPort(new BindPort());
      server.getTsaGroupPort().setValue(pc.chooseRandomPort());

      if (server.getManagementPort() == null) server.setManagementPort(new BindPort());
      server.getManagementPort().setValue(pc.chooseRandomPort());
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
      is = newInputStream();
      fos = new FileOutputStream(tcConfigFile);
      IOUtils.copy(is, fos);
    } finally {
      IOUtils.closeQuietly(fos);
      IOUtils.closeQuietly(is);
    }
  }


  public TcConfigBuilder copy() {
    try {
      TcConfigBuilder aCopy = new TcConfigBuilder(TCConfigurationParser.parse(this.tcConfigFile));
      return aCopy;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
