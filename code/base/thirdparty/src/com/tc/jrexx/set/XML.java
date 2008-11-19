/*
 * 01/07/2003 - 15:19:32
 * 
 * Pattern.java - Copyright (C) 2003 Buero fuer Softwarearchitektur GbR ralf.meyer@karneim.com http://jrexx.sf.net
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this program; if not, write to
 * the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.tc.jrexx.set;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * @deprecated
 */
public abstract class XML implements java.io.Serializable {
  public String toXML(String tag) {
    final StringBuffer buffer = new StringBuffer();
    buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    buffer.append("\n<").append(tag).append('>');
    final StringTokenizer strTkzr = new StringTokenizer(this.toInnerXML(), "\n");
    for (int i = strTkzr.countTokens(); i > 0; --i) {
      buffer.append("\n  ").append(strTkzr.nextToken());
    }
    buffer.append("\n</").append(tag).append('>');
    return buffer.toString();
  }

  public String toXML() {
    final StringBuffer buffer = new StringBuffer();
    buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    buffer.append("\n").append(this.toInnerXML());
    return buffer.toString();
  }

  protected String toInnerXML() {
    try {
      final StringBuffer buffer = new StringBuffer();

      final HashSet methodNames = new HashSet();
      java.lang.reflect.Method[] methods = this.getClass().getMethods();
      for (int i = 0; i < methods.length; ++i) {
        // if (Modifier.isStatic(methods[i].getModifiers())) continue;

        String name = methods[i].getName();
        if (name.length() <= 3) continue;
        if (name.startsWith("get") == false) continue;
        if (Character.toUpperCase(name.charAt(3)) != name.charAt(3)) continue;

        // Class[] parameterTypes = methods[i].getParameterTypes();
        // if (parameterTypes!=null || parameterTypes.length!=0) continue;

        name = name.substring(3);
        methodNames.add(name);
        methodNames.add(Character.toLowerCase(name.charAt(0)) + name.substring(1));
        methodNames.add(name.toUpperCase());
        methodNames.add(name.toLowerCase());
      }

      final Field[] fields = this.getClass().getFields();
      for (int i = 0; i < fields.length; ++i) {
        if (Modifier.isStatic(fields[i].getModifiers())) continue;

        String name = fields[i].getName();
        if (methodNames.contains(name)) continue;

        Object value = fields[i].get(this);
        if (value != null) {
          if (value instanceof XML) {
            buffer.append("\n<").append(name).append(">");
            StringTokenizer strTkzr = new StringTokenizer(((XML) value).toInnerXML(), "\n");
            while (strTkzr.hasMoreTokens())
              buffer.append("\n  ").append(strTkzr.nextToken());
            buffer.append("\n</").append(name).append(">");
          } else {
            if (value.getClass().isArray()) {
              Object[] array = (Object[]) value;
              for (int t = 0; t < array.length; ++t) {
                buffer.append("\n<").append(name).append(">");
                if (array[t] instanceof XML) {
                  StringTokenizer strTkzr = new StringTokenizer(((XML) array[t]).toInnerXML(), "\n");
                  while (strTkzr.hasMoreTokens())
                    buffer.append("\n  ").append(strTkzr.nextToken());
                } else {
                  buffer.append("\n    ").append(array[t]);
                }
                buffer.append("\n</").append(name).append(">");
              }
            } else {
              buffer.append("\n<").append(name).append(">");
              buffer.append("\n  ").append(value);
              buffer.append("\n</").append(name).append(">");
            }
          }
        }
      }

      for (int i = 0; i < methods.length; ++i) {
        String name = methods[i].getName();
        name = name.substring(3);
        if (methodNames.contains(name) == false) continue;
        Object value = methods[i].invoke(this, (Object[]) null);
        if (value != null) {
          if (value instanceof XML) {
            buffer.append("\n<").append(name).append(">");
            StringTokenizer strTkzr = new StringTokenizer(((XML) value).toInnerXML(), "\n");
            while (strTkzr.hasMoreTokens())
              buffer.append("\n  ").append(strTkzr.nextToken());
            buffer.append("\n</").append(name).append(">");
          } else {
            if (value.getClass().isArray()) {
              Object[] array = (Object[]) value;
              for (int t = 0; t < array.length; ++t) {
                buffer.append("\n<").append(name).append(">");
                if (array[t] instanceof XML) {
                  StringTokenizer strTkzr = new StringTokenizer(((XML) array[t]).toInnerXML(), "\n");
                  while (strTkzr.hasMoreTokens())
                    buffer.append("\n  ").append(strTkzr.nextToken());
                } else {
                  buffer.append("\n    ").append(array[t]);
                }
                buffer.append("\n</").append(name).append(">");
              }
            } else {
              buffer.append("\n<").append(name).append(">");
              buffer.append("\n  ").append(value);
              buffer.append("\n</").append(name).append(">");
            }
          }
        }
      }

      return buffer.toString();
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getMessage());
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public String toString() {
    final StringBuffer buffer = new StringBuffer();
    buffer.append(this.getClass().getName());
    buffer.append("\n").append(this.toInnerString());
    return buffer.toString();
  }

  protected String toInnerString() {
    try {
      final StringBuffer buffer = new StringBuffer();

      final HashSet methodNames = new HashSet();
      java.lang.reflect.Method[] methods = this.getClass().getMethods();
      for (int i = 0; i < methods.length; ++i) {
        // if (Modifier.isStatic(methods[i].getModifiers())) continue;

        String name = methods[i].getName();
        if (name.length() <= 3) continue;
        if (name.startsWith("get") == false) continue;
        if (Character.toUpperCase(name.charAt(3)) != name.charAt(3)) continue;

        // Class[] parameterTypes = methods[i].getParameterTypes();
        // if (parameterTypes!=null || parameterTypes.length!=0) continue;

        name = name.substring(3);
        methodNames.add(name);
        methodNames.add(Character.toLowerCase(name.charAt(0)) + name.substring(1));
        methodNames.add(name.toUpperCase());
        methodNames.add(name.toLowerCase());
      }

      final Field[] fields = this.getClass().getFields();
      for (int i = 0; i < fields.length; ++i) {
        if (Modifier.isStatic(fields[i].getModifiers())) continue;

        String name = fields[i].getName();
        if (methodNames.contains(name)) continue;

        Object value = fields[i].get(this);
        if (value != null) {
          if (value instanceof XML) {
            buffer.append("\n").append(name);
            StringTokenizer strTkzr = new StringTokenizer(((XML) value).toInnerString(), "\n");
            while (strTkzr.hasMoreTokens())
              buffer.append("\n  ").append(strTkzr.nextToken());
          } else {
            if (value.getClass().isArray()) {
              Object[] array = (Object[]) value;
              for (int t = 0; t < array.length; ++t) {
                buffer.append("\n").append(name);
                if (array[t] instanceof XML) {
                  StringTokenizer strTkzr = new StringTokenizer(((XML) array[t]).toInnerString(), "\n");
                  while (strTkzr.hasMoreTokens())
                    buffer.append("\n  ").append(strTkzr.nextToken());
                } else {
                  buffer.append("\n    ").append(array[t]);
                }
              }
            } else {
              buffer.append("\n").append(name).append(": ").append(value);
            }
          }
        }
      }

      for (int i = 0; i < methods.length; ++i) {
        String name = methods[i].getName();
        name = name.substring(3);
        if (methodNames.contains(name) == false) continue;
        Object value = methods[i].invoke(this, (Object[]) null);
        if (value != null) {
          if (value instanceof XML) {
            buffer.append("\n").append(name);
            StringTokenizer strTkzr = new StringTokenizer(((XML) value).toInnerString(), "\n");
            while (strTkzr.hasMoreTokens())
              buffer.append("\n  ").append(strTkzr.nextToken());
          } else {
            if (value.getClass().isArray()) {
              Object[] array = (Object[]) value;
              for (int t = 0; t < array.length; ++t) {
                buffer.append("\n").append(name);
                if (array[t] instanceof XML) {
                  StringTokenizer strTkzr = new StringTokenizer(((XML) array[t]).toInnerString(), "\n");
                  while (strTkzr.hasMoreTokens())
                    buffer.append("\n  ").append(strTkzr.nextToken());
                } else {
                  buffer.append("\n    ").append(array[t]);
                }
              }
            } else {
              buffer.append("\n").append(name).append(": ").append(value);
            }
          }
        }
      }

      return buffer.toString();
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getMessage());
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
