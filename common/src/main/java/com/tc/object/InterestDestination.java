package com.tc.object;

/**
 * @author Eugene Shelestovich
 */
public interface InterestDestination {
  String getDestinationName();

  void handleInterest(InterestType type, Object key);
}