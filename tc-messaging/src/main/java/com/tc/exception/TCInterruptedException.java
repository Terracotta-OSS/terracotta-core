/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.exception;

/**
 * The base class for all runtime (non-checked) Terracotta exceptions. Normal production code should never catch this.
 */
public class TCInterruptedException extends RuntimeException {

  public TCInterruptedException() {
    super();
  }

  public TCInterruptedException(String message) {
    super(message);
  }

  public TCInterruptedException(Throwable cause) {
    super(cause);
  }

  public TCInterruptedException(String message, Throwable cause) {
    super(message, cause);
  }

}