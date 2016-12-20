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
package org.terracotta.testing.api;

import org.terracotta.passthrough.IClientTestEnvironment;


/**
 * The interface implemented by classes which wish to be used as the client-side error handler by TestClientStub.
 */
public interface IClientErrorHandler {
  /**
   * Called by the TestClientStub exception handler in the case of a fatal error.  Typically, this is used to collect
   *  diagnostic data to help determine why the test failed.
   * 
   * @param environment The environment of the test run in this client
   * @param error The unhandled error
   */
  public void handleError(IClientTestEnvironment environment, Throwable error);
}
