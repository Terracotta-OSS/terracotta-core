/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.retrieval.SigarUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This statistic gives the disk activity going on in the system
 * <p/>
 * It contains {@link StatisticData} with the following elements:
 * <ul>
 * <li>bytes read</li>
 * <li>bytes written</li>
 * <li>reads</li>
 * <li>writes</li>
 */
public class SRADiskActivity implements StatisticRetrievalAction {

  public final static TCLogger LOGGER                = TCLogging.getLogger(StatisticRetrievalAction.class);

  public final static String   ACTION_NAME           = "disk activity";
  public final static String   ELEMENT_BYTES_READ    = "bytes read";
  public final static String   ELEMENT_BYTES_WRITTEN = "bytes written";
  public final static String   ELEMENT_READS         = "reads";
  public final static String   ELEMENT_WRITES        = "writes";

  private final Sigar          sigar;

  private static final boolean SIGAR_ENABLED         = TCPropertiesImpl.getProperties()
                                                         .getBoolean(TCPropertiesConsts.SIGAR_ENABLED);

  public SRADiskActivity() {
    sigar = SIGAR_ENABLED ? SigarUtil.newSigar() : null;
  }

  public StatisticData[] retrieveStatisticData() {
    if (sigar == null) { return EMPTY_STATISTIC_DATA; }
    try {
      long bytesRead = 0;
      long bytesWrite = 0;
      long reads = 0;
      long writes = 0;

      FileSystem[] list = sigar.getFileSystemList();
      for (FileSystem element : list) {
        if (element.getType() == FileSystem.TYPE_LOCAL_DISK) {
          FileSystemUsage usage = sigar.getFileSystemUsage(element.getDirName());
          bytesRead += usage.getDiskReadBytes();
          bytesWrite += usage.getDiskWriteBytes();
          reads += usage.getDiskReads();
          writes += usage.getDiskWrites();
        }
      }
      List data = new ArrayList();
      data.add(new StatisticData(ACTION_NAME, ELEMENT_BYTES_READ, Long.valueOf(bytesRead)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_BYTES_WRITTEN, Long.valueOf(bytesWrite)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_READS, Long.valueOf(reads)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_WRITES, Long.valueOf(writes)));

      return (StatisticData[]) data.toArray(new StatisticData[data.size()]);

    } catch (SigarException e) {
      LOGGER.warn("Couldn't retrieve data for statistic '" + ACTION_NAME + "'", e);
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
