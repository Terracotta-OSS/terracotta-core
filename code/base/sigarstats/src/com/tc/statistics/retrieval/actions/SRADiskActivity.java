/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.util.ArrayList;
import java.util.List;

/**
 * This statistic gives the disk activity going on in the system
 *
 * It contains {@link StatisticData} with the following elements:
 * <ul>
 * <li>bytes read</li>
 * <li>bytes written</li>
 * <li>reads</li>
 * <li>writes</li>
 */
public class SRADiskActivity implements StatisticRetrievalAction {
  
  public final static String ACTION_NAME = "disk activity";
  public final static String ELEMENT_BYTES_READ = "bytes read";
  public final static String ELEMENT_BYTES_WRITTEN = "bytes written";
  public final static String ELEMENT_READS = "reads";
  public final static String ELEMENT_WRITES = "writes";

  private final Sigar sigar;

  public SRADiskActivity() {
    sigar = new Sigar();
  }

  public StatisticData[] retrieveStatisticData() {
    try {
      long bytesRead = 0;
      long bytesWrite = 0;
      long reads = 0;
      long writes = 0;

      FileSystem[] list = sigar.getFileSystemList();
      for (int i = 0; i < list.length; i++) {
        FileSystemUsage usage = sigar.getFileSystemUsage(list[i].getDirName());
        bytesRead += usage.getDiskReadBytes();
        bytesWrite += usage.getDiskWriteBytes();
        reads += usage.getDiskReads();
        writes += usage.getDiskWrites();
      }
      List data = new ArrayList();
      data.add(new StatisticData(ACTION_NAME, ELEMENT_BYTES_READ, new Long(bytesRead)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_BYTES_WRITTEN, new Long(bytesWrite)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_READS, new Long(reads)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_WRITES, new Long(writes)));

      return (StatisticData[])data.toArray(new StatisticData[data.size()]);

    } catch (SigarException e) {
      LOGGER.warn(e);
      return EMPTY_STATISTIC_DATA;
    }
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}