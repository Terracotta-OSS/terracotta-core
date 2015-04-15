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
package com.tc.management.beans.logging;

import com.tc.management.TerracottaMBean;

import javax.management.NotificationEmitter;

public interface RuntimeOutputOptionsMBean extends TerracottaMBean, NotificationEmitter {
  public static final String AUTOLOCK_DETAILS_EVENT_TYPE = "tc.logging.runtime-output.AutoLockDetails";
  public static final String CALLER_EVENT_TYPE           = "tc.logging.runtime-output.Caller";
  public static final String FULL_STACK_EVENT_TYPE       = "tc.logging.runtime-output.FullStack";

  void setAutoLockDetails(boolean autolockDetails);

  boolean getAutoLockDetails();

  void setCaller(boolean caller);

  boolean getCaller();

  void setFullStack(boolean fullStack);

  boolean getFullStack();
}
