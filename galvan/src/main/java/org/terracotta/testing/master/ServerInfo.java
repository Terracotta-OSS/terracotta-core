/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import org.terracotta.testing.common.Assert;

/**
 * @author vmad
 */
public class ServerInfo {
  private static final String DELIM = ",";
  private final String name;
  private final int serverPort;
  private final int groupPort;


  public ServerInfo(String name, int serverPort, int groupPort) {
    this.name = name;
    this.serverPort = serverPort;
    this.groupPort = groupPort;
  }

  public String getName() {
    return name;
  }

  public int getServerPort() {
    return serverPort;
  }

  public int getGroupPort() {
    return groupPort;
  }

  public String encode() {
    return name + DELIM + serverPort + DELIM + groupPort;
  }

  public static ServerInfo decode(String from) {
    String[] tokens = from.split(DELIM);
    Assert.assertTrue(tokens.length == 3);
    return new ServerInfo(tokens[0], Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
  }

}
