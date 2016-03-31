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


/**
 * Just a struct to batch together the harness configuration data related to the testing environment (paths, etc).
 */
public class EnvironmentOptions {
  /**
   * The class path to use when starting the client sub-processes.
   */
  public String clientClassPath;
  /**
   * The path to where the template kit is located.  This location will not be modified but it will be copied as the basis
   * for every server install used by the harness.
   */
  public String serverInstallDirectory;
  /**
   * The directory which is given to the test harness for it to store arbitrary data and install different components.
   */
  public String testParentDirectory;

  /**
   * A helper to validate that all the options are set and non-empty.
   */
  public boolean isValid() {
    return (null != this.clientClassPath)
        && (this.clientClassPath.length() > 0)
        && (null != this.serverInstallDirectory)
        && (this.serverInstallDirectory.length() > 0)
        && (null != this.testParentDirectory)
        && (this.testParentDirectory.length() > 0);
  }
}
