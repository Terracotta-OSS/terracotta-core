package com.tc.object;

/**
 * @author Eugene Shelestovich
 */
public enum InterestType {
  PUT(0),
  UPDATE(1),
  REMOVE(2),
  EVICT(3),
  EXPIRE(4);

  private final int id;

  InterestType(int id) {
    this.id = id;
  }

  public static InterestType fromInt(int value) {
    switch (value) {
      case 0:
        return PUT;
      case 1:
        return UPDATE;
      case 2:
        return REMOVE;
      case 3:
        return EVICT;
      case 4:
        return EXPIRE;
      default:
        throw new IllegalArgumentException("Unknown interest type: " + value);
    }
  }

  public int toInt() {
    return id;
  }
}
