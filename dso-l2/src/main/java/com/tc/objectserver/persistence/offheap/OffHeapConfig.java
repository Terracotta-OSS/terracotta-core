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
import com.tc.lang.ServerExitStatus;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.utils.L2Utils;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.Conversion.MetricsFormatException;
import com.tc.util.runtime.Vm;

public abstract class OffHeapConfig {
  private static final int              MIN_OFFHEAP_MEM        = 512 * 1024 * 1024;
  private static final int              DEFAULT_MIN_PAGE_SIZE = 4 * 1024;
  private static final int              DEFAULT_MAX_PAGE_SIZE = 8 * 1024 * 1024;
  private static final int              DEFAULT_MIN_CHUNK_SIZE = 32 * 1024 * 1024;
  private static final int              DEFAULT_MAX_CHUNK_SIZE = 512 * 1024 * 1024;

  private static final long             GB = 1024L * 1024L * 1024L;

  private final boolean                 enabled;
  private final long                    offheapSize;

  private final OffheapObjectProperties objectProperties;

  private final int                     mapTableSize;
  private final int                     mapMinPageSize;
  private final int                     mapMaxPageSize;
  private final int                     mapConcurrency;
  private final int                     maxChunkSize;
  private final int                     minChunkSize;

  private static final TCLogger         logger                 = TCLogging.getLogger(OffHeapConfig.class);
  private static final TCLogger         consoleLogger          = CustomerLogging.getConsoleLogger();

  public OffHeapConfig(final boolean offHeapEnabled, final String maxOffHeapDataSize,
                       final boolean skipJVMArgCheck) {
    try {
      // general
      this.enabled = offHeapEnabled;
      this.offheapSize = Conversion.memorySizeAsLongBytes(maxOffHeapDataSize);

      objectProperties = new OffheapObjectProperties(this.offheapSize);
      objectProperties.validateProperties();

      if (isTcPropertySet(TCPropertiesConsts.L2_OFFHEAP_MIN_CHUNK_SIZE)) {
        this.minChunkSize = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
            .getProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_CHUNK_SIZE, false));
      } else {
        this.minChunkSize = DEFAULT_MIN_CHUNK_SIZE;
      }

