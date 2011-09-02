/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

public class BerkeleyDBStatisticsHandler {

  private static final int     LEFT   = 1;
  private static final int     RIGHT  = 2;
  private static final int     CENTER = 3;

  private final Environment    env;
  private boolean              header = true;
  private long                 keyTotal;
  private long                 valuesTotal;
  private long                 grandTotal;
  private long                 totalCount;
  private final Writer         writer;
  private final DatabaseConfig dbc;

  public BerkeleyDBStatisticsHandler(Environment env, Writer writer) throws Exception {
    this.env = env;
    this.writer = writer;
    this.dbc = new DatabaseConfig();
    this.dbc.setReadOnly(true);
    this.dbc.setTransactional(true);
  }

  public void report() throws DatabaseException {
    List dbs = env.getDatabaseNames();
    log("Databases in the environment : " + dbs);

    log("\nReport on individual databases :\n================================\n");
    for (Iterator i = dbs.iterator(); i.hasNext();) {
      String dbNAme = (String) i.next();
      Database db = env.openDatabase(null, dbNAme, dbc);
      DBStats stats = calculate(db);

      db.close();
      report(stats);
    }
    reportGrandTotals();
  }

  private void reportGrandTotals() {
    log("\n");
    log("   TOTAL : ", String.valueOf(totalCount), "", "", "", String.valueOf(keyTotal), "", "", "", String
        .valueOf(valuesTotal), String.valueOf(grandTotal));
  }

  private DBStats calculate(Database db) throws DatabaseException {
    CursorConfig config = new CursorConfig();
    Cursor c = db.openCursor(null, config);
    DBStats stats = new DBStats(db.getDatabaseName());
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    while (OperationStatus.SUCCESS.equals(c.getNext(key, value, LockMode.DEFAULT))) {
      stats.record(key.getData().length, value.getData().length);
    }
    c.close();
    return stats;
  }

  private void report(DBStats stats) {
    if (header) {
      log("DBName", "# Records", "Keys(Bytes)", "Values(Bytes)", "Total(Bytes)");
      log("", "", "min", "max", "avg", "total", "min", "max", "avg", "total", "");
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      header = false;
    }
    log(stats.getDatabaseName(), stats.getRecordCount(), stats.getKeyMin(), stats.getKeyMax(), stats.getKeyAvg(), stats
        .getTotalKeySize(), stats.getValueMin(), stats.getValueMax(), stats.getValueAvg(), stats.getTotalValueSize(),
        stats.getTotalSize());
    this.keyTotal += stats.getTotalKeySize();
    this.valuesTotal += stats.getTotalValueSize();
    this.grandTotal += stats.getTotalSize();
    this.totalCount += stats.getRecordCount();
  }

  private void log(String databaseName, long recordCount, long keyMin, long keyMax, long keyAvg, long totalKeySize,
                   long valueMin, long valueMax, long valueAvg, long totalValueSize, long totalSize) {
    log(databaseName, String.valueOf(recordCount), String.valueOf(keyMin), String.valueOf(keyMax), String
        .valueOf(keyAvg), String.valueOf(totalKeySize), String.valueOf(valueMin), String.valueOf(valueMax), String
        .valueOf(valueAvg), String.valueOf(totalValueSize), String.valueOf(totalSize));
  }

  private void log(String nameHeader, String countHeader, String keyHeader, String valueHeader, String sizeHeader) {
    log(format(nameHeader, 20, LEFT) + format(countHeader, 10, RIGHT) + format(keyHeader, 30, CENTER)
        + format(valueHeader, 30, CENTER) + format(sizeHeader, 15, RIGHT));
  }

  private void log(String databaseName, String count, String kmin, String kmax, String kavg, String kTot, String vmin,
                   String vmax, String vavg, String vTot, String totalSize) {
    log(format(databaseName, 20, LEFT) + format(count, 10, RIGHT) + format(kmin, 5, RIGHT) + format(kmax, 10, RIGHT)
        + format(kavg, 5, RIGHT) + format(kTot, 10, RIGHT) + format(vmin, 5, RIGHT) + format(vmax, 10, RIGHT)
        + format(vavg, 5, RIGHT) + format(vTot, 10, RIGHT) + format(totalSize, 15, RIGHT));
  }

  private static String format(String s, int size, int justification) {
    if (s == null || s.length() >= size) { return s; }
    int diff = size - s.length();
    if (justification == LEFT) {
      return s + createSpaces(diff);
    } else if (justification == RIGHT) {
      return createSpaces(diff) + s;
    } else {
      return createSpaces(diff / 2) + s + createSpaces(diff - (diff / 2));
    }
  }

  private static String createSpaces(int i) {
    StringBuffer sb = new StringBuffer();
    while (i-- > 0) {
      sb.append(' ');
    }
    return sb.toString();
  }

  public long getTotalCount() {
    return totalCount;
  }

  public long getGrandTotal() {
    return grandTotal;
  }

  public long getValuesTotal() {
    return valuesTotal;
  }

  public long getKeyTotal() {
    return keyTotal;
  }

  protected static final class DBStats {

    private long         count;
    private long         keySize;
    private long         valueSize;
    private long         minKey;
    private long         maxKey;
    private long         minValue;
    private long         maxValue;
    private final String databaseName;

    public DBStats(String databaseName) {
      this.databaseName = databaseName;
    }

    public long getValueAvg() {
      return (count == 0 ? 0 : valueSize / count);
    }

    public long getValueMax() {
      return maxValue;
    }

    public long getValueMin() {
      return minValue;
    }

    public long getKeyAvg() {
      return (count == 0 ? 0 : keySize / count);
    }

    public long getKeyMax() {
      return maxKey;
    }

    public long getKeyMin() {
      return minKey;
    }

    public long getTotalValueSize() {
      return valueSize;
    }

    public long getTotalKeySize() {
      return keySize;
    }

    public long getTotalSize() {
      return keySize + valueSize;
    }

    public String getValueStats() {
      return valueSize + "(" + minValue + "/" + maxValue + "/" + getValueAvg() + ")";
    }

    public String getKeyStats() {
      return keySize + "(" + minKey + "/" + maxKey + "/" + getKeyAvg() + ")";
    }

    public long getRecordCount() {
      return count;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void record(int kSize, int vSize) {
      count++;
      keySize += kSize;
      valueSize += vSize;
      if (minKey == 0 || minKey > kSize) {
        minKey = kSize;
      }
      if (maxKey < kSize) {
        maxKey = kSize;
      }
      if (minValue == 0 || minValue > vSize) {
        minValue = vSize;
      }
      if (maxValue < vSize) {
        maxValue = vSize;
      }
    }

  }

  protected void log(String message) {
    try {
      writer.write(message);
      writer.write("\n");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
