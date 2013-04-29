package com.tc.objectserver.interest;

import com.tc.object.InterestType;

/**
 * @author Eugene Shelestovich
 */
public class ExpirationInterest extends CacheInterest {
  private final Object key;

  public ExpirationInterest(final Object key, final String cacheName) {
    super(cacheName);
    this.key = key;
  }

  @Override
  public InterestType getType() {
    return InterestType.EXPIRE;
  }

  public Object getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "ExpirationInterest{" +
           "key=" + key +
           ", cacheName='" + cacheName + '\'' +
           '}';
  }
}
