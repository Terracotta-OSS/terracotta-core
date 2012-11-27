package org.terracotta.express.tests.rejoin;

import java.io.Serializable;

public class TCInt implements Serializable, Comparable {
  private final int i;

  public TCInt(int i) {
    this.i = i;
  }

  public int getI() {
    return i;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + i;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TCInt other = (TCInt) obj;
    if (i != other.i) return false;
    return true;
  }

  @Override
  public String toString() {
    return "MyInt [i=" + i + "]";
  }

  @Override
  public int compareTo(Object paramT) {
    if (paramT == null) { throw new NullPointerException(); }
    if (paramT instanceof TCInt) {
      TCInt myInt = (TCInt) paramT;
      if (this.i > myInt.i) {
        return 1;
      } else if (this.i == myInt.i) {
        return 0;
      } else {
        return -1;
      }
    } else {
      throw new ClassCastException("Invalid object of class : " + paramT.getClass().getName() + "classLoader : "
                                   + paramT.getClass().getClassLoader() + " this.class: " + this.getClass().getName()
                                   + " this.classloader : " + this.getClass().getClassLoader());
    }
  }

}