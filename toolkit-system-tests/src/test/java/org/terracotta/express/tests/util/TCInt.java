package org.terracotta.express.tests.util;

import java.io.Serializable;

public class TCInt implements Serializable, Comparable<TCInt> {
  private final int number;

  public TCInt(int i) {
    this.number = i;
  }

  public int getI() {
    return number;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + number;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TCInt other = (TCInt) obj;
    if (number != other.number) return false;
    return true;
  }

  @Override
  public String toString() {
    return "TCInt [i=" + number + "]";
  }

  @Override
  public int compareTo(TCInt param) {
    return new Integer(this.number).compareTo(param.number);
  }

}