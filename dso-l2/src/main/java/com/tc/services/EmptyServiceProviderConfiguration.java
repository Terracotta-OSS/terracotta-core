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

package com.tc.services;

import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 * Configuration for service providers which don't need special configuration to initialize
 */
public class EmptyServiceProviderConfiguration implements ServiceProviderConfiguration {
  
  private final Class<? extends ServiceProvider> clazz;

  public EmptyServiceProviderConfiguration(Class<? extends ServiceProvider> clazz) {
    this.clazz = clazz;
  }

  @Override
  public Class<? extends ServiceProvider> getServiceProviderType() {
    return clazz;
  }
  
  
}
