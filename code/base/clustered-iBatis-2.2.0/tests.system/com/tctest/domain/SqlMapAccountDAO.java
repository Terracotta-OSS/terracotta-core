package com.tctest.domain;

import java.sql.SQLException;

import com.ibatis.dao.client.DaoException;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.dao.client.template.SqlMapDaoTemplate;

public class SqlMapAccountDAO extends SqlMapDaoTemplate implements AccountDAO {
	
	public SqlMapAccountDAO(DaoManager daoManager) {
		super(daoManager);
	}

	public int insertAccount(Account account) {
		try {
			getSqlMapExecutor().insert("insertAccount", account);
		} catch (SQLException e) {
	      throw new DaoException(e);
		}
		return 0;
	}

	public Account selectAccount(int accountId) {
		try {
			Account acc = (Account)getSqlMapExecutor().queryForObject("selectAccountById", new Integer(accountId));
			return acc;
		} catch (SQLException e) {
	      throw new DaoException(e);
		}
	}

	public int updateAccount(Account account) {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
