/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.terracottatech.config.Authentication;
import com.terracottatech.config.Server;

import java.io.File;

import javax.xml.namespace.QName;

/**
 * The standard implementation of {@link NewCommonL2Config}.
 */
public class NewCommonL2ConfigObject extends BaseNewConfigObject implements NewCommonL2Config {

  private final FileConfigItem   dataPath;
  private final FileConfigItem   logsPath;
  private final IntConfigItem    jmxPort;
  private final StringConfigItem host;
  private final boolean          authentication;
  private final String           passwordFile;
  private final String           accessFile;

  public NewCommonL2ConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);

    this.dataPath = context.configRelativeSubstitutedFileItem("data");
    this.logsPath = context.configRelativeSubstitutedFileItem("logs");
    this.jmxPort = context.intItem("jmx-port");
    this.host = context.stringItem("@host");

    String pwd = null;
    String access = null;
    Server server = (Server) context.bean();
    if (server != null) {
      this.authentication = server.isSetAuthentication();
    } else {
      this.authentication = false;
    }

    if (authentication) {
      pwd = server.getAuthentication().getPasswordFile();
      if (pwd == null) pwd = Authentication.type.getElementProperty(QName.valueOf("password-file")).getDefaultText();
      pwd = new File(ParameterSubstituter.substitute(pwd)).getAbsolutePath();
      access = new File(server.getAuthentication().getAccessFile()).getAbsolutePath();
      if (access == null) access = Authentication.type.getElementProperty(QName.valueOf("access-file"))
          .getDefaultText();
      access = ParameterSubstituter.substitute(access);
    }
    this.passwordFile = pwd;
    this.accessFile = access;
  }

  public FileConfigItem dataPath() {
    return this.dataPath;
  }

  public FileConfigItem logsPath() {
    return this.logsPath;
  }

  public IntConfigItem jmxPort() {
    return this.jmxPort;
  }

  public StringConfigItem host() {
    return this.host;
  }

  public boolean authentication() {
    return authentication;
  }

  public String authenticationAccessFile() {
    return accessFile;
  }

  public String authenticationPasswordFile() {
    return passwordFile;
  }
}
