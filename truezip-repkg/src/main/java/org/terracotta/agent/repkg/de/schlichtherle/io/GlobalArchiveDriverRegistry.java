/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * GlobalArchiveDriverRegistry.java
 *
 * Created on 22. Januar 2007, 22:21
 */
/*
 * Copyright 2007 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.io;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;

/**
 * A global registry for archive file suffixes and archive drivers which is
 * configured by the set of all <i>configuration files</i> on the class path.
 * This registry does not have a delegate, so it can only be used as the tail
 * in a {@link ArchiveDriverRegistry registry chain}.
 * <p>
 * When this class is instantiated, it uses the current thread's context
 * class loader to enumerate all instances of the relative path
 * <i>META-INF/services/de.schlichtherle.io.registry.properties</i>
 * on the class path (this is to ensure that TrueZIP is compatible with JNLP
 * as used by Java Web Start and can be safely added to the Boot or Extension
 * Class Path).
 * <p>
 * The configuration files are processed in arbitrary order.
 * However, configuration files which contain the entry
 * <code>DRIVER=true</code> have lesser priority and will be overruled by
 * any other configuration files which do not contain this entry.
 * This is used by the default configuration file in TrueZIP's JAR:
 * It contains this entry in order to allow any client application provided
 * configuration file to overrule it.
 * <p>
 * Note that this class appears to be a singleton (there's not much point in
 * having multiple instances of this class, all with the same configuration).
 * However, it actually isn't a true singleton because it's
 * {@link Serializable} in order to support serialization of {@link File}
 * instances.
 * This implies that a JVM can send an instance of this class to another JVM,
 * which's own global archive driver registry instance may be configured
 * completely different by its local configuration files.
 * Of course, this requires that the
 * {@link org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.ArchiveDriver}s used by the global
 * archive driver registry are serializable, too.
 * <p>
 * Note that it's actually discouraged to serialize {@link File} instances.
 * It's only supported due to the implementation of this feature in its base
 * class <code>java.io.File</code>.
 * Instead of serializing <code>File</code> instances, a client application
 * should serialize path names instead, which are plain strings.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
final class GlobalArchiveDriverRegistry extends ArchiveDriverRegistry {

    private static final long serialVersionUID = 1579600190374703884L;

    private static final String CLASS_NAME
            = "de/schlichtherle/io/GlobalArchiveDriverRegistry".replace('/', '.'); // support code obfuscation - NOI18N
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private static final String KWD_NULL = "NULL";  // NOI18N
    private static final String KWD_ALL = "ALL";    // NOI18N

    private static final String PROP_KEY_DEFAULT_SUFFIXES
            = "de.schlichtherle.io.default";
    private static final String PROP_KEY_REGISTRY
            = "de.schlichtherle.io.registry";

    /** The (pseudo) singleton instance. */
    public static final GlobalArchiveDriverRegistry INSTANCE
            = new GlobalArchiveDriverRegistry();

    /**
     * The canonical list of archive file suffixes in the global registry
     * which have been configured to be recognized by default.
     */
    public final String defaultSuffixes;

    /**
     * The canonical list of all archive file suffixes in the global registry.
     */
    public final String allSuffixes;

    static {
        logger.config("banner"); // NOI18N
    }

    /**
     * Creates a new <code>GlobalArchiveDriverRegistry</code>.
     * This constructor logs some configuration messages at
     * <code>Level.CONFIG</code>.
     * If an exception occurs during processing of the configuration resource
     * files or no archive drivers are registered, then one or more warnings
     * messages are logged at <code>Level.WARNING</code>, but otherwise the
     * constructor terminates normally.
     * This is to ensure that TrueZIP can be used without throwing exceptions
     * in static initializers just because of a bug in a configuration
     * resource file.
     */
    private GlobalArchiveDriverRegistry() {
        registerArchiveDrivers();

        // Initialize suffix lists.
        // Note that retrieval of the default suffix list must be done first
        // in order to remove the DEFAULT key from the drivers map if present.
        // The driver lookup would throw an exception on this entry otherwise.
        defaultSuffixes = defaultSuffixes().toString();
        allSuffixes = suffixes().toString();

        logConfiguration();
    }

    /**
     * Returns the ordered list of relative path names for configuration files.
     * Prior elements take precedence.
     */
    private static final String[] getServices() {
        return System.getProperty(PROP_KEY_REGISTRY,
                "META-INF/services/de.schlichtherle.io.registry.properties" // since TrueZIP 6.5.2 - NOI18N
                + File.pathSeparator
                + "META-INF/services/" + CLASS_NAME + ".properties" // deprecated - NOI18N
                + File.pathSeparator
                + "META-INF/services/de.schlichtherle.io.archive.spi.ArchiveDriver.properties")
                .split("\\" + File.pathSeparator); // deprecated - NOI18N
    }

    private void registerArchiveDrivers() {
        final ArchiveDriverRegistry clientRegistry
                = new ArchiveDriverRegistry();
        final String[] services = getServices();
        for (int i = services.length; --i >= 0; )
            registerArchiveDrivers(services[i], this, clientRegistry);
        putAll(clientRegistry);
    }

    /**
     * Enumerates all resource URLs for <code>service</code> on the class
     * path using the current thread's context class loader and calls
     * {@link #registerArchiveDrivers(URL, ArchiveDriverRegistry, ArchiveDriverRegistry)}
     * on each instance.
     * <p>
     * Ensures that configuration files specified by client
     * applications always override configuration files specified
     * by driver implementations.
     */
    private static void registerArchiveDrivers(
            final String service,
            final ArchiveDriverRegistry driverRegistry,
            final ArchiveDriverRegistry clientRegistry) {
        assert service != null;
        assert driverRegistry != null;
        assert clientRegistry != null;

        final Enumeration urls;
        try {
            urls = Thread.currentThread().getContextClassLoader()
                    .getResources(service);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "lookup.ex", ex); // NOI18N
            return;
        }

        while (urls.hasMoreElements()) {
            final URL url = (URL) urls.nextElement();
            registerArchiveDrivers(url, driverRegistry, clientRegistry);
        }
    }

    /**
     * Loads and processes the given <code>url</code> in order to register
     * the archive drivers in its config resource file.
     */
    private static void registerArchiveDrivers(
            final URL url,
            final ArchiveDriverRegistry driverRegistry,
            final ArchiveDriverRegistry clientRegistry) {
        assert url != null;
        assert driverRegistry != null;
        assert clientRegistry != null;

        // Load the configuration map from the properties file.
        logger.log(Level.CONFIG, "loading", url); // NOI18N
        final Properties config = new Properties();
        try {
            final InputStream in = url.openStream();
            try {
                config.load(in);
                registerArchiveDrivers(config, driverRegistry, clientRegistry);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "loading.ex", ex); // NOI18N
            // Continue normally.
        }
    }

    /**
     * Processes the given <code>config</code> in order to register
     * its archive drivers.
     * 
     * @throws NullPointerException If any archive driver ID in the
     *         configuration is <code>null</code>.
     */
    private static void registerArchiveDrivers(
            final Map config,
            final ArchiveDriverRegistry driverRegistry,
            final ArchiveDriverRegistry clientRegistry) {
        assert config != null;
        assert driverRegistry != null;
        assert clientRegistry != null;

        // Consume and process DRIVER entry.
        final String driver = (String) config.remove(KWD_DRIVER);
        final boolean isDriver = Boolean.TRUE.equals(Boolean.valueOf(driver));

        // Select registry and register drivers.
        (isDriver ? driverRegistry : clientRegistry).registerArchiveDrivers(
                config, false);
    }

    /**
     * Consumes and processes the entry for the keyword <code>DEFAULT</code>
     * in the map.
     * If a suffix is specified for which no driver is registered, then a
     * warning is logged and the suffix is removed from the return value.
     *
     * @return The set of suffixes processed by evaluating the value of the
     *         entry with the key <code>DEFAULT</code> in the map of drivers.
     *         May be empty, but never <code>null</code>.
     */
    private SuffixSet defaultSuffixes() {
        final SuffixSet set;
        final String defaultSuffixesProperty
                = System.getProperty(PROP_KEY_DEFAULT_SUFFIXES);
        if (defaultSuffixesProperty != null) {
            set = new SuffixSet(defaultSuffixesProperty);
        } else {
            set = (SuffixSet) remove(KWD_DEFAULT);
            if (set == null)
                return new SuffixSet();
        }

        final SuffixSet all = suffixes();
        boolean clear = false;
        boolean addAll = false;
        for (final Iterator i = set.originalIterator(); i.hasNext(); ) {
            final String suffix = (String) i.next();
            if (KWD_NULL.equals(suffix)) {
                i.remove();
                clear = true;
            } else if (KWD_ALL.equals(suffix)) {
                i.remove();
                addAll = true;
            } else if (!all.contains(suffix)) {
                i.remove();
                logger.log(Level.WARNING, "unknownSuffix", suffix); // NOI18N
            }
        }
        if (clear)
            set.clear();
        else if (addAll)
            set.addAll(all);
        return set;
    }

    private void logConfiguration() {
        final Iterator i = entrySet().iterator();
        if (i.hasNext()) {
            do {
                final Map.Entry entry = (Map.Entry) i.next();
                logger.log(Level.CONFIG, "driverRegistered", // NOI18N
                        new Object[] { entry.getKey(), entry.getValue() });
            } while (i.hasNext());

            logger.log(Level.CONFIG, "allSuffixList", allSuffixes); // NOI18N
            if (defaultSuffixes.length() > 0)
                logger.log(Level.CONFIG, "defaultSuffixList", defaultSuffixes); // NOI18N
            else
                logger.config("noDefaultSuffixes"); // NOI18N
        } else {
            logger.warning("noDriversRegistered"); // NOI18N
        }
    }
}
