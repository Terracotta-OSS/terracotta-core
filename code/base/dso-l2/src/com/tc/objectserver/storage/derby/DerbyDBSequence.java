/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class DerbyDBSequence implements MutableSequence {
  public static final String                        SEQUENCE_TABLE = "sequenceTable";
  private static final String                       SEUQENCE_NAME  = "sequenceName";
  private static final String                       SEQUENCE_UID   = "sequenceUid";
  private static final String                       SEQUENCE_VALUE = "sequenceValue";

  private final String                              entryName;
  private final String                              uid;
  private final DerbyPersistenceTransactionProvider ptxp;

  private static final String                       SET_UID_QUERY  = "SELECT " + SEQUENCE_UID + " FROM "
                                                                     + SEQUENCE_TABLE + " WHERE " + SEUQENCE_NAME
                                                                     + " = ?";
  private static final String                       SET_NEXT_QUERY = "UPDATE " + SEQUENCE_TABLE + " SET "
                                                                     + SEQUENCE_VALUE + " = ? " + " WHERE "
                                                                     + SEUQENCE_NAME + " = ?";
  private static final String                       CURRENT_QUERY  = "SELECT " + SEQUENCE_VALUE + " FROM "
                                                                     + SEQUENCE_TABLE + " WHERE " + SEUQENCE_NAME
                                                                     + " = ?";
  private static final String                       INSERT_QUERY   = "INSERT INTO " + SEQUENCE_TABLE
                                                                     + " VALUES (?, ?, ?)";
  private static final String                       EXISTS_QUERY   = "SELECT " + SEQUENCE_VALUE + " FROM "
                                                                     + SEQUENCE_TABLE + " WHERE " + SEUQENCE_NAME
                                                                     + " = ?";

  /**
   * Assuming connection is already open and sequence table has already been created.
   */
  public DerbyDBSequence(DerbyPersistenceTransactionProvider ptxp, String entryName, int startValue)
      throws SQLException {
    this.entryName = entryName;
    this.ptxp = ptxp;
    if (startValue < 0) throw new IllegalArgumentException("start value cannot be < 0");

    createSequenceIfNeccesary(startValue);
    this.uid = setUID();
  }

  private String setUID() throws SQLException {
    ResultSet rs = null;
    PersistenceTransaction txn = ptxp.newTransaction();
    String seqID = null;

    try {
      PreparedStatement psSelect = AbstractDerbyTCDatabase.getOrCreatePreparedStatement(txn, SET_UID_QUERY);
      psSelect.setString(1, entryName);
      rs = psSelect.executeQuery();

      if (rs.next()) {
        seqID = rs.getString(SEQUENCE_UID);
      }
    } finally {
      closeResultSet(rs);
    }
    txn.commit();

    if (seqID == null) { throw new IllegalStateException("Invalid state for sequence"); }
    return seqID;
  }

  public String getUID() {
    return this.uid;
  }

  public synchronized long nextBatch(long batchSize) {
    long current = current();
    setNext(current + batchSize);

    return current;
  }

  public synchronized void setNext(long next) {
    long current = current();
    if (current > next) { throw new AssertionError("Current = " + current + " Next = " + next); }

    try {
      PersistenceTransaction txn = ptxp.newTransaction();
      PreparedStatement psUpdate = AbstractDerbyTCDatabase.getOrCreatePreparedStatement(txn, SET_NEXT_QUERY);
      psUpdate.setLong(1, next);
      psUpdate.setString(2, entryName);
      psUpdate.executeUpdate();

      txn.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized long current() {
    ResultSet rs = null;
    PreparedStatement psSelect;
    try {
      PersistenceTransaction txn = ptxp.newTransaction();
      psSelect = AbstractDerbyTCDatabase.getOrCreatePreparedStatement(txn, CURRENT_QUERY);
      psSelect.setString(1, entryName);
      rs = psSelect.executeQuery();

      if (!rs.next()) { throw new IllegalStateException("The value should always exist"); }

      long current = rs.getLong(1);
      rs.close();
      txn.commit();
      return current;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized long next() {
    return nextBatch(1);
  }

  public void createSequenceIfNeccesary(int startVal) throws SQLException {
    if (exists()) { return; }

    PersistenceTransaction txn = ptxp.newTransaction();
    PreparedStatement psPut = AbstractDerbyTCDatabase.getOrCreatePreparedStatement(txn, INSERT_QUERY);
    psPut.setString(1, entryName);
    psPut.setLong(2, startVal);
    psPut.setString(3, UUID.getUUID().toString());

    psPut.executeUpdate();
    txn.commit();
  }

  private boolean exists() throws SQLException {
    ResultSet rs = null;
    PersistenceTransaction txn = ptxp.newTransaction();

    boolean exists = false;
    try {
      PreparedStatement psSelect = AbstractDerbyTCDatabase.getOrCreatePreparedStatement(txn, EXISTS_QUERY);
      psSelect.setString(1, entryName);
      rs = psSelect.executeQuery();

      exists = rs.next() ? true : false;
    } finally {
      closeResultSet(rs);
    }
    txn.commit();
    return exists;
  }

  /**
   * This method should be called atleast once so that the sequence table exists before creating 1 sequence.
   */
  public static void createSequenceTable(Connection connection) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, SEQUENCE_TABLE)) { return; }

    Statement statement = null;
    try {
      statement = connection.createStatement();
      String query = "CREATE TABLE " + SEQUENCE_TABLE + "(" + SEUQENCE_NAME + " VARCHAR (50), " + SEQUENCE_VALUE
                     + " BIGINT, " + SEQUENCE_UID + " VARCHAR (50), PRIMARY KEY(" + SEUQENCE_NAME + ") )";
      statement.execute(query);
    } finally {
      if (statement != null) {
        statement.close();
      }
    }
    connection.commit();
  }

  private void closeResultSet(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        //
      }
    }
  }
}
