/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.exception;

/**
 * An error type for serious (non-recoverable) error conditions in the Terracotta system. No production code should ever
 * catch this exception.
 */
public class TCInternalError extends TCError {

  public TCInternalError() {
    super("Terracotta Internal Error");
  }

  public TCInternalError(String message) {
    super(message);
  }

  public TCInternalError(Throwable cause) {
    super(cause);
  }

  public TCInternalError(String message, Throwable cause) {
    super(message, cause);
  }

}
