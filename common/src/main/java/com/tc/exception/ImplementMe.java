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
public class ImplementMe extends TCRuntimeException {

  private static final String PRETTY_TEXT = "You've attempted to use an unsupported feature in this Terracotta product. Please consult "
                                            + "the product documentation, or email support@terracottatech.com for assistance.";

  /**
   * Construct new with default text
   */
  public ImplementMe() {
    this(PRETTY_TEXT);
  }

  /**
   * Construct with specified text
   * @param message The message
   */
  public ImplementMe(String message) {
    super(message);
  }

  /**
   * Construct with exception and use default text
   * @param cause The cause
   */
  public ImplementMe(Throwable cause) {
    super(PRETTY_TEXT, cause);
  }

  /**
   * Construct with specified message and cause
   * @param message Specified message
   * @param cause Cause
   */
  public ImplementMe(String message, Throwable cause) {
    super(message, cause);
  }

}
