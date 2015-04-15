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
package com.tc.object.tx;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

/**
 * Thrown when there is an attempt to access a shared object outside the scope of a shared lock.
 */
public class UnlockedSharedObjectException extends RuntimeException {

  public static final String            TROUBLE_SHOOTING_GUIDE = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=usoe";

  private static final ExceptionWrapper wrapper                = new ExceptionWrapperImpl();

  private UnlockedSharedObjectException(final String message) {
    super(wrapper.wrap(message));
  }

  public UnlockedSharedObjectException(final String message, final String threadName, final String vmId) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, vmId));
  }

  public UnlockedSharedObjectException(final String message, final String threadName, final long vmId) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, Long.toString(vmId)));
  }

  public UnlockedSharedObjectException(final String message, final String threadName, final long vmId,
                                       final String details) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, Long.toString(vmId)) + "\n"
         + details);
  }

  private static String createDisplayableString(final String message, final String threadName, final String vmId) {
    return message + "\n\nCaused by Thread: " + threadName + " in VM(" + vmId + ")";
  }
}
