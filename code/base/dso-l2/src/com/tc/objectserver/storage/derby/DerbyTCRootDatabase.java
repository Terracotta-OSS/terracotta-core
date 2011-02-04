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
  public DerbyTCRootDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected final void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createRootDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public long get(byte[] rootName, PersistenceTransaction tx) {
    ResultSet rs = null;
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
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
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(new ObjectID(rs.getLong(1)));
      }
      return set;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      try {
        closeResultSet(rs);
        connection.commit();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  public List<byte[]> getRootNames(PersistenceTransaction tx) {
    ResultSet rs = null;
    ArrayList<byte[]> list = new ArrayList<byte[]>();
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        list.add(rs.getBytes(1));
      }
      return list;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      try {
        closeResultSet(rs);
        connection.commit();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx) {
    ResultSet rs = null;
    Map<byte[], Long> map = new HashMap<byte[], Long>();
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + ", " + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        map.put(rs.getBytes(1), rs.getLong(2));
      }
      return map;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root map", e);
    } finally {
      try {
        closeResultSet(rs);
        connection.commit();
      } catch (SQLException e) {
        // ignore
      }
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
    PreparedStatement psPut;
    Connection connection = pt2nt(tx);

    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setBytes(1, rootName);
      psPut.setLong(2, id);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException("Could not put root", e);
    }
    return Status.SUCCESS;

  }

  private Status update(byte[] rootName, long id, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setLong(1, id);
      psUpdate.setBytes(2, rootName);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public long getIdFromName(byte[] rootName, PersistenceTransaction tx) {
    ResultSet rs = null;
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
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
