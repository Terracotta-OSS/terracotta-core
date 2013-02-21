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
  private final BindPort tsaPort;
  private final BindPort tsaGroupPort;
  private final String   host;
  private final boolean  authentication;
  private final String   passwordFile;
  private final String   loginConfigName;
  private final String   accessFile;
  private final boolean  httpAuthentication;
  private final String   userRealmFile;
  private final boolean  secured;

  public CommonL2ConfigObject(ConfigContext context) {
    this(context, false);
  }

  public CommonL2ConfigObject(ConfigContext context, boolean secured) {
    super(context);
    this.secured = secured;
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
    this.tsaPort = server.getTsaPort();
    this.tsaGroupPort = server.getTsaGroupPort();
  }

  @Override
  public File dataPath() {
    Server server = (Server) getBean();
    return new File(server.getData());
  }

  @Override
  public File logsPath() {
    Server server = (Server) getBean();
    return new File(server.getLogs());
  }

  @Override
  public File serverDbBackupPath() {
    Server server = (Server) getBean();
    return new File(server.getDataBackup());
  }

  @Override
  public File indexPath() {
    Server server = (Server) getBean();
    return new File(server.getIndex());
  }

  @Override
  public BindPort jmxPort() {
    return this.jmxPort;
  }

  @Override
  public BindPort tsaPort() {
    return this.tsaPort;
  }

  @Override
  public BindPort tsaGroupPort() {
    return this.tsaGroupPort;
  }

  @Override
  public String host() {
    return this.host;
  }

  @Override
  public boolean authentication() {
    return authentication;
  }

  @Override
  public String authenticationAccessFile() {
    return accessFile;
  }

  @Override
  public String authenticationLoginConfigName() {
    return loginConfigName;
  }

  @Override
  public String authenticationPasswordFile() {
    return passwordFile;
  }

  @Override
  public boolean httpAuthentication() {
    return httpAuthentication;
  }

  @Override
  public String httpAuthenticationUserRealmFile() {
    return userRealmFile;
  }

  @Override
  public boolean isSecure() {
    return secured;
  }
}
