/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.ServiceConfiguration;


/**
 */
public class EntityMessengerConfiguration implements ServiceConfiguration {
  
  private final boolean waitForReceived;

  public EntityMessengerConfiguration(boolean waitForReceived) {
    this.waitForReceived = waitForReceived;
  }

  public boolean isWaitForReceived() {
    return waitForReceived;
  }

  @Override
  public Class getServiceType() {
    return IEntityMessenger.class;
  }

}
