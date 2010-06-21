package com.tc.license;

import java.util.Date;

public interface License {

  public String licenseType();

  public String licenseNumber();

  public String licensee();

  public String product();
  
  public String edition();

  public int maxClients();

  public Date expirationDate();

  public Capabilities capabilities();
  
  public String getSignature();
  
  public void setSignature(String signature);

  public String toString();
  
  public byte[] getCanonicalData();
}
