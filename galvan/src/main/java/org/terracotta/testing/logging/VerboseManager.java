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
package org.terracotta.testing.logging;


public class VerboseManager {
  private final String prefix;
  private final VerboseLogger harnessLogger;
  private final VerboseLogger fileHelpersLogger;
  private final VerboseLogger clientLogger;
  private final VerboseLogger serverLogger;

  public VerboseManager(String prefix, VerboseLogger harnessLogger, VerboseLogger fileHelpersLogger, VerboseLogger clientLogger, VerboseLogger serverLogger) {
    this.prefix = prefix;
    this.harnessLogger = harnessLogger;
    this.fileHelpersLogger = fileHelpersLogger;
    this.clientLogger = clientLogger;
    this.serverLogger = serverLogger;
  }

  public VerboseManager createComponentManager(String componentName) {
    return new VerboseManager(this.prefix + componentName, this.harnessLogger, this.fileHelpersLogger, this.clientLogger, this.serverLogger);
  }

  public ContextualLogger createHarnessLogger() {
    return new ContextualLogger(this.harnessLogger, this.prefix);
  }

  public ContextualLogger createFileHelpersLogger() {
    return new ContextualLogger(this.fileHelpersLogger, this.prefix);
  }

  public ContextualLogger createClientLogger() {
    return new ContextualLogger(this.clientLogger, this.prefix);
  }

  public ContextualLogger createServerLogger() {
    return new ContextualLogger(this.serverLogger, this.prefix);
  }
}
