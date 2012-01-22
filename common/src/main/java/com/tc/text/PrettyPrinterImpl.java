/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.text;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class PrettyPrinterImpl implements PrettyPrinter {

  private static final String   INDENT        = "--> ";

  private final StringBuffer    prefix;
  private final PrintWriter     out;
  private final IdentityHashMap visited;

  private final PrintPolicy     defaultPolicy = new BasicPrintPolicy();
  private final Collection      policies;

  private boolean               autoflush     = true;

  public PrettyPrinterImpl(PrintWriter out) {
    this(INDENT, out, new IdentityHashMap());
  }

  private PrettyPrinterImpl(String prefix, PrintWriter out, IdentityHashMap visited) {
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
    for (Iterator i = policies.iterator(); i.hasNext();) {
      PrintPolicy policy = (PrintPolicy) i.next();
      if (policy.accepts(o)) { return policy; }
    }
    return defaultPolicy;
  }

  /**
   * Creates a policy path. Each policy is searched in order.
   */
  private Collection initPolicies() {
    Collection rv = new ArrayList();
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

    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return ((PrettyPrintable) o).prettyPrint(pp);
    }

    public boolean accepts(Object o) {
      return o != null && o instanceof PrettyPrintable;
    }

  }

  private static class ShallowMapPrintPolicy implements PrintPolicy {

    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return pp.print(o.getClass().getName()).print(".size()=").print(((Map) o).size() + "");
    }

    public boolean accepts(Object o) {
      return o != null && o instanceof Map;
    }

  }

  private static class ShallowCollectionPrintPolicy implements PrintPolicy {

    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return pp.print(o.getClass().getName()).print(".size()=").print(((Collection) o).size() + "");
    }

    public boolean accepts(Object o) {
      return o != null && o instanceof Collection;
    }

  }

  private static class BasicPrintPolicy implements PrintPolicy {

    public PrettyPrinter visit(PrettyPrinter pp, Object o) {
      return pp.print(o);
    }

    public boolean accepts(Object o) {
      return true;
    }

  }

  public void flush() {
    this.out.flush();
  }
}
