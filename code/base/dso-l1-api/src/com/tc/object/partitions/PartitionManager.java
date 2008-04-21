package com.tc.object.partitions;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;

public class PartitionManager {

  private static final ThreadLocal partitionedClusterLocal = new ThreadLocal();

  public static Manager setPartition(Manager manager) {
    Manager prev = getPartitionManager();
    partitionedClusterLocal.set(manager);
    return prev;
  }

  public static int getNumPartitions() {
    return ClassProcessorHelper.getNumPartitions();
  }

  public static Manager getPartitionManager() {
    return (Manager) partitionedClusterLocal.get();
  }

  public static void init() {
    // does nothing, just a call to make sure this class is loaded
  }

}
