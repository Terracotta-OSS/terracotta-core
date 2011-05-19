/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import java.io.IOException;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DerbyStatisticsHandler {
  private final static Set<String> SUPPORTED_COLUMN_TYPES = new HashSet<String>();

  static {
    SUPPORTED_COLUMN_TYPES.add(Integer.class.getName());
    SUPPORTED_COLUMN_TYPES.add(Long.class.getName());
    SUPPORTED_COLUMN_TYPES.add(String.class.getName());
    SUPPORTED_COLUMN_TYPES.add("byte[]");
    SUPPORTED_COLUMN_TYPES.add(Blob.class.getName());
  }

  private final Connection         connection;
  private final DerbyDBEnvironment dbEnvironment;
  private final List<TableStats>   tablesStat             = new ArrayList<TableStats>();
  private final Writer             writer;

  public DerbyStatisticsHandler(DerbyDBEnvironment dbEnvironment, Writer writer) throws Exception {
    this.dbEnvironment = dbEnvironment;
    this.connection = this.dbEnvironment.createConnection();
    this.writer = writer;
  }

  public void report() throws Exception {
    Iterator<String> tableNames = this.dbEnvironment.getTables().keySet().iterator();
    while (tableNames.hasNext()) {
      // For a table get the result set
      String tableName = tableNames.next();
      String query = "SELECT * FROM " + tableName;
      PreparedStatement statement = connection.prepareStatement(query);
      ResultSet rs = statement.executeQuery();

      // get the column names and the classnames
      final ArrayList<String> classNames = new ArrayList<String>();
      final ArrayList<String> columnNames = new ArrayList<String>();
      getColumnNamesAndClassNames(rs, classNames, columnNames);

      TableStats stats = new TableStats(tableName, columnNames, classNames);
      tablesStat.add(stats);

      // print the table details now
      scanTableAndCollectStats(rs, tableName, classNames, columnNames, stats);
    }

    printTableStats();
    closeConnection();
  }

  private void closeConnection() throws Exception {
    connection.commit();
    connection.close();
  }

  private void printTableStats() {
    log("Printing DERBY Database Stats");
    for (TableStats tableStats : tablesStat) {
      log("====================================");
      log(tableStats.toString());
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

  private void getColumnNamesAndClassNames(ResultSet rs, final ArrayList<String> classNames,
                                           final ArrayList<String> columnNames) throws SQLException {
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      String classname = metaData.getColumnClassName(i);
      String columnName = metaData.getColumnName(i);
      if (!SUPPORTED_COLUMN_TYPES.contains(classname)) { throw new AssertionError("Class not supported -- " + classname); }
      classNames.add(classname);
      columnNames.add(columnName);
    }
  }

  private void scanTableAndCollectStats(ResultSet rs, String tableName, ArrayList<String> classNames,
                                        ArrayList<String> columnNames, TableStats tableStats) throws SQLException {
    final int columnSize = classNames.size();
    while (rs.next()) {
      for (int i = 0; i < columnSize; i++) {
        int tempLength = 0;
        String columnClass = classNames.get(i);
        if (columnClass.equals(String.class.getName())) {
          String str = rs.getString(i + 1);
          if (str != null) {
            tempLength = str.length();
          }
        } else if (columnClass.equals(Integer.class.getName())) {
          tempLength = 4;
        } else if (columnClass.equals(Long.class.getName())) {
          tempLength = 8;
        } else if (columnClass.equals("byte[]")) {
          byte[] b = rs.getBytes(i + 1);
          if (b != null) {
            tempLength = b.length;
          }
        } else if (columnClass.equals(Blob.class.getName())) {
          byte[] b = rs.getBytes(i + 1);
          if (b != null) {
            tempLength = b.length;
          }
        } else {
          throw new AssertionError("Unsupported class");
        }
        tableStats.updateLength(i, tempLength);
      }
    }
  }

  private static class TableStats {
    private final String            tableName;
    private final List<ColumnStats> columnsStat = new ArrayList<ColumnStats>();

    public TableStats(String tableName, List<String> columnNames, List<String> classNames) {
      this.tableName = tableName;
      int columnSize = columnNames.size();

      for (int i = 0; i < columnSize; i++) {
        columnsStat.add(new ColumnStats(columnNames.get(i), classNames.get(i)));
      }
    }

    public void updateLength(int i, int length) {
      this.columnsStat.get(i).updateLength(length);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Table name: ");
      builder.append(tableName);
      builder.append("\n");
      builder.append("Column name: ");
      builder.append("\t\t");
      builder.append("Class: ");
      builder.append("\t\t");
      builder.append("Max (bytes): ");
      builder.append("\t\t");
      builder.append("Min (bytes): ");
      builder.append("\t\t");
      builder.append("Total Rows: ");
      builder.append("\t\t");
      builder.append("Total (bytes): ");
      builder.append("\t\t");
      builder.append("Average (bytes): ");
      builder.append("\n");

      for (ColumnStats columnStats : columnsStat) {
        if (columnStats.totalRows > 0) {
          builder.append(columnStats.name);
          builder.append("\t\t");
          builder.append(columnStats.classType);
          builder.append("\t\t");
          builder.append(columnStats.maxSize);
          builder.append("\t\t");
          builder.append(columnStats.minSize);
          builder.append("\t\t");
          builder.append(columnStats.totalRows);
          builder.append("\t\t");
          builder.append(columnStats.totalSize);
          builder.append("\t\t");
          long average = columnStats.totalSize / columnStats.totalRows;
          builder.append(average);
          builder.append("\n");
        }
      }

      return builder.toString();
    }
  }

  private static class ColumnStats {
    private final String name;
    private final String classType;
    private long         maxSize   = Long.MIN_VALUE;
    private long         minSize   = Long.MAX_VALUE;
    private long         totalSize = 0;
    private long         totalRows = 0;

    public ColumnStats(String name, String classType) {
      this.name = name;
      this.classType = classType;
    }

    private void updateLength(int length) {
      maxSize = length > maxSize ? length : maxSize;
      minSize = length <= minSize ? length : minSize;
      totalSize += length;
      totalRows++;
    }
  }

}
