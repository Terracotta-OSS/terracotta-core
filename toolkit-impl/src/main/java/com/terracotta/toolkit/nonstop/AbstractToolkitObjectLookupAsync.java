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

import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.abortable.AbortableOperationManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

public abstract class AbstractToolkitObjectLookupAsync<T extends ToolkitObject> implements ToolkitObjectLookup<T> {
  private static final TCLogger           LOGGER = TCLogging.getLogger(AbstractToolkitObjectLookupAsync.class);
  private volatile T                      initializedObject;
  private volatile RuntimeException       exceptionDuringInitialization;
  private final AbortableOperationManager abortableOperationManager;
  private final String                    objectName;

  public AbstractToolkitObjectLookupAsync(String objectName, AbortableOperationManager abortableOperationManager) {
    this.objectName = objectName;
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public final T getInitializedObject() {
    boolean interrupted = false;
    while (initializationPending()) {
      try {
        synchronized (this) {
          if (initializationPending()) {
            wait();
          }
        }
      } catch (InterruptedException e) {
        handleInterruptedException(e);
        interrupted = true;
      }
    }

    // If the thread was interrupted(but not by NonStopManager) set the interrupted status
    if (interrupted) {
      Thread.currentThread().interrupt();
    }

    if (exceptionDuringInitialization != null) {
      throw new NonStopToolkitInstantiationException(exceptionDuringInitialization);
    }

    return initializedObject;
  }

  @Override
  public final T getInitializedObjectOrNull() {
    return initializedObject;
  }

  public boolean initialize() {
    try {
      this.initializedObject = lookupObject();
    } catch (RuntimeException e) {
      this.exceptionDuringInitialization = e;
      throw e;
    } finally {
      synchronized (this) {
        notifyAll();
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initialization completed for : " + objectName);
    }
    return true;
  }

  private void handleInterruptedException(InterruptedException e) {
    if (abortableOperationManager.isAborted()) {
      throw new ToolkitAbortableOperationException();
    }
  }

  private boolean initializationPending() {
    return initializedObject == null && exceptionDuringInitialization == null;
  }

  protected abstract T lookupObject();

}
