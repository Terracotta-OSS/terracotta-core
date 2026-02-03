/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package com.tc.l2.ha;

import com.tc.config.ServerConfigurationManager;
import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;


public class TopologyWeightGenerator implements WeightGenerator {

  private final ServerConfigurationManager configuration;
  private final int initialSize;

  public TopologyWeightGenerator(ServerConfigurationManager config) {
    this.configuration = config;
    this.initialSize = config.getNumberOfServers();
  }

  @Override
  public long getWeight() {
    try {
      return configuration.getNumberOfServers();
    } catch (Throwable t) {
      return initialSize;
    }
  }
}
