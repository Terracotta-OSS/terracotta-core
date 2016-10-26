/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema;

import org.terracotta.config.BindPort;
import org.terracotta.config.Server;

import org.terracotta.config.TcConfiguration;

import java.io.File;

/**
 * The standard implementation of {@link CommonL2Config}.
 */
public class CommonL2ConfigObject implements CommonL2Config {

  private final BindPort tsaPort;
  private final BindPort tsaGroupPort;
  private final BindPort managementPort;
  private final String host;
  private final boolean authentication;
  private final String passwordFile;
  private final String loginConfigName;
  private final String accessFile;
  private final boolean httpAuthentication;
  private final String userRealmFile;
  private final boolean secured;
  private final Server server;
  private final TcConfiguration conf;

  public CommonL2ConfigObject(Server server) {
    this(server, null, false);
  }

  public CommonL2ConfigObject(Server context, TcConfiguration conf ,boolean secured) {
    this.secured = secured;
    server = context;
    this.conf = conf;

    this.host = server.getHost();

    // JMX authentication
    //TODO fix this when handling management service
    this.authentication = false;


    this.passwordFile = null;
    this.accessFile = null;
    this.loginConfigName = null;

    // HTTP authentication
    this.httpAuthentication = false;

    if (httpAuthentication) {
    }
    this.userRealmFile = null;

    this.tsaPort = server.getTsaPort();
    this.tsaGroupPort = server.getTsaGroupPort();
    this.managementPort = server.getManagementPort();
  }

  @Override
  public File logsPath() {
    return new File(server.getLogs());
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

  @Override
  public TcConfiguration getBean() {
    return this.conf;
  }
}
