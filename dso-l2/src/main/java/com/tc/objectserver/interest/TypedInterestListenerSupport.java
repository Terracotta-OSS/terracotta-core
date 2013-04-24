package com.tc.objectserver.interest;

/**
 * This superclass should be used, if client wants to handle only specific types of events.
 *
 * @author Eugene Shelestovich
 */
public abstract class TypedInterestListenerSupport implements InterestListener {

  @Override
  public final void onInterest(final Interest interest) {
    switch (interest.getType()) {
      case PUT:
        onPut((PutInterest)interest);
        break;
      case REMOVE:
        onRemove((RemoveInterest)interest);
        break;
      case EVICT:
        onEviction((EvictionInterest)interest);
        break;
      case EXPIRE:
        onExpiration((ExpirationInterest)interest);
        break;
      default:
        throw new IllegalArgumentException("Unknown interest type: " + interest.getType());
    }
  }

  public void onPut(PutInterest interest) {
  }

  public void onRemove(RemoveInterest interest) {
  }

  public void onEviction(EvictionInterest interest) {
  }

  public void onExpiration(ExpirationInterest interest) {
  }

}
