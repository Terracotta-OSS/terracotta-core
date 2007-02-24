package com.tctest.domain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ibatis.dao.client.DaoException;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.dao.client.template.JdbcDaoTemplate;

public class JdbcAccountDAO extends JdbcDaoTemplate implements AccountDAO {

	/**
	 * Constructor for JDBCContactDAO.
	 * 
	 * @param arg0
	 */
	public JdbcAccountDAO(DaoManager arg0) {
		super(arg0);
	}

	public int insertAccount(Account account) {
		// TODO Auto-generated method stub
		System.err.println("Inserting account: " + account);

		try {
			Connection conn = getConnection();
			PreparedStatement insertStmt = conn
					.prepareStatement("insert into ACCOUNT (ACC_ID, ACC_NUMBER) values (?, ?)");
			insertStmt.setInt(1, account.getId());
			insertStmt.setString(2, account.getNumber());
			insertStmt.executeUpdate();
		} catch (SQLException ex) {
			throw new DaoException(ex);
		}
		return account.getId();
	}

	public Account selectAccount(int accountId) {
		// TODO Auto-generated method stub
		System.err.println("Selecting account: " + accountId);
		try {
			Connection conn = getConnection();
			PreparedStatement selectStmt = conn
					.prepareStatement("select * from ACCOUNT where ACC_ID = ?");
			selectStmt.execute();
			ResultSet rs = selectStmt.getResultSet();
			rs.next();
			Account acc = new Account();
			acc.setId(rs.getInt(0));
			acc.setNumber(rs.getString(1));
			
			return acc;
		} catch (SQLException ex) {
			throw new DaoException(ex);
		}
	}

	public int updateAccount(Account account) {
		// TODO Auto-generated method stub
		System.err.println("Updating account: " + account);
		return 0;
	}
}
