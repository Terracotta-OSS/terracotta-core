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

@Label("ServerState")
@Category("Tripwire")
public class ServerStateEvent extends Event implements org.terracotta.tripwire.Event {
  private final String state;
  private final boolean active;

  public ServerStateEvent(String state, boolean active) {
    this.state = state;
    this.active = active;
  }

  @Override
  public void setDescription(String description) {

  }
}
