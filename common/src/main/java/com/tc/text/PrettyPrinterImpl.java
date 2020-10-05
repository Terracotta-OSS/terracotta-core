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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

public class PrettyPrinterImpl implements PrettyPrinter {

  private static final String   INDENT        = "--> ";

  private final StringBuffer    prefix;
  private final PrintWriter     out;
  private final IdentityHashMap<Object, String> visited;

  private final PrintPolicy     defaultPolicy = new BasicPrintPolicy();
  private final Collection<PrintPolicy> policies;

  private boolean               autoflush     = true;

  public PrettyPrinterImpl(PrintWriter out) {
    this(INDENT, out, new IdentityHashMap<Object, String>());
  }

  private PrettyPrinterImpl(String prefix, PrintWriter out, IdentityHashMap<Object, String> visited) {
    this.prefix = new StringBuffer(prefix);
    this.out = out;
    this.visited = visited;
    this.policies = initPolicies();
  }

  public synchronized void autoflush(boolean b) {
    this.autoflush = b;
  }

  public synchronized boolean autoflush() {
    return this.autoflush;
  }

  /**
   * Returns true if the object has been visited before or if the object is null. Otherwise, it accounts for the visited
   * object and returns false.
   */
  private boolean accountFor(Object o) {
    if (o == null) return false;
    synchronized (visited) {
      if (visited.containsKey(o)) {
        return true;
      } else {
        visited.put(o, "");
        return false;
      }
    }
  }

  public PrettyPrinter print(Object o) {
    this.out.print(o);
    if (autoflush()) this.out.flush();
    return this;
  }

  @Override
  public PrettyPrinter println(Object o) {
    this.out.println(o);
    if (autoflush()) this.out.flush();
    return this;
  }

  public PrettyPrinter println() {
    this.out.println();
    if (autoflush()) this.out.flush();
    return this;
  }

  public PrettyPrinter indent() {
    return print(prefix);
  }

  public PrettyPrinter duplicateAndIndent() {
    PrettyPrinterImpl rv = duplicate();
    rv.indentPrefix();
    return rv;
  }

  private void indentPrefix() {
    if (prefix.indexOf("+") > -1) prefix.replace(prefix.indexOf("+"), prefix.indexOf("+") + 1, "|");
    prefix.insert(prefix.indexOf("-->"), "    +");
  }

  private PrettyPrinterImpl duplicate() {
    PrettyPrinterImpl prettyPrinterImpl = new PrettyPrinterImpl(prefix.toString(), out, this.visited);
    prettyPrinterImpl.autoflush(autoflush);
    return prettyPrinterImpl;
  }

  public PrettyPrinter visit(Object o) {
    if (accountFor(o)) {
      print("ALREADY VISITED: " + o);
      return this;
    } else {
      return basicVisit(o);
    }
  }

  private PrettyPrinter basicVisit(Object o) {
    PrintPolicy policy = findPolicyFor(o);
    return policy.visit(this, o);
  }

  private PrintPolicy findPolicyFor(Object o) {
    if (o == null) return defaultPolicy;
    for (PrintPolicy policy : policies) {
      if (policy.accepts(o)) { return policy; }
    }
    return defaultPolicy;
  }

  /**
   * Creates a policy path. Each policy is searched in order.
   */
  private Collection<PrintPolicy> initPolicies() {
    Collection<PrintPolicy> rv = new ArrayList<PrintPolicy>();
    rv.add(new PrettyPrintablePrintPolicy());
    rv.add(new ShallowMapPrintPolicy());
    rv.add(new ShallowCollectionPrintPolicy());
    rv.add(defaultPolicy);
    return rv;
  }

  private static interface PrintPolicy {
    public PrettyPrinter visit(PrettyPrinter pp, Object o);

    public boolean accepts(Object o);
  }

  private static class PrettyPrintablePrintPolicy implements PrintPolicy {

    @Override
    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return ((PrettyPrintable) o).prettyPrint(pp);
    }

    @Override
    public boolean accepts(Object o) {
      return o != null && o instanceof PrettyPrintable;
    }

  }

  private static class ShallowMapPrintPolicy implements PrintPolicy {

    @Override
    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return pp.println(o.getClass().getName()).println(".size()=").println(((Map<?, ?>) o).size() + "");
    }

    @Override
    public boolean accepts(Object o) {
      return o != null && o instanceof Map;
    }

  }

  private static class ShallowCollectionPrintPolicy implements PrintPolicy {

    @Override
    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return pp.println(o.getClass().getName()).println(".size()=").println(((Collection<?>) o).size() + "");
    }

    @Override
    public boolean accepts(Object o) {
      return o != null && o instanceof Collection;
    }

  }

  private static class BasicPrintPolicy implements PrintPolicy {

    @Override
    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return pp.println(o);
    }

    @Override
    public boolean accepts(Object o) {
      return true;
    }

  }

  @Override
  public void flush() {
    this.out.flush();
  }
}
