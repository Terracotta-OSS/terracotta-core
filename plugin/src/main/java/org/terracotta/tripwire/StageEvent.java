/*
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.util.concurrent.TimeUnit;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;
/**
 * The SEDA Stage
 */
@Category("Tripwire")
@Period("1 s")
@Label("Stage")
@StackTrace(false)
class StageEvent extends Event implements org.terracotta.tripwire.Event {

  private final String stage;
  private final int threads;
  private int count;
  @Label("Min Backlog")
  private int min;
  @Label("Max Backlog")
  private int max;
  @Timespan(Timespan.MILLISECONDS)
  private long runtime;

  StageEvent(String stage, int threads)  {
    this.stage = stage;
    this.threads = threads;
  }
  
  void setStats(int count, int min, int max, long runtime) {
    this.count = count;
    this.min = min;
    this.max = max;
    this.runtime = TimeUnit.NANOSECONDS.toMillis(runtime);
  }
  
  boolean hasCount() {
    return count > 0;
  }

  @Override
  public void setDescription(String description) {

  }
}
