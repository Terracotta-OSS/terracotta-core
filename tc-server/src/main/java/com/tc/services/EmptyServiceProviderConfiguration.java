/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
