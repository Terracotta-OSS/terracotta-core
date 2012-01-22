/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;

public abstract class AbstractTerracottaMBean extends StandardMBean implements NotificationEmitter, TerracottaMBean {

  private static final ResourceBundle            DEFAULT_BUNDLE          = getBundleForMBean(TerracottaMBean.class,
                                                                                             TCLogging
                                                                                                 .getLogger(TerracottaMBean.class));

  private static final boolean                   ENABLED                 = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getBoolean(TCPropertiesConsts.TC_MANAGEMENT_MBEANS_ENABLED);

  private final TCLogger                         logger;
  private final ResourceBundle                   beanBundle;
  private final boolean                          isNotificationBroadcaster;

  // NOTE: The use of NotificationBroadcasterSupport has been removed and re-implemented internally
  // to avoid issues with JDK logging (DEV-421)
  private final List                             notificationListeners   = new CopyOnWriteArrayList();
  private boolean                                isActive;

  protected static final MBeanNotificationInfo[] EMPTY_NOTIFICATION_INFO = {};

  protected AbstractTerracottaMBean(final Class mBeanInterface, final boolean isNotificationBroadcaster)
      throws NotCompliantMBeanException {
    this(mBeanInterface, isNotificationBroadcaster, ENABLED);
  }

  protected AbstractTerracottaMBean(final Class mBeanInterface, final boolean isNotificationBroadcaster,
                                    final boolean isActive) throws NotCompliantMBeanException {
    super(mBeanInterface);
    this.logger = TCLogging.getLogger(mBeanInterface);
    this.beanBundle = getBundleForMBean(mBeanInterface, logger);
    this.isNotificationBroadcaster = isNotificationBroadcaster;
    this.isActive = isActive;
  }

  public final String getInterfaceClassName() {
    return getMBeanInterface().getName();
  }

