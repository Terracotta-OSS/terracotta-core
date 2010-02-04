/**

 All content copyright (c) 2003-2008 Terracotta, Inc.,
 except as may otherwise be noted in a separate copyright notice.
 All rights reserved.

 */
package org.terracotta;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;

import java.io.Serializable;

public class Service {

  private static final CacheManager cacheManager    = new CacheManager();
  private        final String       creationContext;

  public Service(String creationContext) {
    Ehcache undecoratedCache = getCache();
    if (!(undecoratedCache instanceof BlockingCache)) {
      cacheManager.replaceCacheWithDecoratedCache(undecoratedCache, new BlockingCache(undecoratedCache));
    }
    this.creationContext = creationContext;
  }

  public AccountView getAccountViewFor(final String accountIdentifier) {
    final AccountViewImpl accountView = new AccountViewImpl(java.text.NumberFormat.getCurrencyInstance());
    getAccountFor(accountIdentifier).visitAccountView(accountView);
    return accountView;
  }

  /**
   * @param accountIdentifier An account id that IS in the cache
   * @param delta             The amount to add
   */
  public void updateAccountBalanceFor(final String accountIdentifier, final int delta) {
    AccountEntity account = getAccountFor(accountIdentifier);
    account.updateBalance(delta);
    getCache().put(new Element(accountIdentifier, account));
  }

  private AccountEntity getAccountFor(final String accountIdentifier) {
    AccountEntity cacheEntry;
    Element element = getCache().get(accountIdentifier);
    if (element == null) {
      cacheEntry = new AccountEntity(creationContext, accountIdentifier, 1000);
      getCache().put(new Element(accountIdentifier, cacheEntry));
    } else {
      cacheEntry = (AccountEntity)element.getValue();
    }
    return cacheEntry;
  }

  private Ehcache getCache() {
    return cacheManager.getEhcache("account-cache");
  }

  static final class AccountEntity implements Serializable {
    private final String creationContext;
    private final long timeCached;
    private final String accountIdentifier;
    private double accountBalance;

    public AccountEntity(final String creationContext, final String id, final long initialAccountBalance) {
      this.creationContext = creationContext;
      this.timeCached = System.currentTimeMillis();
      this.accountIdentifier = id;
      this.accountBalance = initialAccountBalance;
    }

    public String getAccountIdentifier() {
      return this.accountIdentifier;
    }

    public synchronized void updateBalance(final int delta) {
      // NOTE: If this were a database-backed entity, you would need to write back to the database.  You can do that transparently
      // with Terracotta for Hibernate.  You can also use Terracotta's tim-async module to write to a reliable, asynchronous database update queue.
      this.accountBalance += delta;
    }

    public synchronized VisitableAccountBean visitAccountView(final VisitableAccountBean bean) {
      bean.setCreationContext(creationContext);
      bean.setTimeCached(timeCached);
      bean.setAccountIdentifier(accountIdentifier);
      bean.setAccountBalance(accountBalance);
      return bean;
    }
  }

  /**
   * A read-only view of a snapshot of an account entity.
   */
  static final class AccountViewImpl implements AccountView, VisitableAccountBean {
    private final java.text.NumberFormat cfmt;
    private String creationContext = "UNKNOWN";
    private long timeCached;
    private String accountIdentifier = "UNKNOWN";
    private String accountBalance;

    AccountViewImpl(final java.text.NumberFormat currencyFormat) {
      cfmt = currencyFormat;
    }

    public String getCreationContext() {
      return creationContext;
    }

    public void setCreationContext(final String s) {
      this.creationContext = s;
    }

    public String getTimeCached() {
      return new java.util.Date(timeCached).toString();
    }

    public void setTimeCached(final long t) {
      this.timeCached = t;
    }

    public String getAccountIdentifier() {
      return accountIdentifier;
    }

    public void setAccountIdentifier(final String s) {
      this.accountIdentifier = s;
    }

    public String getAccountBalance() {
      return accountBalance;
    }

    public void setAccountBalance(double d) {
      this.accountBalance = cfmt.format(d);
    }
  }

  public static interface AccountView {
    public String getCreationContext();

    public String getTimeCached();

    public String getAccountIdentifier();

    public String getAccountBalance();
  }

  static interface VisitableAccountBean {
    public void setCreationContext(String creationContext);

    public void setTimeCached(long timeCached);

    public void setAccountIdentifier(String id);

    public void setAccountBalance(double d);
  }

}
