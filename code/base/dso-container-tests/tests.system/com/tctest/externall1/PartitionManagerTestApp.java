/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.externall1;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.partitions.PartitionManager;
import com.tc.util.Assert;

import java.util.Map;

public class PartitionManagerTestApp {

  private Map              sharedMap0;
  private Map              sharedMap1;
  private static Manager[] managers = ClassProcessorHelper.getPartitionedManagers();

  public static void main(String[] args) {
    try {
      final PartitionManagerTestApp app = new PartitionManagerTestApp();
      app.doTest();
    } catch (Exception e) {
      System.err.println("NOT OK: " + e.toString());
      System.exit(1);
    }
  }

  private void doTest() {
    String mapStr = System.getProperty("map.num");
    if (!"0".equals(mapStr) && !"1".equals(mapStr)) {
      Assert.fail("There should be a property \"map.num\" with value \"0\" or \"1");
    }
    int mapNum = Integer.parseInt(mapStr);

    String p = System.getProperty("partition.num");
    if (!"0".equals(p) && !"1".equals(p)) {
      Assert.fail("There should be a property \"partition.num\" with value \"0\" or \"1");
    }
    int partition = Integer.parseInt(p);

    System.err.println("Setting partition number to: " + partition);
    PartitionManager.setPartition(managers[partition]);
    Map map = null;
    switch (mapNum) {
      case 0:
        map = sharedMap0;
        break;
      case 1:
        map = sharedMap1;
        break;
    }

    System.err.println("Using map: " + mapNum + " hashCode: " + System.identityHashCode(map));
    String cmd = System.getProperty("cmd");

    if ("print".equals(cmd)) {
      System.err.println("partition: "
                         + partition
                         + " sharedMap:"
                         + (map == null ? "NULL" : ("map:" + System.identityHashCode(map) + " keys:" + map.keySet()
                             .toString())));
      System.err.println("OK");
    }
    if ("assertSize".equals(cmd)) {
      int size = -1;
      try {
        final String sizePar = System.getProperty("size");
        System.err.println("SizeParameter: " + (sizePar == null ? "NULL" : sizePar));
        size = Integer.parseInt(sizePar);
      } catch (Exception e) {
        Assert.fail("There should be a property \"size\" for \"assertSize\" command");
      }
      System.err.println("Size: " + map.size() + " Expected: " + size);
      System.err.println("Map elements: " + map.toString());
      Assert.assertEquals(size, map.size());
      System.err.println("OK");
    }
    if ("assertNull".equals(cmd)) {
      System.err.println("Asserting null...");
      Assert.assertNull(map);
      System.err.println("OK");
    }
  }
}