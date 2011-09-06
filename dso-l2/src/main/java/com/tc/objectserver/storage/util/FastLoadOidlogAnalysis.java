/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.util.Conversion;
import com.tc.util.OidLongArray;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FastLoadOidlogAnalysis extends BaseUtility {

  private static final int        LEFT             = 1;
  private static final int        RIGHT            = 2;
  private static final int        CENTER           = 3;

  private final EnvironmentConfig enc;
  private final Environment       env;
  private final DatabaseConfig    dbc;
  protected List                  oidlogsStatsList = new ArrayList();

  public FastLoadOidlogAnalysis(File dir) throws Exception {
    this(dir, new OutputStreamWriter(System.out));
  }

  public FastLoadOidlogAnalysis(File dir, Writer writer) throws Exception {
    super(writer, new File[] {});
    this.enc = new EnvironmentConfig();
    this.enc.setReadOnly(true);
    this.env = new Environment(dir, enc);
    this.dbc = new DatabaseConfig();
    this.dbc.setReadOnly(true);
  }

  public void report() {
    List dbs = env.getDatabaseNames();

    log(" ");
    log("\nAnalysis of oid databases :\n================================\n");

    for (Iterator i = dbs.iterator(); i.hasNext();) {
      String dbName = (String) i.next();
      if (dbName.equals("oid_store_log")) {
        Database db = env.openDatabase(null, dbName, dbc);
        OidlogsStats stats = new OidlogsStats(dbName);
        stats.analyze(db);
        oidlogsStatsList.add(stats);
        db.close();
        stats.report();
      }
      if (dbName.equals("objects_oid_store") || dbName.equals("mapsdatabase_oid_store")) {
        Database db = env.openDatabase(null, dbName, dbc);
        OidStoreStats stats = new OidStoreStats(dbName);
        stats.analyze(db);
        db.close();
        stats.report();
      }
    }
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

  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      usage();
      System.exit(1);
    }

    try {
      File dir = new File(args[0]);
      validateDir(dir);
      // db usage
      DBUsage reporter = new DBUsage(dir);
      reporter.report();
      // OidLogs analysis
      FastLoadOidlogAnalysis analysis = new FastLoadOidlogAnalysis(dir);
      analysis.report();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: FastLoadOidlogAnalysis <environment home directory>");
  }

  protected abstract class AbstractOidStats {
    private final String databaseName;

    public AbstractOidStats(String databaseName) {
      this.databaseName = databaseName;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void analyze(Database db) {
      CursorConfig config = new CursorConfig();
      Cursor c = db.openCursor(null, config);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      try {
        while (OperationStatus.SUCCESS.equals(c.getNext(key, value, LockMode.DEFAULT))) {
          record(key.getData(), value.getData());
        }
      } catch (TCDatabaseException e) {
        log("Bad database " + db.getDatabaseName() + " " + e);
      } finally {
        c.close();
      }
    }

    abstract public void record(byte[] key, byte[] value) throws TCDatabaseException;

    abstract public void report();

  }

  protected final class OidlogsStats extends AbstractOidStats {
    private long    addCount;
    private long    deleteCount;
    private long    startSequence;
    private long    endSequence;

    private boolean hasStartSeq = false;

    public OidlogsStats(String databaseName) {
      super(databaseName);
    }

    public long getAddCount() {
      return addCount;
    }

    public long getDeleteCount() {
      return deleteCount;
    }

    public long getStartSeqence() {
      return startSequence;
    }

    public long getEndSequence() {
      return endSequence;
    }

    private boolean isAddOper(byte[] key) {
      return (key[OidLongArray.BYTES_PER_LONG] == 0);
    }

    public void record(byte[] key, byte[] value) throws TCDatabaseException {

      // key must be a long and a byte
      if ((OidLongArray.BYTES_PER_LONG + 1) != key.length) { throw new TCDatabaseException("Wrong key size"); }

      if (isAddOper(key)) {
        ++addCount;
      } else {
        ++deleteCount;
      }

      if (!hasStartSeq) {
        startSequence = Conversion.bytes2Long(key);
        hasStartSeq = true;
      } else {
        endSequence = Conversion.bytes2Long(key);
        if (endSequence <= startSequence) { throw new TCDatabaseException("Wrong order of sequence"); }
      }
    }

    private void sublog(String nameHeader, String countHeader, String keyHeader, String valueHeader, String sizeHeader) {
      log(format(nameHeader, 20, LEFT) + format(countHeader, 10, RIGHT) + format(keyHeader, 30, CENTER)
          + format(valueHeader, 30, CENTER) + format(sizeHeader, 15, RIGHT));
    }

    public void report() {
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      sublog("DBName", "# ADD", "# DEL", "Start Sequence", "End Sequence");
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      sublog(getDatabaseName(), String.valueOf(getAddCount()), String.valueOf(getDeleteCount()), String
          .valueOf(getStartSeqence()), String.valueOf(getEndSequence()));
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }

  }

  protected final class OidStoreStats extends AbstractOidStats {
    private long totalRecord = 0;
    private long totalBitsOn = 0;

    public OidStoreStats(String databaseName) {
      super(databaseName);
    }

    public long getTotalRecords() {
      return totalRecord;
    }

    public long getTotalBitsOn() {
      return totalBitsOn;
    }

    public void record(byte[] key, byte[] value) throws TCDatabaseException {
      // sanity check, key must be a long, value must be a long array
      if (OidLongArray.BYTES_PER_LONG != key.length) { throw new TCDatabaseException("Wrong key size!"); }
      if (0 != (value.length % OidLongArray.BYTES_PER_LONG)) { throw new TCDatabaseException("Wrong value size!"); }

      ++totalRecord;
      // check on bits
      for (int i = 0; i < value.length; ++i) {
        byte cmp = (byte) 1;
        byte b = value[i];
        for (int j = 0; j < 8; ++j) {
          if ((b & cmp) != 0) ++totalBitsOn;
          cmp <<= 1;
        }
      }
    }

    private void sublog(String nameHeader, String countHeader, String keyHeader) {
      log(format(nameHeader, 20, LEFT) + format(countHeader, 10, RIGHT) + format(keyHeader, 30, CENTER));
    }

    public void report() {
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      sublog("DBName", "# records", "# bits on");
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      sublog(getDatabaseName(), String.valueOf(getTotalRecords()), String.valueOf(getTotalBitsOn()));
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

    }

  }

}
