/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

  private static final SessionFactory sessionFactory;

  static {
      try {
          // Create the SessionFactory from hibernate.cfg.xml
          sessionFactory = new Configuration().configure().buildSessionFactory();
      } catch (Throwable ex) {
          // Make sure you log the exception, as it might be swallowed
          System.err.println("Initial SessionFactory creation failed." + ex);
          throw new ExceptionInInitializerError(ex);
      }
  }

  public static SessionFactory getSessionFactory() {
      return sessionFactory;
  }

}
