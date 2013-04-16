package com.tc.objectserver.interest;

/**
 * @author Eugene Shelestovich
 */
public class ExpirationInterest implements Interest {
  private final Object key;

  public ExpirationInterest(final Object key) {
    this.key = key;
  }

  public Object getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "ExpirationInterest{" +
           "key=" + key +
           '}';
  }
}
