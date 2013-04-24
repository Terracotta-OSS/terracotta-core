package com.tc.object;

/**
 * @author Eugene Shelestovich
 */
public enum InterestType {
  PUT(0),
  REMOVE(1),
  EVICT(2),
  EXPIRE(3);

  private final int id;

  InterestType(int id) {
    this.id = id;
  }

  public static InterestType fromInt(int value) {
    switch (value) {
      case 0:
        return PUT;
      case 1:
        return REMOVE;
      case 2:
        return EVICT;
      case 3:
        return EXPIRE;
      default:
        throw new IllegalArgumentException("Unknown interest type: " + value);
    }
  }

  public int toInt() {
    return id;
  }
}
