/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.text;

public interface PrettyPrinter {

  public void autoflush(boolean b);

  public boolean autoflush();
  
  public PrettyPrinter print(Object o);

  public PrettyPrinter println(Object o);

  public PrettyPrinter println();

  public PrettyPrinter indent();

  public PrettyPrinter duplicateAndIndent();
  
  public PrettyPrinter visit(Object o);
  
  public void flush();
}
