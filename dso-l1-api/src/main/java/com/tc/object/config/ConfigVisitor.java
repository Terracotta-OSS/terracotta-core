/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Allows a class to visit a L1DSOConfig to augment/adjust it
 */
public class ConfigVisitor {
  private final Map           visited                                        = new HashMap();
  private final TCLogger      logger                                         = TCLogging.getLogger(getClass());

  public static final String  VISIT_METHOD_NAME                              = "visitL1DSOConfig";
  public static final String  VISIT_INSTANCE_METHOD_NAME                     = "instanceVisitL1DSOConfig";
  public static final Class[] VISIT_METHOD_PARAMETERS                        = new Class[] { ConfigVisitor.class,
      DSOClientConfigHelper.class                                           };
  public static final Class[] VISIT_METHOD_PARAMETERS_WITH_ATTRIBUTES        = new Class[] { ConfigVisitor.class,
      DSOClientConfigHelper.class, Map.class                                };
  public static final String  VISIT_DSO_APPLICATION_CONFIG_METHOD_NAME       = "visitDSOApplicationConfig";
  public static final Class[] VISIT_DSO_APPLICATION_CONFIG_METHOD_PARAMETERS = new Class[] { ConfigVisitor.class,
      DSOApplicationConfig.class                                            };

  public void visitDSOApplicationConfig(DSOApplicationConfig config, Class clazz) {
    if (checkAndSetVisited(config, clazz)) { return; }
    doVisit(clazz, VISIT_DSO_APPLICATION_CONFIG_METHOD_NAME, VISIT_DSO_APPLICATION_CONFIG_METHOD_PARAMETERS,
            new Object[] { this, config });
  }

  public void visit(DSOClientConfigHelper config, Visitable v) {
    if (checkAndSetVisited(config, v)) { return; }
    v.visit(this, config);
  }

  public void visit(DSOClientConfigHelper config, Class clazz) {
    if (checkAndSetVisited(config, clazz)) { return; }
    Object[] args = new Object[] { this, config };
    // use instance visiting if available, otherwise do class visiting
    if (!doInstanceVisit(clazz, VISIT_INSTANCE_METHOD_NAME, VISIT_METHOD_PARAMETERS, args)) doVisit(
                                                                                                    clazz,
                                                                                                    VISIT_METHOD_NAME,
                                                                                                    VISIT_METHOD_PARAMETERS,
                                                                                                    new Object[] {
                                                                                                        this, config });
  }

  public void visit(DSOClientConfigHelper config, Class clazz, Map optionalAttributes) {
    if (checkAndSetVisited(config, clazz)) { return; }
    Object[] args = new Object[] { this, config };
    // use instance visiting if available, otherwise do class visiting
    if (!doInstanceVisit(clazz, VISIT_INSTANCE_METHOD_NAME, VISIT_METHOD_PARAMETERS, args)) doVisit(
                                                                                                    clazz,
                                                                                                    VISIT_METHOD_NAME,
                                                                                                    VISIT_METHOD_PARAMETERS_WITH_ATTRIBUTES,
                                                                                                    new Object[] {
                                                                                                        this, config,
                                                                                                        optionalAttributes });
  }

  // instance visiting only works if clazz defines a niladic constructor
  private boolean doInstanceVisit(Class clazz, String methodName, Class[] parameters, Object[] arguments) {
    boolean result = false;
    try {
      Method visitMethod = clazz.getMethod(VISIT_INSTANCE_METHOD_NAME, parameters);
      System.out.println("instance configuration method found");
      Constructor construct = clazz.getConstructor(new Class[0]);
      Object instance = construct.newInstance(new Object[0]);
      visitMethod.setAccessible(true);
      visitMethod.invoke(instance, arguments);
      result = true;
    } catch (NoSuchMethodException e) {
      // nothing to do
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void doVisit(Class clazz, String methodName, Class[] parameters, Object[] arguments) {
    while (clazz != null) {
      try {
        // look for instance method first
        Method visitMethod = clazz.getMethod(methodName, parameters);
        if (Modifier.isStatic(visitMethod.getModifiers())) {
          visitMethod.setAccessible(true);
          logger.info("Visiting: " + clazz.getName());
          visitMethod.invoke(null, arguments);
        }
      } catch (NoSuchMethodException e) {
        if (!Object.class.getName().equals(clazz.getName())) {
          StringBuffer paramString = new StringBuffer();
          for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
              paramString.append(",");
            }
            paramString.append(parameters[i].getName());
          }
          logger.info("Visit method not defined: " + clazz.getName() + "." + methodName + "(" + paramString + ")");
        }
        continue;
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        clazz = clazz.getSuperclass();
      }
    }
  }

  private boolean checkAndSetVisited(Object config, Class clazz) {
    return checkAndSetVisited(config, clazz.getName());
  }

  private boolean checkAndSetVisited(Object config, Visitable v) {
    return checkAndSetVisited(config, v.getClass());
  }

  private boolean checkAndSetVisited(Object config, Object key) {
    Set set;
    synchronized (visited) {
      set = getOrCreateVisitedFor(key);
    }
    synchronized (set) {
      boolean rv = !set.add(config);
      if (rv) {
        logger.warn("Already visited: " + key);
      }
      return rv;
    }
  }

  private Set getOrCreateVisitedFor(Object key) {
    synchronized (visited) {
      Set set = (Set) visited.get(key);
      if (set == null) {
        set = new HashSet();
        visited.put(key, set);
      }
      return set;
    }
  }

}
