/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.tc.license.LicenseCheck;
import com.tc.license.util.LicenseConstants;
import com.terracottatech.config.Authentication;
import com.terracottatech.config.AuthenticationMode;
import com.terracottatech.config.HttpAuthentication;
import com.terracottatech.config.Server;

import java.io.File;

import javax.xml.namespace.QName;

/**
 * The standard implementation of {@link NewCommonL2Config}.
 */
public class NewCommonL2ConfigObject extends BaseNewConfigObject implements NewCommonL2Config {

  private final FileConfigItem   dataPath;
  private final FileConfigItem   logsPath;
  private final FileConfigItem   serverDbBackupPath;
  private final FileConfigItem   statisticsPath;
  private final IntConfigItem    jmxPort;
  private final StringConfigItem host;
  private final boolean          authentication;
  private final String           passwordFile;
  private final String           loginConfigName;
  private final String           accessFile;
  private final boolean          httpAuthentication;
  private final String           userRealmFile;

  public NewCommonL2ConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);

    this.dataPath = context.configRelativeSubstitutedFileItem("data");
    this.logsPath = context.configRelativeSubstitutedFileItem("logs");

    this.serverDbBackupPath = context.configRelativeSubstitutedFileItem("data-backup");

    this.statisticsPath = context.configRelativeSubstitutedFileItem("statistics");
    int tempJmxPort = context.intItem("dso-port").getInt() + NewCommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= NewCommonL2Config.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % NewCommonL2Config.MAX_PORTNUMBER) + NewCommonL2Config.MIN_PORTNUMBER);
    this.jmxPort = context.intItem("jmx-port", defaultJmxPort, true);
    this.host = context.stringItem("@host");

    // JMX authentication
    String pwd = null;
    String loginConfig = null;
    String access = null;
    Server server = (Server) context.bean();
    if (server != null) {
      this.authentication = server.isSetAuthentication();
    } else {
      this.authentication = false;
    }

    if (authentication) {
      LicenseCheck.checkCapability(LicenseConstants.AUTHENTICATION);
      if (server.getAuthentication().isSetMode()) {
        if (server.getAuthentication().getMode().isSetLoginConfigName()) {
          loginConfig = server.getAuthentication().getMode().getLoginConfigName();
        } else {
          pwd = server.getAuthentication().getMode().getPasswordFile();
          if (pwd == null) pwd = AuthenticationMode.type.getElementProperty(QName.valueOf("password-file"))
              .getDefaultText();
          pwd = new File(ParameterSubstituter.substitute(pwd)).getAbsolutePath();
        }
      } else {
        pwd = AuthenticationMode.type.getElementProperty(QName.valueOf("password-file")).getDefaultText();
        pwd = new File(ParameterSubstituter.substitute(pwd)).getAbsolutePath();
      }
      access = server.getAuthentication().getAccessFile();
      if (access == null) access = Authentication.type.getElementProperty(QName.valueOf("access-file"))
          .getDefaultText();
      access = new File(ParameterSubstituter.substitute(access)).getAbsolutePath();
    }
    this.passwordFile = pwd;
    this.accessFile = access;
    this.loginConfigName = loginConfig;

    // HTTP authentication
    String userRealm = null;
    if (server != null) {
      this.httpAuthentication = server.isSetHttpAuthentication();
    } else {
      this.httpAuthentication = false;
    }

    if (httpAuthentication) {
      userRealm = server.getHttpAuthentication().getUserRealmFile();
      if (null == userRealm) {
        userRealm = HttpAuthentication.type.getElementProperty(QName.valueOf("user-realm-file")).getDefaultText();
      }
      userRealm = new File(ParameterSubstituter.substitute(userRealm)).getAbsolutePath();
    }
    this.userRealmFile = userRealm;
  }

  public FileConfigItem dataPath() {
    return this.dataPath;
  }

  public FileConfigItem logsPath() {
    return this.logsPath;
  }

  public FileConfigItem statisticsPath() {
    return this.statisticsPath;
  }

  public FileConfigItem serverDbBackupPath() {
    return this.serverDbBackupPath;
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

  public String authenticationLoginConfigName() {
    return loginConfigName;
  }

  public String authenticationPasswordFile() {
    return passwordFile;
  }

  public boolean httpAuthentication() {
    return httpAuthentication;
  }

  public String httpAuthenticationUserRealmFile() {
    return userRealmFile;
  }
}
