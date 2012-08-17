/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * DefaultArchiveDetector.java
 *
 * Created on 24. Dezember 2005, 00:01
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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
import java.util.*;
import java.util.regex.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.util.*;
import org.terracotta.agent.repkg.de.schlichtherle.util.regex.*;

/**
 * An {@link ArchiveDetector} which matches file paths against a pattern of
 * archive file suffixes in order to detect prospective archive files and
 * look up their corresponding {@link ArchiveDriver} in its <i>registry</i>.
 * <p>
 * When this class is loaded, it uses the current thread's context class
 * loader to enumerate all instances of the relative path
 * <i>META-INF/services/de.schlichtherle.io.registry.properties</i>
 * on the class path (this is to ensure that TrueZIP is compatible with JNLP /
 * Java Web Start and can be safely added to the boot class path or extension
 * class path).
 * These <i>configuration files</i> are processed in arbitrary order
 * to configure the <i>global registry</i> of archive file suffixes and
 * archive drivers.
 * This allows archive drivers to be &quot;plugged in&quot; by simply providing
 * their own configuration file somewhere on the class path.
 * One such instance is located inside the JAR for TrueZIP itself and contains
 * TrueZIP's default configuration (please refer to this file for full details
 * on the syntax).
 * Likewise, client applications may provide their own configuration
 * file somewhere on the class path in order to extend or override the settings
 * configured by TrueZIP and any optional plug-in drivers.
 * <p>
 * Each instance has a <i>local registry</i>. Constructors are provided which
 * allow an instance to:
 * <ol>
 * <li>Filter the set of archive file suffixes in the global registry.
 *     For example, <code><font color="#800080">"tar|zip"</font></code> could
 *     be accepted by the filter in order to recognize only the TAR and ZIP
 *     file formats.</li>
 * <li>Add custom archive file suffixes for supported archive types to the
 *     local registry in order to create <i>pseudo archive types</i>.
 *     For example, <code><font color="#800080">&quot;myapp&quot;</font></code>
 *     could be added as an custom archive file suffix for the JAR file format.</li>
 * <li>Add custom archive file suffixes and archive drivers to the local
 *     registry in order to support new archive types.
 *     For example, the suffix <code><font color="#800080">"7z&quot;</font></code>
 *     could be associated to a custom archive driver which supports the 7z
 *     file format.</li>
 * <li>Put multiple instances in a chain of responsibility:
 *     The first instance which holds a mapping for any given archive file
 *     suffix in its registry determines the archive driver to be used.</li>
 * </ol>
 * <p>
 * Altogether, this enables to build arbitrary complex configurations with
 * very few lines of Java code or properties in the configuration file(s).
 * <p>
 * Where a constructor expects a suffix list as a parameter, this string must
 * have the form <code>&quot;suffix[|suffix]*&quot;</code>, where
 * <code>suffix</code> is a combination of case insensitive letters.
 * Empty or duplicated suffixes and leading dots are silently ignored
 * and <code>null</code> is interpreted as an empty list.
 * As an example, the parameter <code>&quot;zip|jar&quot;</code> would cause
 * the archive detector to recognize ZIP and JAR files in a path.
 * The same would be true for <code>&quot;||.ZIP||.JAR||ZIP||JAR||&quot;</code>,
 * but this notation is discouraged because it's not in canonical form
 * (see {@link #getSuffixes}.
 * <p>
 * {@link ArchiveDriver} classes are loaded on demand by the
 * {@link #getArchiveDriver} method using the current thread's context class
 * loader. This usually happens when a client application instantiates the
 * {@link File} class.
 * <p>
 * This implementation is (virtually) immutable and thread safe.
 * <p>
 * Since TrueZIP 6.4, this class is serializable in order to meet the
 * requirements of the {@link org.terracotta.agent.repkg.de.schlichtherle.io.File} class.
 * However, it's not recommended to serialize DefaultArchiveDetector instances:
 * Together with the instance, all associated archive drivers are serialized,
 * too, which is pretty inefficient for a single instance.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @see ArchiveDetector#NULL
 * @see ArchiveDetector#DEFAULT
 * @see ArchiveDetector#ALL
 * @since TrueZIP 6.0
 */
