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
 * An exception used to signify a test failure.  It contains the human-readable description of the test failure, potentially
 *  including an underlying cause.
 */
public class GalvanFailureException extends Exception {
  private static final long serialVersionUID = 1L;


  public GalvanFailureException(String message) {
    super(message);
  }

  public GalvanFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
