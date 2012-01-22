/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.terracottatech.config.Authentication;
import com.terracottatech.config.AuthenticationMode;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.HttpAuthentication;
import com.terracottatech.config.Server;

import java.io.File;

import javax.xml.namespace.QName;

/**
 * The standard implementation of {@link CommonL2Config}.
 */
public class CommonL2ConfigObject extends BaseConfigObject implements CommonL2Config {

  private final BindPort jmxPort;
  private final String   host;
  private final boolean  authentication;
  private final String   passwordFile;
  private final String   loginConfigName;
  private final String   accessFile;
  private final boolean  httpAuthentication;
  private final String   userRealmFile;

  public CommonL2ConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Server.class);

    Server server = (Server) context.bean();

    this.host = server.getHost();

    // JMX authentication
    String pwd = null;
    String loginConfig = null;
    String access = null;
    this.authentication = server.isSetAuthentication();

    if (authentication) {
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
    this.httpAuthentication = server.isSetHttpAuthentication();

    if (httpAuthentication) {
      userRealm = server.getHttpAuthentication().getUserRealmFile();
      if (null == userRealm) {
        userRealm = HttpAuthentication.type.getElementProperty(QName.valueOf("user-realm-file")).getDefaultText();
      }
      userRealm = new File(ParameterSubstituter.substitute(userRealm)).getAbsolutePath();
    }
    this.userRealmFile = userRealm;

    this.jmxPort = server.getJmxPort();
  }

  public File dataPath() {
    Server server = (Server) getBean();
    return new File(server.getData());
  }

  public File logsPath() {
    Server server = (Server) getBean();
    return new File(server.getLogs());
  }

  public File serverDbBackupPath() {
    Server server = (Server) getBean();
    return new File(server.getDataBackup());
  }

  public File statisticsPath() {
    Server server = (Server) getBean();
    return new File(server.getStatistics());
  }

  public File indexPath() {
    Server server = (Server) getBean();
    return new File(server.getIndex());
  }

  public BindPort jmxPort() {
    return this.jmxPort;
  }

  public String host() {
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
