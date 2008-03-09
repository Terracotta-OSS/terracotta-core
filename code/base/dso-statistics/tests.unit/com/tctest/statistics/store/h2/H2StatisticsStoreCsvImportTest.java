/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.store.h2;

import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.StatisticsStoreImportListener;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.test.TCTestCase;
import com.tc.test.TempDirectoryHelper;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;
import java.util.Random;

public class H2StatisticsStoreCsvImportTest extends TCTestCase {
  private StatisticsStore store;

  private Random random = new Random();

  public H2StatisticsStoreCsvImportTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }
  
  public void setUp() throws Exception {
    synchronized (random) {
      File tmp_dir_parent = new TempDirectoryHelper(getClass(), false).getDirectory();
      File tmp_dir = new File(tmp_dir_parent, "statisticsbuffer-" + random.nextInt() + "-" + System.currentTimeMillis());
      tmp_dir.mkdirs();
      store = new H2StatisticsStoreImpl(tmp_dir);
      store.open();
    }
  }

  public void tearDown() throws Exception {
    store.close();
  }

  public void testCsvImport() throws Exception {
    long start = System.currentTimeMillis();
    File file = new File("/Users/gbevin/Downloads/statistics-20080227233807.csv");
    Reader reader = new FileReader(file);
    store.importCsvStatistics(reader, new StatisticsStoreImportListener() {
      public void started() {
        System.out.println("started");
      }

      public void imported(final long count) {
        System.out.println("imported "+count);
      }

      public void optimizing() {
        System.out.println("optimizing");
      }

      public void finished(final long total) {
        System.out.println("finished "+total);
      }
    });
    long stop = System.currentTimeMillis();
    System.out.println("time "+((stop-start)/1000)+" seconds");
  }
}