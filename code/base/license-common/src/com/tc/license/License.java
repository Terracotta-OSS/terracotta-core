package com.tc.license;

import java.util.Date;

public interface License {

  public String licenseType();

  public String licenseNumber();

  public String licensee();

  public String product();

  public int maxClients();

  public Date expirationDate();

  public Capabilities capabilities();

  public byte[] getCanonicalData();

  public String toString();

  public String getSignature();

  public void setSignature(String signature);
}
