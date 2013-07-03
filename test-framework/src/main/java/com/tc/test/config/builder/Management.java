package com.tc.test.config.builder;

/**
 * @author Ludovic Orban
 */
public class Management {

  private String ia;

  public String getIa() {
    return ia;
  }

  public void setIa(String ia) {
    this.ia = ia;
  }

  public Management ia(String ia) {
    setIa(ia);
    return this;
  }

}
