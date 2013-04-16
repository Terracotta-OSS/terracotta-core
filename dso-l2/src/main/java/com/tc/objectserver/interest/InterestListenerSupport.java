package com.tc.objectserver.interest;

/**
 * A convinient base class if a consumer is interested in only a specific types of events.
 *
 * @author Eugene Shelestovich
 */
public class InterestListenerSupport implements InterestListener {

  @Override
  public void onPut(final PutInterest interest) {
  }

  @Override
  public void onRemove(final RemoveInterest interest) {
  }

  @Override
  public void onEviction(final EvictionInterest interest) {
  }

  @Override
  public void onExpiration(final ExpirationInterest interest) {
  }
}
