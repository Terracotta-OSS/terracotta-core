/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression.regexp;


import com.tc.aspectwerkz.expression.SubtypePatternType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Implements an abstract regular expression pattern matcher for AspectWerkz.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public abstract class Pattern implements Serializable {
  /**
   * Defines a single wildcard.
   */
  public static final String REGULAR_WILDCARD = "*";

  /**
   * Defines a multiple wildcard.
   */
  public static final String EAGER_WILDCARD = "..";

  /**
   * Abbreviations for all the classes in the java.lang.* and the java.util.* namespaces.
   */
  public static final Map ABBREVIATIONS = new HashMap();

  static {
    // TODO: update for Java 1.5?
    // abbreviations used in XML def advice name
    ABBREVIATIONS.put("JoinPoint", "com.tc.aspectwerkz.joinpoint.JoinPoint");
    ABBREVIATIONS.put("StaticJoinPoint", "com.tc.aspectwerkz.joinpoint.StaticJoinPoint");
    ABBREVIATIONS.put("Rtti", "com.tc.aspectwerkz.joinpoint.Rtti");
    // java.lang.*
    ABBREVIATIONS.put("CharSequence", "java.lang.CharSequence");
    ABBREVIATIONS.put("Cloneable", "java.lang.Cloneable");
    ABBREVIATIONS.put("Comparable", "java.lang.Comparable");
    ABBREVIATIONS.put("Runnable", "java.lang.Runnable");
    ABBREVIATIONS.put("Boolean", "java.lang.Boolean");
    ABBREVIATIONS.put("Byte", "java.lang.Byte");
    ABBREVIATIONS.put("Character", "java.lang.Character");
    ABBREVIATIONS.put("Class", "java.lang.Class");
    ABBREVIATIONS.put("ClassLoader", "java.lang.ClassLoader");
    ABBREVIATIONS.put("Compiler", "java.lang.Compiler");
    ABBREVIATIONS.put("Double", "java.lang.Double");
    ABBREVIATIONS.put("Float", "java.lang.Float");
    ABBREVIATIONS.put("InheritableThreadLocal", "java.lang.InheritableThreadLocal");
    ABBREVIATIONS.put("Integer", "java.lang.Integer");
    ABBREVIATIONS.put("Long", "java.lang.Long");
    ABBREVIATIONS.put("Math", "java.lang.Math");
    ABBREVIATIONS.put("Number", "java.lang.Number");
    ABBREVIATIONS.put("Object", "java.lang.Object");
    ABBREVIATIONS.put("Package", "java.lang.Package");
    ABBREVIATIONS.put("Process", "java.lang.Process");
    ABBREVIATIONS.put("Runtime", "java.lang.Runtime");
    ABBREVIATIONS.put("RuntimePermission", "java.lang.RuntimePermission");
    ABBREVIATIONS.put("SecurityManager", "java.lang.SecurityManager");
    ABBREVIATIONS.put("Short", "java.lang.Short");
    ABBREVIATIONS.put("StackTraceElement", "java.lang.StackTraceElement");
    ABBREVIATIONS.put("StrictMath", "java.lang.StrictMath");
    ABBREVIATIONS.put("String", "java.lang.String");
    ABBREVIATIONS.put("StringBuffer", "java.lang.StringBuffer");
    ABBREVIATIONS.put("System", "java.lang.System");
    ABBREVIATIONS.put("Thread", "java.lang.Thread");
    ABBREVIATIONS.put("ThreadGroup", "java.lang.ThreadGroup");
    ABBREVIATIONS.put("ThreadLocal", "java.lang.ThreadLocal");
    ABBREVIATIONS.put("Throwable", "java.lang.Throwable");
    ABBREVIATIONS.put("Exception", "java.lang.Exception");
    ABBREVIATIONS.put("Void", "java.lang.Void");
    ABBREVIATIONS.put("CharSequence[]", "java.lang.CharSequence[][]");
    ABBREVIATIONS.put("Cloneable[]", "java.lang.Cloneable[]");
    ABBREVIATIONS.put("Comparable[]", "java.lang.Comparable[]");
    ABBREVIATIONS.put("Runnable[]", "java.lang.Runnable[]");
    ABBREVIATIONS.put("Boolean[]", "java.lang.Boolean[]");
    ABBREVIATIONS.put("Byte[]", "java.lang.Byte[]");
    ABBREVIATIONS.put("Character[]", "java.lang.Character[]");
    ABBREVIATIONS.put("Class[]", "java.lang.Class[]");
    ABBREVIATIONS.put("ClassLoader[]", "java.lang.ClassLoader[]");
    ABBREVIATIONS.put("Compiler[]", "java.lang.Compiler[]");
    ABBREVIATIONS.put("Double[]", "java.lang.Double[]");
    ABBREVIATIONS.put("Float[]", "java.lang.Float[]");
    ABBREVIATIONS.put("InheritableThreadLocal[]", "java.lang.InheritableThreadLocal[]");
    ABBREVIATIONS.put("Integer[]", "java.lang.Integer[]");
    ABBREVIATIONS.put("Long[]", "java.lang.Long[]");
    ABBREVIATIONS.put("Math[]", "java.lang.Math[]");
    ABBREVIATIONS.put("Number[]", "java.lang.Number[]");
    ABBREVIATIONS.put("Object[]", "java.lang.Object[]");
    ABBREVIATIONS.put("Package[]", "java.lang.Package[]");
    ABBREVIATIONS.put("Process[]", "java.lang.Process[]");
    ABBREVIATIONS.put("Runtime[]", "java.lang.Runtime[]");
    ABBREVIATIONS.put("RuntimePermission[]", "java.lang.RuntimePermission[]");
    ABBREVIATIONS.put("SecurityManager[]", "java.lang.SecurityManager[]");
    ABBREVIATIONS.put("Short[]", "java.lang.Short[]");
    ABBREVIATIONS.put("StackTraceElement[]", "java.lang.StackTraceElement[]");
    ABBREVIATIONS.put("StrictMath[]", "java.lang.StrictMath[]");
    ABBREVIATIONS.put("String[]", "java.lang.String[]");
    ABBREVIATIONS.put("StringBuffer[]", "java.lang.StringBuffer[]");
    ABBREVIATIONS.put("System[]", "java.lang.System[]");
    ABBREVIATIONS.put("Thread[]", "java.lang.Thread[]");
    ABBREVIATIONS.put("ThreadGroup[]", "java.lang.ThreadGroup[]");
    ABBREVIATIONS.put("ThreadLocal[]", "java.lang.ThreadLocal[]");
    ABBREVIATIONS.put("Throwable[]", "java.lang.Throwable[]");
    ABBREVIATIONS.put("Exception[]", "java.lang.Exception[]");
    ABBREVIATIONS.put("Void[]", "java.lang.Void[]");
    ABBREVIATIONS.put("CharSequence[][]", "java.lang.CharSequence[][]");
    ABBREVIATIONS.put("Cloneable[][]", "java.lang.Cloneable[][]");
    ABBREVIATIONS.put("Comparable[][]", "java.lang.Comparable[][]");
    ABBREVIATIONS.put("Runnable[][]", "java.lang.Runnable[][]");
    ABBREVIATIONS.put("Boolean[][]", "java.lang.Boolean[][]");
    ABBREVIATIONS.put("Byte[][]", "java.lang.Byte[][]");
    ABBREVIATIONS.put("Character[][]", "java.lang.Character[][]");
    ABBREVIATIONS.put("Class[][]", "java.lang.Class[][]");
    ABBREVIATIONS.put("ClassLoader[][]", "java.lang.ClassLoader[][]");
    ABBREVIATIONS.put("Compiler[][]", "java.lang.Compiler[][]");
    ABBREVIATIONS.put("Double[][]", "java.lang.Double[][]");
    ABBREVIATIONS.put("Float[][]", "java.lang.Float[][]");
    ABBREVIATIONS.put("InheritableThreadLocal[][]", "java.lang.InheritableThreadLocal[][]");
    ABBREVIATIONS.put("Integer[][]", "java.lang.Integer[][]");
    ABBREVIATIONS.put("Long[][]", "java.lang.Long[][]");
    ABBREVIATIONS.put("Math[][]", "java.lang.Math[][]");
    ABBREVIATIONS.put("Number[][]", "java.lang.Number[][]");
    ABBREVIATIONS.put("Object[][]", "java.lang.Object[][]");
    ABBREVIATIONS.put("Package[][]", "java.lang.Package[][]");
    ABBREVIATIONS.put("Process[][]", "java.lang.Process[][]");
    ABBREVIATIONS.put("Runtime[][]", "java.lang.Runtime[][]");
    ABBREVIATIONS.put("RuntimePermission[][]", "java.lang.RuntimePermission[][]");
    ABBREVIATIONS.put("SecurityManager[][]", "java.lang.SecurityManager[][]");
    ABBREVIATIONS.put("Short[][]", "java.lang.Short[][]");
    ABBREVIATIONS.put("StackTraceElement[][]", "java.lang.StackTraceElement[][]");
    ABBREVIATIONS.put("StrictMath[][]", "java.lang.StrictMath[][]");
    ABBREVIATIONS.put("String[][]", "java.lang.String[][]");
    ABBREVIATIONS.put("StringBuffer[][]", "java.lang.StringBuffer[][]");
    ABBREVIATIONS.put("System[][]", "java.lang.System[][]");
    ABBREVIATIONS.put("Thread[][]", "java.lang.Thread[][]");
    ABBREVIATIONS.put("ThreadGroup[][]", "java.lang.ThreadGroup[][]");
    ABBREVIATIONS.put("ThreadLocal[][]", "java.lang.ThreadLocal[][]");
    ABBREVIATIONS.put("Throwable[][]", "java.lang.Throwable[][]");
    ABBREVIATIONS.put("Exception[][]", "java.lang.Exception[][]");
    ABBREVIATIONS.put("Void[][]", "java.lang.Void[][]");
    ABBREVIATIONS.put("Collection", "java.util.Collection");
    ABBREVIATIONS.put("Comparator", "java.util.Comparator");
    ABBREVIATIONS.put("Enumeration", "java.util.Enumeration");
    ABBREVIATIONS.put("EventListener", "java.util.EventListener");
    ABBREVIATIONS.put("Iterator", "java.util.Iterator");
    ABBREVIATIONS.put("List", "java.util.List");
    ABBREVIATIONS.put("ListIterator", "java.util.ListIterator");
    ABBREVIATIONS.put("Map", "java.util.Map");
    ABBREVIATIONS.put("Map.Entry", "java.util.Map.Entry");
    ABBREVIATIONS.put("Observer", "java.util.Observer");
    ABBREVIATIONS.put("RandomAccess", "java.util.RandomAccess");
    ABBREVIATIONS.put("Set", "java.util.Set");
    ABBREVIATIONS.put("SortedMap", "java.util.SortedMap");
    ABBREVIATIONS.put("SortedSet", "java.util.SortedSet");
    ABBREVIATIONS.put("AbstractCollection", "java.util.AbstractCollection");
    ABBREVIATIONS.put("AbstractList", "java.util.AbstractList");
    ABBREVIATIONS.put("AbstractMap", "java.util.AbstractMap");
    ABBREVIATIONS.put("AbstractSequentialList ", "java.util.AbstractSequentialList");
    ABBREVIATIONS.put("AbstractSet", "java.util.AbstractSet");
    ABBREVIATIONS.put("ArrayList", "java.util.ArrayList");
    ABBREVIATIONS.put("Arrays", "java.util.Arrays");
    ABBREVIATIONS.put("BitSet", "java.util.BitSet");
    ABBREVIATIONS.put("Calendar", "java.util.Calendar");
    ABBREVIATIONS.put("Collections", "java.util.Collections");
    ABBREVIATIONS.put("Currency", "java.util.Currency");
    ABBREVIATIONS.put("Date", "java.util.Date");
    ABBREVIATIONS.put("Dictionary", "java.util.Dictionary");
    ABBREVIATIONS.put("EventListenerProxy", "java.util.EventListenerProxy");
    ABBREVIATIONS.put("EventObject", "java.util.EventObject");
    ABBREVIATIONS.put("GregorianCalender", "java.util.GregorianCalender");
    ABBREVIATIONS.put("HashMap", "java.util.HashMap");
    ABBREVIATIONS.put("HashSet", "java.util.HashSet");
    ABBREVIATIONS.put("Hashtable", "java.util.Hashtable");
    ABBREVIATIONS.put("IdentityHashMap", "java.util.IdentityHashMap");
    ABBREVIATIONS.put("LinkedHashMap", "java.util.LinkedHashMap");
    ABBREVIATIONS.put("LinkedHashSet", "java.util.LinkedHashSet");
    ABBREVIATIONS.put("LinkedList", "java.util.LinkedList");
    ABBREVIATIONS.put("ListResourceBundle", "java.util.ListResourceBundle");
    ABBREVIATIONS.put("Locale", "java.util.Locale");
    ABBREVIATIONS.put("Observable", "java.util.Observable");
    ABBREVIATIONS.put("Properties", "java.util.Properties");
    ABBREVIATIONS.put("PropertyPermission", "java.util.PropertyPermission");
    ABBREVIATIONS.put("PropertyResourceBundle", "java.util.PropertyResourceBundle");
    ABBREVIATIONS.put("Random", "java.util.Random");
    ABBREVIATIONS.put("ResourceBundle", "java.util.ResourceBundle");
    ABBREVIATIONS.put("SimpleTimeZone", "java.util.SimpleTimeZone");
    ABBREVIATIONS.put("Stack", "java.util.Stack");
    ABBREVIATIONS.put("StringTokenizer", "java.util.StringTokenizer");
    ABBREVIATIONS.put("Timer", "java.util.Timer");
    ABBREVIATIONS.put("TimerTask", "java.util.TimerTask");
    ABBREVIATIONS.put("TimeZone", "java.util.TimeZone");
    ABBREVIATIONS.put("TreeMap", "java.util.TreeMap");
    ABBREVIATIONS.put("TreeSet", "java.util.TreeSet");
    ABBREVIATIONS.put("Vector", "java.util.Vector");
    ABBREVIATIONS.put("WeakHashMap", "java.util.WeakHashMap");
    ABBREVIATIONS.put("Collection[]", "java.util.Collection[]");
    ABBREVIATIONS.put("Comparator[]", "java.util.Comparator[]");
    ABBREVIATIONS.put("Enumeration[]", "java.util.Enumeration[]");
    ABBREVIATIONS.put("EventListener[]", "java.util.EventListener[]");
    ABBREVIATIONS.put("Iterator[]", "java.util.Iterator[]");
    ABBREVIATIONS.put("List[]", "java.util.List[]");
    ABBREVIATIONS.put("ListIterator[]", "java.util.ListIterator[]");
    ABBREVIATIONS.put("Map[]", "java.util.Map[]");
    ABBREVIATIONS.put("Map.Entry[]", "java.util.Map.Entry[]");
    ABBREVIATIONS.put("Observer[]", "java.util.Observer[]");
    ABBREVIATIONS.put("RandomAccess[]", "java.util.RandomAccess[]");
    ABBREVIATIONS.put("Set[]", "java.util.Set[]");
    ABBREVIATIONS.put("SortedMap[]", "java.util.SortedMap[]");
    ABBREVIATIONS.put("SortedSet[]", "java.util.SortedSet[]");
    ABBREVIATIONS.put("AbstractCollection[]", "java.util.AbstractCollection[]");
    ABBREVIATIONS.put("AbstractList[]", "java.util.AbstractList[]");
    ABBREVIATIONS.put("AbstractMap[]", "java.util.AbstractMap[]");
    ABBREVIATIONS.put("AbstractSequentialList []", "java.util.AbstractSequentialList[]");
    ABBREVIATIONS.put("AbstractSet[]", "java.util.AbstractSet[]");
    ABBREVIATIONS.put("ArrayList[]", "java.util.ArrayList[]");
    ABBREVIATIONS.put("Arrays[]", "java.util.Arrays[]");
    ABBREVIATIONS.put("BitSet[]", "java.util.BitSet[]");
    ABBREVIATIONS.put("Calendar[]", "java.util.Calendar[]");
    ABBREVIATIONS.put("Collections[]", "java.util.Collections[]");
    ABBREVIATIONS.put("Currency[]", "java.util.Currency[]");
    ABBREVIATIONS.put("Date[]", "java.util.Date[]");
    ABBREVIATIONS.put("Dictionary[]", "java.util.Dictionary[]");
    ABBREVIATIONS.put("EventListenerProxy[]", "java.util.EventListenerProxy[]");
    ABBREVIATIONS.put("EventObject[]", "java.util.EventObject[]");
    ABBREVIATIONS.put("GregorianCalender[]", "java.util.GregorianCalender[]");
    ABBREVIATIONS.put("HashMap[]", "java.util.HashMap[]");
    ABBREVIATIONS.put("HashSet[]", "java.util.HashSet[]");
    ABBREVIATIONS.put("Hashtable[]", "java.util.Hashtable[]");
    ABBREVIATIONS.put("IdentityHashMap[]", "java.util.IdentityHashMap[]");
    ABBREVIATIONS.put("LinkedHashMap[]", "java.util.LinkedHashMap[]");
    ABBREVIATIONS.put("LinkedHashSet[]", "java.util.LinkedHashSet[]");
    ABBREVIATIONS.put("LinkedList[]", "java.util.LinkedList[]");
    ABBREVIATIONS.put("ListResourceBundle[]", "java.util.ListResourceBundle[]");
    ABBREVIATIONS.put("Locale[]", "java.util.Locale[]");
    ABBREVIATIONS.put("Observable[]", "java.util.Observable[]");
    ABBREVIATIONS.put("Properties[]", "java.util.Properties[]");
    ABBREVIATIONS.put("PropertyPermission[]", "java.util.PropertyPermission[]");
    ABBREVIATIONS.put("PropertyResourceBundle[]", "java.util.PropertyResourceBundle[]");
    ABBREVIATIONS.put("Random[]", "java.util.Random[]");
    ABBREVIATIONS.put("ResourceBundle[]", "java.util.ResourceBundle[]");
    ABBREVIATIONS.put("SimpleTimeZone[]", "java.util.SimpleTimeZone[]");
    ABBREVIATIONS.put("Stack[]", "java.util.Stack[]");
    ABBREVIATIONS.put("StringTokenizer[]", "java.util.StringTokenizer[]");
    ABBREVIATIONS.put("Timer[]", "java.util.Timer[]");
    ABBREVIATIONS.put("TimerTask[]", "java.util.TimerTask[]");
    ABBREVIATIONS.put("TimeZone[]", "java.util.TimeZone[]");
    ABBREVIATIONS.put("TreeMap[]", "java.util.TreeMap[]");
    ABBREVIATIONS.put("TreeSet[]", "java.util.TreeSet[]");
    ABBREVIATIONS.put("Vector[]", "java.util.Vector[]");
    ABBREVIATIONS.put("WeakHashMap[]", "java.util.WeakHashMap[]");
    ABBREVIATIONS.put("Collection[][]", "java.util.Collection[][]");
    ABBREVIATIONS.put("Comparator[][]", "java.util.Comparator[][]");
    ABBREVIATIONS.put("Enumeration[][]", "java.util.Enumeration[][]");
    ABBREVIATIONS.put("EventListener[][]", "java.util.EventListener[][]");
    ABBREVIATIONS.put("Iterator[][]", "java.util.Iterator[][]");
    ABBREVIATIONS.put("List[][]", "java.util.List[][]");
    ABBREVIATIONS.put("ListIterator[][]", "java.util.ListIterator[][]");
    ABBREVIATIONS.put("Map[][]", "java.util.Map[][]");
    ABBREVIATIONS.put("Map.Entry[][]", "java.util.Map.Entry[][]");
    ABBREVIATIONS.put("Observer[][]", "java.util.Observer[][]");
    ABBREVIATIONS.put("RandomAccess[][]", "java.util.RandomAccess[][]");
    ABBREVIATIONS.put("Set[][]", "java.util.Set[][]");
    ABBREVIATIONS.put("SortedMap[][]", "java.util.SortedMap[][]");
    ABBREVIATIONS.put("SortedSet[][]", "java.util.SortedSet[][]");
    ABBREVIATIONS.put("AbstractCollection[][]", "java.util.AbstractCollection[][]");
    ABBREVIATIONS.put("AbstractList[][]", "java.util.AbstractList[][]");
    ABBREVIATIONS.put("AbstractMap[][]", "java.util.AbstractMap[][]");
    ABBREVIATIONS.put("AbstractSequentialList [][]", "java.util.AbstractSequentialList[][]");
    ABBREVIATIONS.put("AbstractSet[][]", "java.util.AbstractSet[][]");
    ABBREVIATIONS.put("ArrayList[][]", "java.util.ArrayList[][]");
    ABBREVIATIONS.put("Arrays[][]", "java.util.Arrays[][]");
    ABBREVIATIONS.put("BitSet[][]", "java.util.BitSet[][]");
    ABBREVIATIONS.put("Calendar[][]", "java.util.Calendar[][]");
    ABBREVIATIONS.put("Collections[][]", "java.util.Collections[][]");
    ABBREVIATIONS.put("Currency[][]", "java.util.Currency[][]");
    ABBREVIATIONS.put("Date[][]", "java.util.Date[][]");
    ABBREVIATIONS.put("Dictionary[][]", "java.util.Dictionary[][]");
    ABBREVIATIONS.put("EventListenerProxy[][]", "java.util.EventListenerProxy[][]");
    ABBREVIATIONS.put("EventObject[][]", "java.util.EventObject[][]");
    ABBREVIATIONS.put("GregorianCalender[][]", "java.util.GregorianCalender[][]");
    ABBREVIATIONS.put("HashMap[][]", "java.util.HashMap[][]");
    ABBREVIATIONS.put("HashSet[][]", "java.util.HashSet[][]");
    ABBREVIATIONS.put("Hashtable[][]", "java.util.Hashtable[][]");
    ABBREVIATIONS.put("IdentityHashMap[][]", "java.util.IdentityHashMap[][]");
    ABBREVIATIONS.put("LinkedHashMap[][]", "java.util.LinkedHashMap[][]");
    ABBREVIATIONS.put("LinkedHashSet[][]", "java.util.LinkedHashSet[][]");
    ABBREVIATIONS.put("LinkedList[][]", "java.util.LinkedList[][]");
    ABBREVIATIONS.put("ListResourceBundle[][]", "java.util.ListResourceBundle[][]");
    ABBREVIATIONS.put("Locale[][]", "java.util.Locale[][]");
    ABBREVIATIONS.put("Observable[][]", "java.util.Observable[][]");
    ABBREVIATIONS.put("Properties[][]", "java.util.Properties[][]");
    ABBREVIATIONS.put("PropertyPermission[][]", "java.util.PropertyPermission[][]");
    ABBREVIATIONS.put("PropertyResourceBundle[][]", "java.util.PropertyResourceBundle[][]");
    ABBREVIATIONS.put("Random[][]", "java.util.Random[][]");
    ABBREVIATIONS.put("ResourceBundle[][]", "java.util.ResourceBundle[][]");
    ABBREVIATIONS.put("SimpleTimeZone[][]", "java.util.SimpleTimeZone[][]");
    ABBREVIATIONS.put("Stack[][]", "java.util.Stack[][]");
    ABBREVIATIONS.put("StringTokenizer[][]", "java.util.StringTokenizer[][]");
    ABBREVIATIONS.put("Timer[][]", "java.util.Timer[][]");
    ABBREVIATIONS.put("TimerTask[][]", "java.util.TimerTask[][]");
    ABBREVIATIONS.put("TimeZone[][]", "java.util.TimeZone[][]");
    ABBREVIATIONS.put("TreeMap[][]", "java.util.TreeMap[][]");
    ABBREVIATIONS.put("TreeSet[][]", "java.util.TreeSet[][]");
    ABBREVIATIONS.put("Vector[][]", "java.util.Vector[][]");
    ABBREVIATIONS.put("WeakHashMap[][]", "java.util.WeakHashMap[][]");
  }

  /**
   * Compiles and returns a new type pattern.
   *
   * @param pattern            the full pattern as a string
   * @param subtypePatternType the subtype pattern type
   * @return the pattern
   */
  public static TypePattern compileTypePattern(final String pattern, final SubtypePatternType subtypePatternType) {
    return new TypePattern(pattern, subtypePatternType);
  }

  /**
   * Compiles and returns a new name pattern.
   *
   * @param pattern the full pattern as a string
   * @return the pattern
   */
  public static NamePattern compileNamePattern(final String pattern) {
    return new NamePattern(pattern);
  }
}