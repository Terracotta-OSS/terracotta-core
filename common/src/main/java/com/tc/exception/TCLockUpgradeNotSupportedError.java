/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
 * Thrown when someone tries to call an unimplemented feature.
 */
public class TCLockUpgradeNotSupportedError extends TCError {
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  private static final String PRETTY_TEXT = "Lock upgrade is not supported. The READ lock needs to be unlocked before a WRITE lock can be requested. \n";

  public TCLockUpgradeNotSupportedError() {
    this(PRETTY_TEXT);
  }

  public TCLockUpgradeNotSupportedError(String message) {
    super(wrapper.wrap(message));
  }

  public TCLockUpgradeNotSupportedError(Throwable cause) {
    this(PRETTY_TEXT, cause);
  }

  public TCLockUpgradeNotSupportedError(String message, Throwable cause) {
    super(wrapper.wrap(message), cause);
  }

}
