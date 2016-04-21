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

import org.terracotta.testing.common.Assert;


/**
 * Note that this version of this class is a placeholder to simplify an API transformation.  Hence why it is little more than
 * a container for a small number of data elements of no obvious connection (as it just replaces them in some parameter
 * lists).
 * In a later change, it will expand to have a more meaningful purpose but introducing it allows a broad API change to be
 * made, in isolation.
 */
public class VerboseManager {
  private final boolean enableVerbose;
  private VerboseLogger verboseLogger;
  private ContextualLogger fileHelpersLogger;

  public VerboseManager(boolean enableVerbose) {
    this.enableVerbose = enableVerbose;
  }

  public boolean isVerboseEnabled() {
    return this.enableVerbose;
  }

  public void setVerboseLogger(VerboseLogger verboseLogger) {
    Assert.assertNull(this.verboseLogger);
    this.verboseLogger = verboseLogger;
  }

  public VerboseLogger getVerboseLogger() {
    Assert.assertNotNull(this.verboseLogger);
    return this.verboseLogger;
  }

  public void setFileHelpersLogger(ContextualLogger fileHelpersLogger) {
    Assert.assertNull(this.fileHelpersLogger);
    this.fileHelpersLogger = fileHelpersLogger;
  }

  public ContextualLogger getFileHelpersLogger() {
    Assert.assertNotNull(this.fileHelpersLogger);
    return this.fileHelpersLogger;
  }
}
