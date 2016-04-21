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


/**
 * The generic logger interface passed through to many components.
 */
public interface ILogger {
  /**
   * Logs a message which is considered informational/optional.  Many implementors will ignore this (perhaps depending on
   * their configuration).
   * 
   * @param message A human-readable message.
   */
  public void output(String message);

  /**
   * Logs a message which is considered fatal/important.
   * 
   * @param message A human-readable message.
   */
  public void error(String message);
}
