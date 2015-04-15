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
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.terracotta.toolkit.ToolkitInitializer;

public class ToolkitLockLookup implements ToolkitObjectLookup<ToolkitLock> {
  private final ToolkitLockTypeInternal lockType;
  private final String                  name;
  private final ToolkitInitializer      toolkitInitializer;

  public ToolkitLockLookup(ToolkitInitializer toolkitInitializer, String name,
                           ToolkitLockTypeInternal lockType) {
    this.toolkitInitializer = toolkitInitializer;
    this.name = name;
    this.lockType = lockType;
  }

  @Override
  public ToolkitLock getInitializedObject() {
    return toolkitInitializer.getToolkit().getLock(name, lockType);
  }

  @Override
  public ToolkitLock getInitializedObjectOrNull() {
    ToolkitInternal toolkitInternal = toolkitInitializer.getToolkitOrNull();
    if (toolkitInternal != null) {
      return toolkitInternal.getLock(name, lockType);
    } else {
      return null;
    }
  }
}