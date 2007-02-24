package com.tctest.domain;

import java.sql.SQLException;

import com.ibatis.dao.client.DaoException;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.dao.client.template.SqlMapDaoTemplate;

public class SqlMapCustomerDAO extends SqlMapDaoTemplate implements CustomerDAO {
	
	public SqlMapCustomerDAO(DaoManager daoManager) {
		super(daoManager);
	}

	public int insertCustomer(Customer customer) {
		try {
			getSqlMapExecutor().insert("insertCustomer", customer);
		} catch (SQLException e) {
	      throw new DaoException(e);
		}
		return 0;
	}

	public Customer selectCustomer(int customerId) {
		try {
			Customer cus = (Customer)getSqlMapExecutor().queryForObject("selectCustomerById", new Integer(customerId));
			return cus;
		} catch (SQLException e) {
	      throw new DaoException(e);
		}
	}

	public int updateCustomer(Customer customer) {
		// TODO Auto-generated method stub
		return 0;
	}
	
  public DaoManager getDaoManager() {
    return daoManager;
  }

}
