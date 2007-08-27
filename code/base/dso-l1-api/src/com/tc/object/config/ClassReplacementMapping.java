/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.net.URL;

public interface ClassReplacementMapping {
  
  public String addMapping(final String originalClassName, final String replacementClassName, final URL replacementResource);
  
  public URL getReplacementResource(final String replacementClassName);
  
  public URL getReplacementResource(final String replacementClassName, final ClassLoader defaultClassLoader);
  
  public boolean hasReplacement(String originalClassName);

  public String getReplacementClassName(String original);
  
  public String getOriginalClassNameSlashes(String replacement);
  
  public String getOriginalAsmType(String replacement);

  public String ensureOriginalAsmTypes(String s);
}