      // OffHeap Map cache
      this.mapTableSize = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_TABLESIZE, false));
      this.mapMinPageSize = floorMinPageSize(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, true));
      this.mapMaxPageSize = capMaxPageSize(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE, true));
      this.mapConcurrency = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CONCURRENCY, false));

      int maxChunkSizeProvided;
      if (isTcPropertySet(TCPropertiesConsts.L2_OFFHEAP_MAX_CHUNK_SIZE)) {
        maxChunkSizeProvided = Conversion.memorySizeAsIntBytes(TCPropertiesImpl.getProperties()
            .getProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_CHUNK_SIZE, false));
      } else {
        maxChunkSizeProvided = DEFAULT_MAX_CHUNK_SIZE;
      }
      this.maxChunkSize = (int) Math.min(maxChunkSizeProvided, this.offheapSize / 2);

      if (this.maxChunkSize < maxChunkSizeProvided) {
        logger.info("The specified max chunk size is larger than " +
                    "the max data size and hence the max chunk size is set to "
                    + Conversion.toJvmArgument(this.maxChunkSize));
      }

      Assert.eval(MIN_OFFHEAP_MEM >= this.minChunkSize);
      long maxOffHeapFromVMInBytes = Vm.maxDirectMemory();
      boolean canProceed = sanityCheckMemorySize(this.offheapSize, maxOffHeapFromVMInBytes, skipJVMArgCheck);
      if (!canProceed) {
        System.exit(ServerExitStatus.EXITCODE_STARTUP_ERROR);
      }
    } catch (Exception e) {
      throw new TCRuntimeException("Problem building offheap cache config: ", e);
    }
    logger.info(toString());
  }

  private static int floorMinPageSize(String prop) throws MetricsFormatException {
      int min = ( prop == null ) ? DEFAULT_MIN_PAGE_SIZE
          : Conversion.memorySizeAsIntBytes(prop);
      if ( min < DEFAULT_MIN_PAGE_SIZE ) {
          return DEFAULT_MIN_PAGE_SIZE;
      }
      return min;
  }

  private int capMaxPageSize(String prop) throws MetricsFormatException {
      if ( prop != null ) {
          int maxSize = Conversion.memorySizeAsIntBytes(prop);
          if ( maxSize < mapMinPageSize ) {
              logger.warn("Trying to set offheap page size maximum lower than the minimum page size.  Maximum page size will be set to the minimum.  Check your configuration.");
              maxSize = this.mapMinPageSize;
          }
          return maxSize;
      }
      String pageCount = TCPropertiesImpl.getProperties()
          .getProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_COUNT, true);

      int maxPages = 100 * 1024;
      if ( pageCount != null ) {
          maxPages = Integer.parseInt(pageCount);
    }

      int max = DEFAULT_MAX_PAGE_SIZE;
      long granularity = this.offheapSize / maxPages;
      while ( max > granularity && max > this.mapMinPageSize ) {
          max /= 2;
      }
      if ( max < this.mapMinPageSize ) {
          max = this.mapMinPageSize;
      }

      return max;
  }

  private static boolean isTcPropertySet(String prop) {
    String propValue = TCPropertiesImpl.getProperties().getProperty(prop, true);
    return propValue != null;
  }

  /**
   * Checks if offheap size asked for is serviceable by the TCServer
   *
   * @param offheapSize offheap size requested
   * @param maxOffHeapFromVMInBytes : long size in -XX:MaxDirectMemorySize=<size>
   * @param skipCheck ignore the check
   * @return boolen : true if the cacheConfig offheap size is serviceable by the TCServer. false, otherwise.
   * @throws NumberFormatException
   */
  private static boolean sanityCheckMemorySize(final long offheapSize, final long maxOffHeapFromVMInBytes,
                                               boolean skipCheck) throws NumberFormatException {

    if (skipCheck) {
      logger.warn("JVM argument -XX:MaxDirectMemorySize=<size> sanity check disabled");
      return true;
    }

    if (maxOffHeapFromVMInBytes <= 0) {
      String errMsg = "JVM argument -XX:MaxDirectMemorySize=<size> missing. OffHeapCache might have problems.";
      consoleLogger.error(errMsg);
      return true;
    }

    final long commsMaxDirectMemoryRequirement = L2Utils.getMaxDirectMemmoryConsumable();

    String errMsg;
    if (offheapSize < MIN_OFFHEAP_MEM) {
      errMsg = "The offheap size(" + Conversion.toJvmArgument(offheapSize)
               + ") in the tc-config.xml cannot be less than TC minimum OffHeap memory requirement: "
               + Conversion.toJvmArgument(MIN_OFFHEAP_MEM);
      consoleLogger.error(errMsg);
      return false;
    } else if (maxOffHeapFromVMInBytes < (MIN_OFFHEAP_MEM + commsMaxDirectMemoryRequirement)) {
      errMsg = "The JVM argument -XX:MaxDirectMemorySize(" + Conversion.toJvmArgument(maxOffHeapFromVMInBytes)
               + ") cannot be less than TC minimum Direct memory requirement: "
               + Conversion.toJvmArgument(MIN_OFFHEAP_MEM + commsMaxDirectMemoryRequirement);
      consoleLogger.error(errMsg);
      return false;
    }

    if ((offheapSize + commsMaxDirectMemoryRequirement) > maxOffHeapFromVMInBytes) {
      errMsg = "Minimum -XX:MaxDirectMemorySize="
               + Conversion.toJvmArgument(offheapSize + commsMaxDirectMemoryRequirement)
               + " is needed for offheap size of " + Conversion.toJvmArgument(offheapSize) + ". ";

      if ((maxOffHeapFromVMInBytes - commsMaxDirectMemoryRequirement) > MIN_OFFHEAP_MEM) {
        errMsg += " Or, OffHeapCache offheap size(" + Conversion.toJvmArgument(offheapSize)
                  + ") in tc-config.xml should be "
                  + Conversion.toJvmArgument(maxOffHeapFromVMInBytes - commsMaxDirectMemoryRequirement)
                  + " (-XX:MaxDirectMemorySize=" + Conversion.toJvmArgument(maxOffHeapFromVMInBytes) + " - "
                  + Conversion.toJvmArgument(commsMaxDirectMemoryRequirement) + ") or lesser.";
      }

      consoleLogger.error(errMsg);
      return false;
    }
    verifyHeapToOffheapRatio(Runtime.getRuntime().maxMemory(), offheapSize);
    return true;
  }

  static boolean verifyHeapToOffheapRatio(final long heapSize, final long offheapSize) {
    long minHeapSize = 0;
    if (offheapSize > GB && offheapSize <= 10 * GB) {
      minHeapSize = GB;
    } else if (offheapSize > 10 * GB && offheapSize <= 100 * GB) {
      minHeapSize = 2 * GB;
    } else if (offheapSize > 100 * GB) {
      minHeapSize = 3 * GB;
    }
    // DEV-9664: 5% confidence interval
    if (heapSize < minHeapSize * 0.95) {
      consoleLogger.warn("Current java heap size of " + Conversion.toJvmArgument(heapSize)
                         + " is less then TC minimum requirement of " + Conversion.toJvmArgument(minHeapSize)
                         + " to correctly work with " + Conversion.toJvmArgument(offheapSize) + " of offheap");
      return false;
    }
    return true;
  }

  public boolean enabled() {
    return this.enabled;
  }

  public long getOffheapSize() {
    return this.offheapSize;
  }

  public int getObjectTableSize() {
    return this.objectProperties.getObjectTableSize();
  }

  public int getObjectInitialDataSize() {
    return (int) this.objectProperties.getObjectInitialDataSize();
  }

  public int getObjectConcurrency() {
    return this.objectProperties.getObjectConcurrency();
  }

  public int getMapTableSize() {
    return this.mapTableSize;
  }

  public int getMinMapPageSize() {
    return this.mapMinPageSize;
  }

  public int getMaxMapPageSize() {
    return this.mapMaxPageSize;
  }

  public int getMinChunkSize() {
    return this.minChunkSize;
  }

  public int getMaxChunkSize() {
    return this.maxChunkSize;
  }

  @Override
  public final String toString() {
    try {
      return ("OffHeapCacheConfig [ Enabled:" + enabled + "; Offheap Size: " + Conversion.memoryBytesAsSize(offheapSize)
              + "; MaxChunkSize, MinChunkSize: " + Conversion.memoryBytesAsSize(this.maxChunkSize) + ", " + Conversion.memoryBytesAsSize(this.minChunkSize)
              + "; MapsDBConcurrency: " + mapConcurrency + "; mapsTableSize: " + mapTableSize
              + "; mapMaxPageSize, mapMinPageSize: " + Conversion.memoryBytesAsSize(mapMaxPageSize) + ','
              + Conversion.memoryBytesAsSize(mapMinPageSize) + "; ObjInitialDataSize: "
              + Conversion.memoryBytesAsSize(getObjectInitialDataSize()) + "; ObjTableSize: " + getObjectTableSize()
              + "; ObjConcurrency: " + getObjectConcurrency() + " ], Vm.MaxDirectMemorySize [ "
              + Conversion.memoryBytesAsSize(Vm.maxDirectMemory()) + " ]");
    } catch (Exception e) {
      return ("OffHeapCacheConfig [ Enabled:" + enabled + "; Offheap Size: " + offheapSize + "; MapsDBConcurrency: "
              + mapConcurrency + "; mapsTableSize: " + mapTableSize + "; mapMaxPageSize, mapMinPageSize: "
              + mapMaxPageSize + ',' + mapMinPageSize + "; ObjInitialDataSize: " + getObjectInitialDataSize()
              + "; ObjTableSize: " + getObjectTableSize() + "; ObjConcurrency: " + getObjectConcurrency()
              + " ], Vm.MaxDirectMemorySize [ " + Vm.maxDirectMemory() + " ]");
    }
  }
}
