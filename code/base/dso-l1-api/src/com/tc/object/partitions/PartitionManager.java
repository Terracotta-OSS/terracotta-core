package com.tc.object.partitions;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;

public class PartitionManager {

  private static final ThreadLocal partitionedClusterLocal = new ThreadLocal();

  public static int setPartition(int partitionNumber) {
    if (!ClassProcessorHelper.USE_PARTITIONED_CONTEXT) return -1;
    String oldPartition = (String) partitionedClusterLocal.get();
    partitionedClusterLocal.set("Partition" + partitionNumber);
    if (oldPartition != null) { return new Integer(oldPartition.substring(9)).intValue(); }
    return -1;
  }

  public static int getNumPartitions() {
    int partitions = ClassProcessorHelper.getNumPartitions();
    if(partitions == 0)
    	partitions = 1;
    return partitions;
  }

  public static Manager getPartitionManager() {
    String clusterId = (String) partitionedClusterLocal.get();
    return ClassProcessorHelper.getParitionedManager(clusterId);
  }

  public static void init() {
    // does nothing, just a call to make sure this class is loaded
  }

}
