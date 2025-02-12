/*
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.tripwire;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Threshold;
/**
 *
 */
@Category("Tripwire")
@Label("SEDA Event")
@Threshold("200 ms")
class MonitoringEvent extends Event implements org.terracotta.tripwire.Event {
  private final String stage;
  private String description;

  MonitoringEvent(String stage, String description) {
    this.stage = stage;
    this.description = description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }
}
