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
import org.terracotta.config.TcConfiguration;

import java.io.File;

/**
 * Contains methods exposing DSO L2 config.
 */
public interface CommonL2Config extends Config<TcConfiguration> {

  File logsPath();

  BindPort tsaPort();

  BindPort tsaGroupPort();

  BindPort managementPort();

  String host();

  boolean authentication();

  String authenticationPasswordFile();

  String authenticationAccessFile();

  String authenticationLoginConfigName();

  boolean httpAuthentication();

  String httpAuthenticationUserRealmFile();

  boolean isSecure();
}
