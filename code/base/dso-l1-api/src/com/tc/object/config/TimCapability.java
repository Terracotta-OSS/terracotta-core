/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

public enum TimCapability {

  // This is a less than elegant way for TIMs to declare to the L1 config that certain "capabilities" are enabled.
  // Basically it gives TIMs a reasonably strong means to indicate that they are actually loaded. Only those TIMs that
  // need to express something like this do so (ie. not all TIMs have an associated capability)

  CGLIB, SESSIONS;

}
