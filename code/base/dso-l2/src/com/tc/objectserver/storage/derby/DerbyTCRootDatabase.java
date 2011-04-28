/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCRootDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DerbyTCRootDatabase extends AbstractDerbyTCDatabase implements TCRootDatabase {
  private final String rootNamesQuery;
  private final String getQuery;
  private final String rootNamesToIDQuery;
  private final String insertQuery;
  private final String updateQuery;
  private final String rootIDsQuery;
  private final String idFromNameQuery;

  public DerbyTCRootDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    getQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
    rootIDsQuery = "SELECT " + VALUE + " FROM " + tableName;
    rootNamesQuery = "SELECT " + KEY + " FROM " + tableName;
    rootNamesToIDQuery = "SELECT " + KEY + ", " + VALUE + " FROM " + tableName;
    insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?)";
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?";
    idFromNameQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
  }

  @Override
  protected final void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createRootDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public long get(byte[] rootName, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE "
      // + KEY + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getQuery);
      psSelect.setBytes(1, rootName);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return ObjectID.NULL_ID.toLong(); }
      long temp = rs.getLong(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root", e);
    } finally {
      closeResultSet(rs);
    }
  }

  public Set<ObjectID> getRootIds(PersistenceTransaction tx) {
    ResultSet rs = null;
    Set<ObjectID> set = new HashSet<ObjectID>();

    try {
      // "SELECT " + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, rootIDsQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(new ObjectID(rs.getLong(1)));
      }
      return set;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public List<byte[]> getRootNames(PersistenceTransaction tx) {
    ResultSet rs = null;
    ArrayList<byte[]> list = new ArrayList<byte[]>();
    try {
      // "SELECT " + KEY + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, rootNamesQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        list.add(rs.getBytes(1));
      }
      return list;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx) {
    ResultSet rs = null;
    Map<byte[], Long> map = new HashMap<byte[], Long>();
    try {
      // "SELECT " + KEY + ", " + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, rootNamesToIDQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        map.put(rs.getBytes(1), rs.getLong(2));
      }
      return map;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root map", e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public Status put(byte[] rootName, long id, PersistenceTransaction tx) {
    if (get(rootName, tx) == ObjectID.NULL_ID.toLong()) {
      return insert(rootName, id, tx);
    } else {
      return update(rootName, id, tx);
    }
  }

  private Status insert(byte[] rootName, long id, PersistenceTransaction tx) {
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      PreparedStatement psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setBytes(1, rootName);
      psPut.setLong(2, id);
      if (psPut.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (SQLException e) {
      throw new DBException("Could not put root", e);
    }
  }

  private Status update(byte[] rootName, long id, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setLong(1, id);
      psUpdate.setBytes(2, rootName);
      if (psUpdate.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public long getIdFromName(byte[] rootName, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE "
      // + KEY + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, idFromNameQuery);
      psSelect.setBytes(1, rootName);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return ObjectID.NULL_ID.toLong(); }
      long temp = rs.getLong(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      closeResultSet(rs);
    }
  }

}
