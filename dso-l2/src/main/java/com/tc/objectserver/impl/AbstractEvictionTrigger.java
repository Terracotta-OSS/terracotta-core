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
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.EvictionTrigger;
import com.tc.objectserver.context.ServerMapEvictionContext;

import java.util.Map;

/**
 * @author mscott
 */
public abstract class AbstractEvictionTrigger implements EvictionTrigger {

  private final ObjectID      oid;
  private boolean             started     = false;
  private boolean             evicting    = false;
  private boolean             mapEvicting = false;
  private boolean             processed   = false;
  private String              name;
  private long                startTime   = 0;
  private long                endTime     = 0;
  private int                 count;
  private int                 mapSize;

  public AbstractEvictionTrigger(final ObjectID oid) {
    this.oid = oid;
  }

  protected void reset() {
    started = false;
    evicting = false;
    mapEvicting = false;
    processed = false;
    name = null;
    startTime = 0;
    endTime = 0;
    count = 0;
    mapSize = 0;
  }

  @Override
  public ObjectID getId() {
    return oid;
  }

  @Override
  public String getName() {
    return getClass().getName();
  }

  public int boundsCheckSampleSize(int sampled) {
    if (sampled < 0) {
      sampled = 0;
    }
    if (sampled > 100000) {
      sampled = 100000;
    }
    return sampled;
  }

  @Override
  public boolean startEviction(final EvictableMap map) {
    started = true;
    name = map.getCacheName();
    startTime = System.currentTimeMillis();
    mapSize = map.getSize();
    if (mapSize > 0) {
      mapEvicting = map.startEviction();
    } 
    return mapEvicting;
  }

  @Override
  public void completeEviction(final EvictableMap map) {
    if (!started) { throw new AssertionError("sample not started"); }
    if (!processed) { throw new AssertionError("sample not processed"); }
    endTime = System.currentTimeMillis() + 1;
    if (!evicting && mapEvicting) {
//  only call this if nothing was returned from the sample and eviction was started locally
      map.evictionCompleted();
    }

  }

  private Map<Object, EvictableEntry> processSample(final Map<Object, EvictableEntry> sample) {
    evicting = !sample.isEmpty();
    count = sample.size();
    processed = true;
    return sample;
  }

  protected ServerMapEvictionContext createEvictionContext(final String className, Map<Object, EvictableEntry> sample) {
    sample = processSample(sample);
    if (sample.isEmpty()) { return null; }
    return new ServerMapEvictionContext(this, sample, className, name);

  }

  @Override
  public long getRuntimeInMillis() {
    if (startTime == 0 || endTime == 0) { return 0; }
    return endTime - startTime;
  }

  @Override
  public int getCount() {
    return count;
  }
  
  @Override
  public boolean isValid() {
    return !started;
  }

  @Override
  public String toString() {
    return "AbstractEvictionTrigger{" + "name=" + name + " - " + oid + ", collected count=" + count + ", started=" + started
           + ", startTime=" + startTime + ", endTime=" + endTime + ", processed=" + processed + ", map evicting="
           + mapEvicting + ", map size=" + mapSize + ", evicting=" + evicting + '}';
  }
}
