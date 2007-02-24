package com.tctest.domain;

import com.ibatis.dao.client.Dao;

public interface AccountDAO extends Dao {
	public int insertAccount(Account account);
    public int updateAccount(Account account);
    public Account selectAccount(int accountId);

}
