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
package com.terracotta.toolkit;

import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.feature.EnabledToolkitFeature;

public class NonStopFeatureImpl extends EnabledToolkitFeature implements NonStopFeature {
  private final NonStopToolkitImpl nonStopToolkitImpl;
  private final AbortableOperationManager abortableOperationManager;

  public NonStopFeatureImpl(NonStopToolkitImpl nonStopToolkitImpl, AbortableOperationManager abortableOperationManager) {
    this.nonStopToolkitImpl = nonStopToolkitImpl;
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void start(NonStopConfiguration nonStopConfig) {
    if (nonStopConfig == null) { return; }

    nonStopToolkitImpl.start(nonStopConfig);
  }

  @Override
  public void finish() {
    nonStopToolkitImpl.stop();
  }

  @Override
  public NonStopConfigurationRegistry getNonStopConfigurationRegistry() {
    return nonStopToolkitImpl.getNonStopConfigurationToolkitRegistry();
  }

  @Override
  public boolean isTimedOut() {
    return abortableOperationManager.isAborted();
  }

}
