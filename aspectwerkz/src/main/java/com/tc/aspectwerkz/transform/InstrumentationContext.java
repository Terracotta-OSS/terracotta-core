/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform;

import com.tc.asm.Label;

import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.proxy.ProxyDelegationStrategy;
import com.tc.aspectwerkz.proxy.ProxySubclassingStrategy;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;


/**
 * Implementation of the transformation context interface for the delegation weaving.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class InstrumentationContext {
  /**
   * The name of the class.
   */
  private final String m_className;

  /**
   * The initial bytecode of the class
   */
  private final byte[] m_initialBytecode;

  /**
   * The current bytecode of the class
   */
  private byte[] m_currentBytecode;

  /**
   * The class loader for the class being transformed.
   */
  private final ClassLoader m_loader;

  /**
   * Marks the class being transformed as advised.
   */
  private boolean m_advised = false;

  /**
   * Marks the class being transformed as made advisable for interceptor support.
   */
  private boolean m_madeAdvisable = false;

  /**
   * Is the class being transformed a proxy or not?
   */
  private boolean m_isProxy = false;

  /**
   * Marks the context as read-only.
   */
  private boolean m_readOnly = false;

  /**
   * Meta-data for the transformation.
   */
  private Map m_metaData = new HashMap();

  /**
   * The contextual set of SystemDefinitions
   */
  private final Set m_definitions;

  /**
   * The emitted join points.
   */
  private final List m_emittedJoinPoints = new ArrayList();

  /**
   * A map of line number per label.
   * Note: labels are valid in the scope of one single ASM accept() only (one phase)
   */
  private final HashMap m_labelTolineNumbers = new HashMap();

  private long m_serialVerUid;

  /**
   * Creates a new context.
   */
  public InstrumentationContext(final String className, final byte[] bytecode, final ClassLoader loader, final Set definitions) {
    m_className = className.replace('.', '/');
    m_loader = loader;
    m_initialBytecode = bytecode;
    m_currentBytecode = bytecode;
    m_definitions = definitions;
    if (isAWProxy(className) || isCGLIBProxy(className) || isDynamicProxy(className)) {
      markAsProxy();
    }
  }

  /**
   * Creates a new context.
   */
  public InstrumentationContext(final String className, final byte[] bytecode, final ClassLoader loader) {
    this(className, bytecode, loader, SystemDefinitionContainer.getDefinitionsFor(loader));
  }

  public String getClassName() {
    return m_className;
  }

  /**
   * Returns the initial bytecode.
   *
   * @return bytecode
   */
  public byte[] getInitialBytecode() {
    return m_initialBytecode;
  }

  /**
   * Returns the current bytecode.
   *
   * @return bytecode
   */
  public byte[] getCurrentBytecode() {
    return m_currentBytecode;
  }

  /**
   * Sets the current bytecode.
   *
   * @param bytecode
   */
  public void setCurrentBytecode(final byte[] bytecode) {
    m_currentBytecode = bytecode;
  }

  /**
   * Returns the class loader.
   *
   * @return the class loader
   */
  public ClassLoader getLoader() {
    return m_loader;
  }

  /**
   * The definitions context (with hierarchical structure)
   *
   * @return
   */
  public Set getDefinitions() {
    return m_definitions;
  }

  /**
   * Marks the class being transformed as advised. The marker can at most be set once per class per transformer
   */
  public void markAsAdvised() {
    m_advised = true;
  }

  /**
   * Marks the class as made advisable.
   */
  public void markMadeAdvisable() {
    m_madeAdvisable = true;
  }

  /**
   * Resets the isAdviced flag.
   */
  public void resetAdvised() {
    m_advised = false;
  }

  /**
   * Is the class being transformed a proxy or not?
   */
  public boolean isProxy() {
    return m_isProxy;
  }

  /**
   * Marks the class being transformed as a proxy.
   */
  public void markAsProxy() {
    m_isProxy = true;
  }

  /**
   * Checks if the class being transformed has beed advised.
   *
   * @return boolean
   */
  public boolean isAdvised() {
    return m_advised;
  }

  /**
   * Checks if the class has been made advisable.
   *
   * @return
   */
  public boolean isMadeAdvisable() {
    return m_madeAdvisable;
  }

  /**
   * Marks the context as read-only.
   */
  public void markAsReadOnly() {
    m_readOnly = true;
  }

  /**
   * Checks if the context is read-only.
   *
   * @return boolean
   */
  public boolean isReadOnly() {
    return m_readOnly;
  }

  /**
   * Returns meta-data for the transformation.
   *
   * @param key the key
   * @return the value
   */
  public Object getMetaData(final Object key) {
    return m_metaData.get(key);
  }

  /**
   * Adds new meta-data for the transformation.
   *
   * @param key   the key
   * @param value the value
   */
  public void addMetaData(final Object key, final Object value) {
    if (m_readOnly) {
      throw new IllegalStateException("context is read only");
    }
    m_metaData.put(key, value);
  }

  /**
   * Dumps the class to specific directory.
   *
   * @param dumpDir
   */
  public void dump(final String dumpDir) {
    try {
      int lastSegmentIndex = m_className.lastIndexOf('/');
      if (lastSegmentIndex < 0) {
        lastSegmentIndex = 0;
      }
      File dir = new File(dumpDir + File.separator + m_className.substring(0, lastSegmentIndex));
      dir.mkdirs();
      FileOutputStream os = new FileOutputStream(
              dumpDir
                      + File.separator
                      + m_className.replace('.', '/')
                      + ".class"
      );
      os.write(m_currentBytecode);
      os.close();
    } catch (Exception e) {
      System.err.println("failed to dump " + m_className);
      e.printStackTrace();
    }
  }

  /**
   * Adds a new EmittedJoinPoint
   *
   * @param jp
   */
  public void addEmittedJoinPoint(final EmittedJoinPoint jp) {
    m_emittedJoinPoints.add(jp);
  }

  /**
   * Returns all the EmittedJoinPoints
   *
   * @return
   */
  public List getEmittedJoinPoints() {
    return m_emittedJoinPoints;
  }

  public void setSerialVerUid(long initialSerialVerUid) {
    m_serialVerUid = initialSerialVerUid;
  }

  public long getSerialVerUid() {
    return m_serialVerUid;
  }

  public void addLineNumberInfo(Label label, int lineNumber) {
    m_labelTolineNumbers.put(label, Integer.valueOf(lineNumber));
  }

  /**
   * Tries to resolve the line number from the given label
   *
   * @param label
   * @return
   */
  public int resolveLineNumberInfo(Label label) {
    Integer info = (Integer) m_labelTolineNumbers.get(label);
    return info==null ? 0 : info.intValue();
  }

  public static boolean isAWProxy(final String className) {
    return className.indexOf(ProxySubclassingStrategy.PROXY_SUFFIX) != -1
            || className.indexOf(ProxyDelegationStrategy.PROXY_SUFFIX) != -1;
  }

  public static boolean isCGLIBProxy(final String className) {
    return className.indexOf("$$EnhancerByCGLIB$$") != -1
            || className.indexOf("$$FastClassByCGLIB$$") != -1;
  }

  private boolean isDynamicProxy(final String className) {
    return className.startsWith("$Proxy");
  }
}