  public final boolean hasListeners() {
    return !notificationListeners.isEmpty();
  }

  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter,
                                      final Object obj) {
    // don't register listeners from foreign classloaders
    if (!isListenerInSameClassLoader(listener)) return;

    notificationListeners.add(new Listener(listener, filter, obj));
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    if (isNotificationBroadcaster()) {
      final RuntimeException re = new TCRuntimeException("MBean error: this MBean[" + getClass().getName()
                                                         + "] must override getNotificationInfo() since"
                                                         + " it broadcasts notifications");
      throw re;
    }
    return new MBeanNotificationInfo[0];
  }

  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter,
                                         final Object obj) throws ListenerNotFoundException {
    // ignore removal of listeners from foreign classloaders
    if (!isListenerInSameClassLoader(listener)) return;

    boolean removed = false;

    for (Iterator i = notificationListeners.iterator(); i.hasNext();) {
      Listener lsnr = (Listener) i.next();
      if (lsnr.listener == listener && lsnr.filter == filter && lsnr.handback == obj) {
        removed = true;
        notificationListeners.remove(lsnr);
      }
    }

    if (!removed) { throw new ListenerNotFoundException(); }
  }

  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    // ignore removal of listeners from foreign classloaders
    if (!isListenerInSameClassLoader(listener)) return;

    boolean removed = false;

    for (Iterator i = notificationListeners.iterator(); i.hasNext();) {
      Listener lsnr = (Listener) i.next();
      if (lsnr.listener == listener) {
        removed = true;
        notificationListeners.remove(lsnr);
      }
    }

    if (!removed) { throw new ListenerNotFoundException(); }
  }

  /**
   * In HotSpot, listener is always of class com.sun.jmx.interceptor.DefaultMBeanServerInterceptor$ListenerWrapper
   * and that class' classloader always is the system classloader.
   *
   * Instances of this class contain a reference to a delegate listener which is the object registered itself in
   * the JMX remote code and is responsible for forwarding produced notification events. That delegate listener is
   * of class com.sun.jmx.remote.opt.internal.ArrayNotificationBuffer$BufferListener and that class' classloader
   * always is the L1 classloader. This is pictured in the following diagram:
   *
   * -------------------------------------------------------------------------------------------------------
   * listener com.sun.jmx.interceptor.DefaultMBeanServerInterceptor$ListenerWrapper
   * |- <class> class com.sun.jmx.interceptor.DefaultMBeanServerInterceptor$ListenerWrapper
   * |  '- <classloader> java.lang.ClassLoader <system class loader>
   * '- listener com.sun.jmx.remote.opt.internal.ArrayNotificationBuffer$BufferListener
   *   '- <class> class com.sun.jmx.remote.opt.internal.ArrayNotificationBuffer$BufferListener
   *      '- <classloader> org.terracotta.express.L1Loader <L1 class loader>
   * -------------------------------------------------------------------------------------------------------
   *
   * The containing listener is always registered in the VM's MBean server which keeps a strong reference to it
   * as long as it isn't unregistered. Transitively, the contained listener is hard-referenced and keeps its
   * L1 classloader alive as long as the containing listener hasn't been unregistered.
   *
   * In some situations, like when rejoin kicks in, the L1 is shut down and re-created but any listener registered
   * on an L1 MBean by 3rd party code will keep a hard ref onto the L1 classloader, provoking a perm gen leak.
   *
   * This is the reason why we simply ignore the registration of NotificationListeners which are not from the same L1
   * classloader (ie: 'foreign' classloaders). The fact that the original listener gets wrapped in another one
   * whose classloader is the system classloader explains the extra reflection trickery.
   */
  private boolean isListenerInSameClassLoader(final NotificationListener listener) {
    if (logger.isDebugEnabled()) {
      logger.debug("checking notification listener " + listener + " - CL : " + listener.getClass()
          .getClassLoader());
    }
    ClassLoader currentCl = getClass().getClassLoader();
    if (logger.isDebugEnabled()) {
      logger.debug("current CL : " + currentCl);
    }

    try {
      Field[] declaredFields = listener.getClass().getDeclaredFields();
      for (Field field : declaredFields) {
        field.setAccessible(true);
        Object subListener = field.get(listener);
        if (subListener == null || !(subListener instanceof NotificationListener)) { continue; }
        ClassLoader fieldObjectCl = subListener.getClass().getClassLoader();

        if (logger.isDebugEnabled()) {
          logger.debug("checking notification listener field " + subListener + " - CL : " + fieldObjectCl);
        }
        if (fieldObjectCl != currentCl) {
          if (logger.isDebugEnabled()) {
            logger.debug("Unauthorized classloader of listener field " + subListener + 
                         ", NOT authorizing notification listener " + listener);
          }
          return false;
        }
      }
    } catch (Exception e) {
      logger.warn("Reflection error, NOT authorizing notification listener " + listener, e);
      return false;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Authorized notification listener " + listener);
    }
    return true;
  }

  public final void sendNotification(final Notification notification) {
    if (isEnabled()) {
      for (Iterator i = notificationListeners.iterator(); i.hasNext();) {
        Listener lsnr = (Listener) i.next();

        if (lsnr.filter == null || lsnr.filter.isNotificationEnabled(notification)) {
          lsnr.listener.handleNotification(notification, lsnr.handback);
        }
      }
    }
  }

  public final boolean isNotificationBroadcaster() {
    return isNotificationBroadcaster;
  }

  public final void enable() {
    setState(true);
  }

  public final void disable() {
    setState(false);
  }

  private synchronized void setState(final boolean isActive) {
    if (this.isActive && !isActive) {
      reset();
    }
    final boolean previous = this.isActive;
    this.isActive = isActive;
    if (previous != isActive) {
      enabledStateChanged();
    }
  }

  protected synchronized void enabledStateChanged() {
    //
  }

  public final synchronized boolean isEnabled() {
    return isActive;
  }

  /**
   * As far as I can tell (at least with the Sun implementation), most if not all of the {@link StandardMBean}
   * customization hooks for descriptions come through this one method. Since we are using a {@link ResourceBundle} we
   * don't really need to worry about the exact type of the feature (only the name), so we should be able to get away
   * with overriding only this particular method to supply descriptions.
   */
  @Override
  protected String getDescription(final MBeanFeatureInfo featureInfo) {
    final String name = featureInfo.getName();
    String bundleDescription = null;
    if (beanBundle != null) {
      try {
        bundleDescription = beanBundle.getString(name);
      } catch (MissingResourceException mre) {
        if (DEFAULT_BUNDLE != null) {
          try {
            bundleDescription = DEFAULT_BUNDLE.getString(name);
          } catch (MissingResourceException defaultMre) {
            // We tried :)
          }
        }
      } catch (Throwable t) {
        // Not important enough to do anything about, but the log might reveal an operational problem
        logger.warn("Unexpected error while trying to retrieve feature description[" + name + "]", t);
      } finally {
        if (bundleDescription == null) {
          bundleDescription = super.getDescription(featureInfo);
        }
      }
    }
    return bundleDescription;
  }

  private static ResourceBundle getBundleForMBean(final Class mBeanInterface, final TCLogger logger) {
    ResourceBundle bundle = null;
    try {
      bundle = ResourceBundle.getBundle(mBeanInterface.getName(), Locale.getDefault(),
                                        AbstractTerracottaMBean.class.getClassLoader());
    } catch (MissingResourceException mre) {
      /* Caller must deal with null return value when missing */
    } catch (Throwable t) {
      logger.warn("Unexpected error loading resource bundle for MBean " + mBeanInterface.getName(), t);
    }
    return bundle;
  }

  private static class Listener {
    private final NotificationListener listener;
    private final NotificationFilter   filter;
    private final Object               handback;

    Listener(NotificationListener listener, NotificationFilter filter, Object obj) {
      this.listener = listener;
      this.filter = filter;
      this.handback = obj;
    }

  }

}
