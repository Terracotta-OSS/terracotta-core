/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class AbstractFactory {
  public static AbstractFactory getFactory(String id, Class defaultImpl) {
    String factoryClassName = findFactoryClassName(id);
    AbstractFactory factory = null;
    
    if(factoryClassName != null) {
      try {
        factory = (AbstractFactory)Class.forName(factoryClassName).newInstance();
      } catch(Exception e) {
        throw new RuntimeException("Could not instantiate '"+factoryClassName+"'", e);
      }
    }
    
    if(factory == null) {
      try {
        factory = (AbstractFactory)defaultImpl.newInstance();
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
    }

    return factory;
  }
    
    private static String findFactoryClassName(String id) {
      String serviceId = "META-INF/services/"+id;
      InputStream is = null;

      ClassLoader cl = AbstractFactory.class.getClassLoader();
      if (cl != null) {
        is = cl.getResourceAsStream(serviceId);
      }

      if (is == null) {
        return System.getProperty(id);
      }

      BufferedReader rd;
      try {
        rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      } catch (java.io.UnsupportedEncodingException e) {
        rd = new BufferedReader(new InputStreamReader(is));
      }
      
      String factoryClassName = null;
      try {
        factoryClassName = rd.readLine();
        rd.close();
      } catch(IOException x) {
        return System.getProperty(id);
      }

      if (factoryClassName != null && !"".equals(factoryClassName)) {
        return factoryClassName;
      }
    
      return System.getProperty(id);
    }
  }