public class DefaultArchiveDetector
        extends AbstractArchiveDetector
        implements Serializable {

    private static final long serialVersionUID = 848158760183179884L;

    /**
     * The canonical list of archive file suffixes in the global registry
     * which have been configured to be recognized by default.
     * 
     * @deprecated This field is not for public use and will vanish
     *             private access in the next major release.
     *             Use <code>ArchiveDetector.DEFAULT.getSuffixes()</code> instead.
     */
    public static final String DEFAULT_SUFFIXES
            = GlobalArchiveDriverRegistry.INSTANCE.defaultSuffixes;

    /**
     * The canonical list of all archive file suffixes in the global registry.
     * 
     * @deprecated This field is not for public use and will vanish
     *             private access in the next major release.
     *             Use <code>ArchiveDetector.ALL.getSuffixes()</code> instead.
     */
    public static final String ALL_SUFFIXES
            = GlobalArchiveDriverRegistry.INSTANCE.allSuffixes;

    /**
     * The local registry for archive file suffixes and archive drivers.
     * This could actually be the global registry
     * ({@link GlobalArchiveDriverRegistry#INSTANCE}), filtered by a custom
     * {@link #list}.
     */
    private final ArchiveDriverRegistry registry;

    /**
     * The canonical suffix list recognized by this archive detector.
     * This list is used to filter the registered archive file suffixes in
     * {@link #registry}.
     */
    private final String list;

    /**
     * The thread local matcher used to match archive file suffixes.
     * This field should be considered final!
     */
    private transient ThreadLocalMatcher matcher; // never transmit this over the wire!

    /**
     * Creates a new <code>DefaultArchiveDetector</code> by filtering the
     * global registry for all canonicalized suffixes in <code>list</code>.
     * 
     * @param list A list of suffixes which shall identify prospective
     *        archive files. May be <code>null</code> or empty, but must
     *        obeye the usual syntax.
     * @see DefaultArchiveDetector Syntax Definition for Suffix Lists
     * @throws IllegalArgumentException If any of the suffixes in the suffix
     *         list names a suffix for which no {@link ArchiveDriver} is
     *         configured in the global registry.
     */
    public DefaultArchiveDetector(final String list) {
        registry = GlobalArchiveDriverRegistry.INSTANCE;
        final SuffixSet set = new SuffixSet(list);
        final SuffixSet all = registry.suffixes();
        if (set.retainAll(all)) {
            final SuffixSet unknown = new SuffixSet(list);
            unknown.removeAll(all);
            throw new IllegalArgumentException("\"" + unknown + "\" (no archive driver installed for these suffixes)");
        }
        this.list = set.toString();
        matcher = new ThreadLocalMatcher(set.toRegex());
    }

    /**
     * Equivalent to
     * {@link #DefaultArchiveDetector(DefaultArchiveDetector, String, ArchiveDriver)
     * DefaultArchiveDetector(ArchiveDetector.NULL, list, driver)}.
     */
    public DefaultArchiveDetector(String list, ArchiveDriver driver) {
        this(NULL, list, driver);
    }

    /**
     * Creates a new <code>DefaultArchiveDetector</code> by
     * decorating the configuration of <code>delegate</code> with
     * mappings for all canonicalized suffixes in <code>list</code> to
     * <code>driver</code>.
     * 
     * @param delegate The <code>DefaultArchiveDetector</code> which's
     *        configuration is to be virtually inherited.
     * @param list A non-null, non-empty archive file suffix list, obeying
     *        the usual syntax.
     * @param driver The archive driver to map for the suffix list.
     *        This must either be an archive driver instance or
     *        <code>null</code>.
     *        A <code>null</code> archive driver may be used to shadow a
     *        mapping for the same archive driver in <code>delegate</code>,
     *        effectively removing it.
     * @see DefaultArchiveDetector Syntax Definition for Suffix Lists
     * @throws NullPointerException If <code>delegate</code> or
     *         <code>list</code> is <code>null</code>.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         suffix list.
     */
    public DefaultArchiveDetector(
            DefaultArchiveDetector delegate,
            String list,
            ArchiveDriver driver) {
        this(delegate, new Object[] { list, driver });
    }

    /**
     * Creates a new <code>DefaultArchiveDetector</code> by
     * decorating the configuration of <code>delegate</code> with
     * mappings for all entries in <code>config</code>.
     * 
     * @param delegate The <code>DefaultArchiveDetector</code> which's
     *        configuration is to be virtually inherited.
     * @param config An array of suffix lists and archive driver IDs.
     *        Each key in this map must be a non-null, non-empty archive file
     *        suffix list, obeying the usual syntax.
     *        Each value must either be an archive driver instance, an archive
     *        driver class, a string with the fully qualified name name of
     *        an archive driver class, or <code>null</code>.
     *        A <code>null</code> archive driver may be used to shadow a
     *        mapping for the same archive driver in <code>delegate</code>,
     *        effectively removing it.
     * @throws NullPointerException If any parameter or configuration element
     *         other than an archive driver is <code>null</code>.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         configuration.
     * @see DefaultArchiveDetector Syntax Definition for Suffix Lists
     */
    public DefaultArchiveDetector(
            DefaultArchiveDetector delegate,
            Object[] config) {
        this(delegate, toMap(config));
    }

    /**
     * Creates a new <code>DefaultArchiveDetector</code> by
     * decorating the configuration of <code>delegate</code> with
     * mappings for all entries in <code>config</code>.
     * 
     * @param delegate The <code>DefaultArchiveDetector</code> which's
     *        configuration is to be virtually inherited.
     * @param config A map of suffix lists and archive drivers.
     *        Each key in this map must be a non-null, non-empty archive file
     *        suffix list, obeying the usual syntax.
     *        Each value must either be an archive driver instance, an archive
     *        driver class, a string with the fully qualified name name of
     *        an archive driver class, or <code>null</code>.
     *        A <code>null</code> archive driver may be used to shadow a
     *        mapping for the same archive driver in <code>delegate</code>,
     *        effectively removing it.
     * @throws NullPointerException If any parameter or configuration element
     *         other than an archive driver is <code>null</code>.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         configuration.
     * @see DefaultArchiveDetector Syntax Definition for Suffix Lists
     */
    public DefaultArchiveDetector(
            final DefaultArchiveDetector delegate,
            final Map config) {
        registry = new ArchiveDriverRegistry(delegate.registry, config);
        final SuffixSet set = registry.decorate(new SuffixSet(delegate.list)); // may be a subset of delegate.registry.decorate(new SuffixSet())!
        list = set.toString();
        matcher = new ThreadLocalMatcher(set.toRegex());
    }

    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        matcher = new ThreadLocalMatcher(new SuffixSet(list).toRegex());
    }

    private static Map toMap(final Object[] config) {
        if (config == null)
            return null;

        final Map map = new LinkedHashMap((int) (config.length / .75) + 1); // order may be important!
        for (int i = 0, l = config.length; i < l; i++)
            map.put(config[i], config[++i]);

        return map;
    }

    /**
     * Looks up a registered archive driver for the given (file) path by
     * matching it against the set of configured archive file suffixes.
     * An archive driver is looked up in the registry as follows:
     * <ol>
     * <li>If the registry holds a string, it's supposed to be the fully
     *     qualified class name of an <code>ArchiveDriver</code>
     *     implementation. The class will be loaded using the context class
     *     loader of the current thread and stored in the registry.
     * <li>If the registry then holds a class instance, it's instantiated
     *     with its no-arguments constructor, cast to the
     *     <code>ArchiveDriver</code> type and stored in the registry.
     * <li>If the registry then holds an instance of an
     *     <code>ArchiveDriver</code> implementation, it's returned.
     * <li>Otherwise, <code>null</code> is returned.
     * </ol>
     *
     * @throws RuntimeException A subclass is thrown if loading or
     *         instantiating an archive driver class fails.
     */
    public ArchiveDriver getArchiveDriver(final String path) {
        final Matcher m = matcher.reset(path);
        if (!m.matches())
            return null;
        final ArchiveDriver driver = registry.getArchiveDriver(
                m.group(1).toLowerCase(Locale.ENGLISH));
        assert driver != null : "archive driver does not exist for a recognized suffix";
        return driver;
    }

    /**
     * Returns the set of archive file suffixes recognized by this archive
     * detector in canonical form.
     * 
     * @return Either <code>&quot;&quot;</code> to indicate an empty set or
     *         a string of the form <code>&quot;suffix[|suffix]*&quot;</code>,
     *         where <code>suffix</code> is a combination of lower case
     *         letters which does <em>not</em> start with a dot.
     *         The string never contains empty or duplicated suffixes and the
     *         suffixes are sorted in natural order.
     * @see #DefaultArchiveDetector(String)
     */
    public String getSuffixes() {
        return list; // canonical form
    }
}
