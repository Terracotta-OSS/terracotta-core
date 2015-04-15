/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  private final BindPort managementPort;
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
    this.managementPort = server.getManagementPort();
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
  public BindPort managementPort() {
    return this.managementPort;
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
