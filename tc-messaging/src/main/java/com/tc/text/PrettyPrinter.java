/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
