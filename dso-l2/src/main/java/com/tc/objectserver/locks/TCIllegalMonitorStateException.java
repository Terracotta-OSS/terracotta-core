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
package com.tc.objectserver.locks;

/**
 * Thrown by server side locks when an illegal attempt to wait/notify is performed
 */
public class TCIllegalMonitorStateException extends IllegalStateException {

  public TCIllegalMonitorStateException() {
    super();
  }

  public TCIllegalMonitorStateException(String message) {
    super(message);
  }

  public TCIllegalMonitorStateException(Throwable cause) {
    super(cause);
  }

  public TCIllegalMonitorStateException(String message, Throwable cause) {
    super(message, cause);
  }

}