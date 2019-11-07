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
package org.terracotta.tripwire;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Threshold;
/**
 *
 */
@Category("Java Application")
@Label("TC SEDA Event")
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
