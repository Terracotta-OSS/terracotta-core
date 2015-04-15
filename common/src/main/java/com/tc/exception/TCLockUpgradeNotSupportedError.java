/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.exception;

/**
 * Thrown when someone tries to call an unimplemented feature.
 */
public class TCLockUpgradeNotSupportedError extends TCError {
  public final static String  CLASS_SLASH = "com/tc/exception/TCLockUpgradeNotSupportedError";
  
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
