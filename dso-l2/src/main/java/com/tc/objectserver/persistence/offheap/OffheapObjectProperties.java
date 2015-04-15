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
package com.tc.objectserver.persistence.offheap;

import com.tc.exception.TCRuntimeException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Conversion;
import com.tc.util.Conversion.MetricsFormatException;

public class OffheapObjectProperties {
  public final static int  ONE_KB                               = 1024;
  public final static int  ONE_MB                               = 1024 * 1024;
  public final static long ONE_GB                               = 1024 * 1024 * 1024;

  final static int         MINIMUM_SEGMENT_SIZE                 = 32 * ONE_MB;
  static final int         OBJECT_CONCURRENCY_BELOW_1GB         = 32;
  static final int         OBJECT_CONCURRENCY_BETWEEN_1_AND_4GB = 32 * 4;
  static final int         OBJECT_CONCURRENCY_ABOVE_4GB         = 4 * ONE_KB;

  static final long        OBJECT_MIN_INIT_DATA_SIZE            = ONE_KB;
  static final long        OBJECT_MAX_INIT_DATA_SIZE            = ONE_MB;

  static final int         OBJECT_TABLE_SIZE_BELOW_1GB          = 512 * ONE_KB;
  static final int         OBJECT_TABLE_SIZE_ABOVE_1GB          = ONE_MB;

  private int              objectTableSize;
  private int              objectConcurrency;
  private long             objectInitialDataSize;
  private final long       objectMaxSize;

  // 50 % occupation to begin with
  public final static int  OBJECT_PERCENTAGE                    = 50;

  public OffheapObjectProperties(long maxSize) throws MetricsFormatException {
    this.objectMaxSize = (long) (maxSize * (OBJECT_PERCENTAGE / 100.0));
    setProperties();
  }

  private void setProperties() throws MetricsFormatException {
    setObjectOffheapProperties();
  }

  private void setObjectOffheapProperties() throws MetricsFormatException {
    setObjectTableSize();
    setObjectConcurrency();
    setObjectInitDataSize(this.objectConcurrency);
  }

  private int setObjectConcurrency() throws MetricsFormatException {
    if (isTcPropertySet(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_CONCURRENCY)) {
      this.objectConcurrency = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_CONCURRENCY, false));
      return this.objectConcurrency;
    }

    // Calculate concurrency making use of segment size
    this.objectConcurrency = (int) Math.ceil(this.objectMaxSize / (double) MINIMUM_SEGMENT_SIZE);

    if (this.objectMaxSize < ONE_GB) {
      this.objectConcurrency = Math.min(OBJECT_CONCURRENCY_BELOW_1GB, this.objectConcurrency);
    } else if (this.objectMaxSize >= ONE_GB && this.objectMaxSize < (4 * ONE_GB)) {
      this.objectConcurrency = Math.min(OBJECT_CONCURRENCY_BETWEEN_1_AND_4GB, this.objectConcurrency);
    } else {
      this.objectConcurrency = Math.min(OBJECT_CONCURRENCY_ABOVE_4GB, this.objectConcurrency);
    }

    return this.objectConcurrency;
  }

  private long setObjectInitDataSize(int concurrency) throws MetricsFormatException {
    if (isTcPropertySet(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_INITIAL_DATASIZE)) {
      this.objectInitialDataSize = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_INITIAL_DATASIZE, false));
      return this.objectInitialDataSize;
    }

    long dataSize = (int) (((this.objectMaxSize) - (objectTableSize * 16)) / concurrency);
    dataSize = Long.highestOneBit(dataSize);
    if (dataSize > OBJECT_MAX_INIT_DATA_SIZE) {
      dataSize = OBJECT_MAX_INIT_DATA_SIZE;
    } else if (dataSize < OBJECT_MIN_INIT_DATA_SIZE) {
      dataSize = OBJECT_MIN_INIT_DATA_SIZE;
    }

    this.objectInitialDataSize = dataSize;
    return this.objectInitialDataSize;
  }

  private int setObjectTableSize() throws MetricsFormatException {
    if (isTcPropertySet(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_TABLESIZE)) {
      objectTableSize = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_TABLESIZE, false));
    } else {
      objectTableSize = (this.objectMaxSize > ONE_GB) ? OBJECT_TABLE_SIZE_ABOVE_1GB : OBJECT_TABLE_SIZE_BELOW_1GB;
    }
    return this.objectTableSize;
  }

  public int getObjectTableSize() {
    return objectTableSize;
  }

  public int getObjectConcurrency() {
    return objectConcurrency;
  }

  public long getObjectInitialDataSize() {
    return objectInitialDataSize;
  }

  private boolean isTcPropertySet(String prop) {
    String propValue = TCPropertiesImpl.getProperties().getProperty(prop, true);
    if (propValue == null) { return false; }
    return true;
  }

  public void validateProperties() {
    long memoryExpected = (this.objectConcurrency * this.objectInitialDataSize) + (16 * objectTableSize);
    if (memoryExpected > this.objectMaxSize) { throw new TCRuntimeException(
                                                                            "The current memory setting exceeds the memory assigned for the Offheap. ObjectMaxSizeExpected: "
                                                                                + memoryExpected
                                                                                + " Vs ObjectMaxSizeActual: "
                                                                                + this.objectMaxSize + "; "
                                                                                + toString()); }
  }

  @Override
  public String toString() {
    return "OffheapObjectProperties [objectConcurrency=" + objectConcurrency + ", objectInitialDataSize="
           + objectInitialDataSize + ", objectOffheapSize=" + objectMaxSize + ", objectTableSize=" + objectTableSize
           + "]";
  }
}