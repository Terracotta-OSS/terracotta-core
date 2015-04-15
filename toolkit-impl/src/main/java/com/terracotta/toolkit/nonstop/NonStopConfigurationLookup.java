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

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;

public class NonStopConfigurationLookup {
  private final NonStopContext    context;
  private final ToolkitObjectType objectType;
  private final String            name;

  public NonStopConfigurationLookup(NonStopContext context, ToolkitObjectType objectType, String name) {
    this.context = context;
    this.objectType = objectType;
    this.name = name;
  }

  public ToolkitObjectType getObjectType() {
    return objectType;
  }

  public NonStopConfiguration getNonStopConfiguration() {
    NonStopConfiguration config = context.getNonStopConfigurationRegistry()
        .getConfigForInstance(name, objectType);
    if (!context.isEnabledForCurrentThread()) {
      return new DisabledNonStopConfiguration(config);
    }
    return config;
  }

  public NonStopConfiguration getNonStopConfigurationForMethod(String methodName) {
    NonStopConfiguration config = context.getNonStopConfigurationRegistry()
        .getConfigForInstanceMethod(methodName, name, objectType);
    if (!context.isEnabledForCurrentThread()) {
      return new DisabledNonStopConfiguration(config);
    }
    return config;
  }


  private static final class DisabledNonStopConfiguration implements NonStopConfiguration {

    private final NonStopConfiguration delegate;

    public DisabledNonStopConfiguration(NonStopConfiguration delegate) {
      this.delegate = delegate;
    }

    @Override
    public NonStopConfigurationFields.NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
      return delegate.getReadOpNonStopTimeoutBehavior();
    }

    @Override
    public NonStopConfigurationFields.NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
      return delegate.getWriteOpNonStopTimeoutBehavior();
    }

    @Override
    public long getTimeoutMillis() {
      return delegate.getTimeoutMillis();
    }

    @Override
    public long getSearchTimeoutMillis() {
      return delegate.getSearchTimeoutMillis();
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    public boolean isImmediateTimeoutEnabled() {
      return delegate.isImmediateTimeoutEnabled();
    }
  }

}
