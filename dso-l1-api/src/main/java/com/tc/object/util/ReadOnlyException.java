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
package com.tc.object.util;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

/**
 * Indicates a read-only transaction is trying to access a shared object.  This is most likely 
 * a problem with an incorrect lock configuration.
 */
public class ReadOnlyException extends RuntimeException {
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  /** Indicates a default invalid VM_ID to use */
  public static final long INVALID_VMID = -1;
  
  /**
   * @param message Message, which will be wrapped
   */
  protected ReadOnlyException(String message) {
    super(wrapper.wrap(message));
  }
  
  /**
   * @param message Message
   * @param threadName Thread name
   * @param vmId VM identifier
   */
  public ReadOnlyException(String message, String threadName, long vmId) {
    this(ReadOnlyException.createDisplayableString(message, threadName, vmId));
  }
  
  /**
   * @param message Message
   * @param threadName Thread name
   * @param vmId VM identifier
   * @param details Additional details
   */
  public ReadOnlyException(String message, String threadName, long vmId, String details) {
    this(ReadOnlyException.createDisplayableString(message, threadName, vmId) + "\n    " + details);
  }
  
  private static String createDisplayableString(String message, String threadName, long vmId) {
    if (vmId == INVALID_VMID) {
      return message + "\n\n    Caused by Thread: " + threadName;
    }
    return message + "\n\n    Caused by Thread: " + threadName + "  in  VM(" + vmId + ")";
  }
}
