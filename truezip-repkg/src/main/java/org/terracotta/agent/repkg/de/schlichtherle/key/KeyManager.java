/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Copyright 2006 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.key;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;

/**
 * An abstract class which maintains a static map of {@link KeyProvider}
 * instances for any protected resource which clients need to create or open.
 * This key manager class is designed to be of general purpose:
 * Resources are simply represented by a string as their identifier, called
 * the <i>resource identifier</i> or <i>resource ID</i> for short.
 * For each resource ID, a key provider may be associated to it which handles
 * the actual retrieval of the key.
 * <p>
 * Clients need to call {@link #getInstance} to get the default instance.
 * Because the map of key providers and some associated methods are static
 * members of this class, the default instance of this class may be changed
 * dynamically (using {@link #setInstance}) without affecting already mapped
 * key providers.
 * This allows to change other aspects of the implementation dynamically
 * (the user interface for example) without affecting the key providers and hence the
 * keys.
 * <p>
 * Implementations need to subclass this class and provide a public
 * no-arguments constructor.
 * Finally, an instance of the implementation must be installed either by
 * calling {@link #setInstance(KeyManager)} or by setting the system property
 * <code>de.schlichtherle.key.KeyManager</code> to the fully qualified class
 * name of the implementation before this class is ever used.
 * In the latter case, the class will be loaded using the context class loader
 * of the current thread.
 * <p>
 * Note that class loading and instantiation may happen in a JVM shutdown hook,
 * so class initializers and constructors must behave accordingly.
 * In particular, it's not permitted to construct or use a Swing GUI there.
 * <p>
 * This class is thread safe.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class KeyManager {

    private static volatile KeyManager keyManager;

    /**
     * Maps resource IDs [String] -> providers [KeyProvider]
     */
    private static final Map providers = new HashMap();

    private final Map providerTypes = new HashMap();

    //
    // Static Methods.
    //

    /**
     * Returns the default instance of the key manager.
     * <p>
     * If the default instance has been explicitly set using
     * {@link #setInstance}, then this instance is returned.
     * <p>
     * Otherwise, the value of the system property
     * <code>de.schlichtherle.key.KeyManager</code> is considered:
     * <p>
     * If this system property is set, it must denote the fully qualified
     * class name of a subclass of this class. The class is loaded by name
     * using the current thread's context class loader and instantiated using
     * its public, no-arguments constructor.
     * <p>
     * Otherwise, if the JVM is running in headless mode and the API conforms
     * to JSE 6 (where the class <code>java.io.Console</code> is available),
     * then the console I/O based implementation in the class
     * {@link de.schlichtherle.key.passwd.console.PromptingKeyManager}
     * is loaded by name using the current thread's context class loader and
     * instantiated using its public, no-arguments constructor.
     * <p>
     * Otherwise, the Swing based implementation in the class
     * {@link org.terracotta.agent.repkg.de.schlichtherle.key.passwd.swing.PromptingKeyManager}
     * is loaded by name using the current thread's context class loader and
     * instantiated using its public, no-arguments constructor.
     * <p>
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     * 
     * @throws ClassCastException If the class name in the system property
     *         does not denote a subclass of this class.
     * @throws UndeclaredThrowableException If any other precondition on the
     *         value of the system property does not hold.
     */
    public static synchronized KeyManager getInstance() {
        if (keyManager != null)
            return keyManager;

        final String cn = System.getProperty(
                "de.schlichtherle.key.KeyManager",
                getDefaultKeyManagerClassName());
        try {
            final Class c = Thread.currentThread()
                    .getContextClassLoader().loadClass(cn);
            keyManager = (KeyManager) c.newInstance();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UndeclaredThrowableException(ex);
        }

        return keyManager;
    }

    private static String getDefaultKeyManagerClassName() {
        if (GraphicsEnvironment.isHeadless()) {
            try {
                Class.forName("java.io.Console");
                return "de.schlichtherle.key.passwd.console.PromptingKeyManager";
            } catch (ClassNotFoundException noJSE6ConsoleAvailable) {
                // Ignore and fall through - prompting will be disabled.
            }
        }

        return "de.schlichtherle.key.passwd.swing.PromptingKeyManager";
    }

    /**
     * Sets the default instance of the key manager explicitly.
     *
     * @param keyManager The key manager to use as the default instance.
     *        If this is set to <code>null</code>, on the next call to
     *        {@link #getInstance} the default instance will be recreated.
     */
    public static void setInstance(final KeyManager keyManager) {
        KeyManager.keyManager = keyManager;
    }

    /**
     * Maps the given key provider for the given resource identifier.
     *
     * @return The key provider previously mapped for the given resource
     *         identifier or <code>null</code> if no key provider was mapped.
     * @throws NullPointerException If <code>resourceID</code> or
     *         <code>provider</code> is <code>null</code>.
     */
    static final synchronized KeyProvider mapKeyProvider(
            String resourceID,
            KeyProvider provider)
    throws NullPointerException {
        if (resourceID == null || provider == null)
            throw new NullPointerException();
        return (KeyProvider) providers.put(resourceID, provider);
    }

    /**
     * Removes the key provider for the given resource identifier from the map.
     *
     * @return The key provider previously mapped for the given resource
     *         identifier or <code>null</code> if no key provider was mapped.
     * @throws NullPointerException If <code>resourceID</code> is
     *         <code>null</code>.
     */
    static synchronized final KeyProvider unmapKeyProvider(String resourceID)
    throws NullPointerException {
        if (resourceID == null)
            throw new NullPointerException();
        return (KeyProvider) providers.remove(resourceID);
    }

    /**
     * Resets the key provider for the given resource identifier, causing it
     * to forget its common key.
     * This works only if the key provider associated with the given resource
     * identifier is an instance of {@link AbstractKeyProvider}.
     * Otherwise, nothing happens.
     * 
     * @param resourceID The resource identifier.
     * @return Whether or not an instance of {@link AbstractKeyProvider}
     *         is mapped for the resource identifier and has been reset.
     */
    public static synchronized boolean resetKeyProvider(final String resourceID) {
        final KeyProvider provider = (KeyProvider) providers.get(resourceID);
        if (provider instanceof AbstractKeyProvider) {
            final AbstractKeyProvider skp = (AbstractKeyProvider) provider;
            skp.reset();
            return true;
        }
        return false;
    }

    /**
     * Resets the key provider for the given resource identifier, causing it
     * to forget its common key, and throws the key provider away.
     * If the key provider associated with the given resource identifier is
     * not an instance of {@link AbstractKeyProvider}, it is just removed from
     * the map.
     * 
     * @param resourceID The resource identifier.
     * @return Whether or not a key provider was mapped for the resource
     *         identifier and has been removed.
     */
    public static synchronized boolean resetAndRemoveKeyProvider(final String resourceID) {
        final KeyProvider provider = (KeyProvider) providers.get(resourceID);
        if (provider instanceof AbstractKeyProvider) {
            final AbstractKeyProvider skp = (AbstractKeyProvider) provider;
            skp.reset();
            final KeyProvider result = skp.removeFromKeyManager(resourceID);
            assert provider == result;
            return true;
        } else if (provider != null) {
            final KeyProvider previous = unmapKeyProvider(resourceID);
            assert provider == previous;
            return true;
        }
        return false;
    }
    
    /**
     * Resets all key providers, causing them to forget their respective
     * common key.
     * If a mapped key provider is not an instance of {@link AbstractKeyProvider},
     * nothing happens.
     */
    public static void resetKeyProviders() {
        forEachKeyProvider(new KeyProviderCommand() {
            public void run(String resourceID, KeyProvider provider) {
                if (provider instanceof AbstractKeyProvider) {
                    ((AbstractKeyProvider) provider).reset();
                }
            }
        });
    }

    /**
     * @deprecated Use {@link #resetAndRemoveKeyProviders} instead.
     */
    public static final void resetAndClearKeyProviders() {
        resetAndRemoveKeyProviders();
    }

    /**
     * Resets all key providers, causing them to forget their key, and removes
     * them from the map.
     * If the key provider associated with the given resource identifier is
     * not an instance of {@link AbstractKeyProvider}, it is just removed from
     * the map.
     * 
     * @since TrueZIP 6.1
     * @throws IllegalStateException If resetting or unmapping one or more
     *         key providers is prohibited by a constraint in a subclass of
     *         {@link AbstractKeyProvider}, in which case the respective key
     *         provider(s) are reset but remain mapped.
     *         The operation is continued normally for all other key providers.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    public static synchronized void resetAndRemoveKeyProviders() {
        class ResetAndRemoveKeyProvider implements KeyProviderCommand {
            IllegalStateException ise = null;

            public void run(String resourceID, KeyProvider provider) {
                if (provider instanceof AbstractKeyProvider) {
                    final AbstractKeyProvider skp = (AbstractKeyProvider) provider;
                    skp.reset();
                    try {
                        skp.removeFromKeyManager(resourceID); // support proper clean up!
                    } catch (IllegalStateException exc) {
                        ise = exc; // mark and forget any previous exception
                    }
                } else {
                    final KeyProvider previous = unmapKeyProvider(resourceID);
                    assert provider == previous;
                }
            }
        }
        
        final ResetAndRemoveKeyProvider cmd = new ResetAndRemoveKeyProvider();
        forEachKeyProvider(cmd);
        if (cmd.ise != null)
            throw cmd.ise;
    }

    /**
     * Executes a {@link KeyProviderCommand} for each mapped key provider.
     * It is safe to call any method of this class within the command,
     * even if it modifies the map of key providers.
     */
    protected static synchronized void forEachKeyProvider(
            final KeyProviderCommand command) {
        // We can't use an iterator because the command may modify the map.
        // Otherwise, resetAndClearKeyProviders() would fail with a
        // ConcurrentModificationException.
        final Set entrySet = providers.entrySet();
        final int n = entrySet.size();
        final Map.Entry[] entries
                = (Map.Entry[]) entrySet.toArray(new Map.Entry[n]);
        for (int i = 0; i < n; i++) {
            final Map.Entry entry = entries[i];
            final String resourceID = (String) entry.getKey();
            final KeyProvider provider = (KeyProvider) entry.getValue();
            command.run(resourceID, provider);
        }
    }

    /**
     * Implemented by sub classes to define commands which shall be executed
     * on key providers with the {@link #forEachKeyProvider} method.
     */
    protected interface KeyProviderCommand {
        void run(String resourceID, KeyProvider provider);
    }

    /**
     * Moves a key provider from one resource identifier to another.
     * This may be useful if a protected resource changes its identifier.
     * For example, if the protected resource is a file, the most obvious
     * identifier would be its canonical path name.
     * Calling this method then allows you to rename a file without the need
     * to retrieve its keys again, thereby possibly prompting (and confusing)
     * the user.
     * 
     * @return <code>true</code> if and only if the operation succeeded,
     *         which means that there was an instance of
     *         {@link KeyProvider} associated with
     *         <code>oldResourceID</code>.
     * @throws NullPointerException If <code>oldResourceID</code> or
     *         <code>newResourceID</code> is <code>null</code>.
     * @throws IllegalStateException If unmapping or mapping the key provider
     *         is prohibited by a constraint in a subclass of
     *         {@link AbstractKeyProvider}, in which case the transaction is
     *         rolled back before this exception is (re)thrown.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    public static synchronized boolean moveKeyProvider(
            final String oldResourceID,
            final String newResourceID)
    throws NullPointerException, IllegalStateException {
        if (oldResourceID == null || newResourceID == null)
            throw new NullPointerException();

        final KeyProvider provider = (KeyProvider) providers.get(oldResourceID);
        if (provider == null)
            return false;

        if (provider instanceof AbstractKeyProvider) {
            final AbstractKeyProvider skp = (AbstractKeyProvider) provider;
            // Implement transactional behaviour.
            skp.removeFromKeyManager(oldResourceID);
            try {
                skp.addToKeyManager(newResourceID);
            } catch (RuntimeException failure) {
                skp.addToKeyManager(oldResourceID);
                throw failure;
            }
        } else {
            unmapKeyProvider(oldResourceID);
            mapKeyProvider(newResourceID, provider);
        }

        return true;
    }

    //
    // Instance methods:
    //

    /**
     * Creates a new <code>KeyManager</code>.
     * This class does <em>not</em> map any key provider types.
     * This must be done in the subclass using {@link #mapKeyProviderType}.
     */
    public KeyManager() {
    }
    
    /**
     * Subclasses must use this method to register default implementation
     * classes for the interfaces {@link KeyProvider} and {@link AesKeyProvider}
     * and optionally other subinterfaces or subclasses of
     * <code>KeyProvider</code>.
     * This is best done in the constructor of the subclass.
     *
     * @param forKeyProviderType The type which shall be substituted with
     *        <code>useKeyProviderType</code> when determining a suitable
     *        run time type in <code>getKeyProvider(String, Class)</code>.
     * @param useKeyProviderType The type which shall be substituted for
     *        <code>forKeyProviderType</code> when determining a suitable
     *        run time type in <code>getKeyProvider(String, Class)</code>.
     * @throws NullPointerException If any of the parameters is
     *         <code>null</code>.
     * @throws IllegalArgumentException If <code>forKeyProviderType</code>
     *         is not assignment compatible to the <code>KeyProvider</code>
     *         interface,
     *         or if <code>useKeyProviderType</code> is the same as
     *         <code>forKeyProviderType</code>,
     *         or if <code>useKeyProviderType</code> is not assignment
     *         compatible to <code>forKeyProviderType</code>,
     *         or if <code>useKeyProviderType</code> does not provide a
     *         public constructor with no parameters.
     * @see #getKeyProvider(String, Class)
     * @since TrueZIP 6.1
     */
    protected final synchronized void mapKeyProviderType(
            final Class forKeyProviderType,
            final Class useKeyProviderType) {
        if (!KeyProvider.class.isAssignableFrom(forKeyProviderType)
                || !forKeyProviderType.isAssignableFrom(useKeyProviderType)
                || forKeyProviderType == useKeyProviderType)
            throw new IllegalArgumentException(
                    useKeyProviderType.getName()
                    + " must be a subclass or implementation of "
                    + forKeyProviderType.getName() + "!");
        try {
            useKeyProviderType.getConstructor(null);
        } catch (NoSuchMethodException noPublicNullaryConstructor) {
            final IllegalArgumentException iae = new IllegalArgumentException(
                    useKeyProviderType.getName() + " (no public nullary constructor)");
            iae.initCause(noPublicNullaryConstructor);
            throw iae;
        }
        providerTypes.put(forKeyProviderType, useKeyProviderType);
    }

    /**
     * Equivalent to <code>return {@link #getKeyProvider(String, Class)
     * getKeyProvider(resourceID, KeyProvider.class)};</code> - provided for
     * convenience.
     *
     * @deprecated Use #getKeyProvider(String, Class) instead.
     */
    public KeyProvider getKeyProvider(String resourceID) {
        return getKeyProvider(resourceID, KeyProvider.class);
    }

    /**
     * Returns the {@link KeyProvider} for the given resource identifier.
     * If no key provider is mapped, this key manager will determine an
     * appropriate class which is assignment compatible to
     * <code>keyProviderType</code> (but is not necessarily the same),
     * instantiate it, map the instance for the protected resource and return
     * it.
     * <p>
     * Client applications should specify an interface rather than an
     * implementation as the <code>keyProviderType</code> in order to allow
     * the key manager to instantiate a useful default implementation of this
     * interface unless another key provider was already mapped for the
     * protected resource.
     * <p>
     * <b>Example:</b>
     * The following example asks the default key manager to provide a
     * suitable implementation of the {@link AesKeyProvider} interface
     * for a protected resource.
     * <pre>
     * String pathname = file.getCanonicalPath();
     * KeyManager km = KeyManager.getInstance();
     * KeyProvider kp = km.getKeyProvider(pathname, AesKeyProvider.class);
     * Object key = kp.getCreateKey(); // may prompt the user
     * int ks;
     * if (kp instanceof AesKeyProvider) {
     *      // The run time type of the implementing class is determined
     *      // by the key manager.
     *      // Anyway, the AES key provider can be safely asked for a cipher
     *      // key strength.
     *      ks = ((AesKeyProvider) kp).getKeyStrength();
     * } else {
     *      // Unfortunately, another key provider was already mapped for the
     *      // pathname before - use default key strength.
     *      ks = AesKeyProvider.KEY_STRENGTH_256;
     * }
     * </pre>.
     *
     * @param resourceID The identifier of the protected resource.
     * @param keyProviderType Unless another key provider is already mapped
     *        for the protected resource, this denotes the root of the class
     *        hierarchy to which the run time type of the returned instance
     *        may belong.
     *        In case the key manager does not know a more suitable class in
     *        this hierarchy, this parameter must denote an implementation of
     *        the {@link KeyProvider} interface with a public no-argument
     *        constructor.
     * @return The {@link KeyProvider} mapped for the protected resource.
     *         If no key provider has been previously mapped for the protected
     *         resource, the run time type of this instance is guaranteed to be
     *         assignment compatible to the given <code>keyProviderType</code>.
     * @throws NullPointerException If <code>resourceID</code> or
     *         <code>keyProviderType</code> is <code>null</code>.
     * @throws ClassCastException If no other key provider is mapped for the
     *         protected resource and the given class is not an implementation
     *         of the <code>KeyProvider</code> interface.
     * @throws IllegalArgumentException If any other precondition on the
     *         parameter <code>keyProviderType</code> does not hold.
     * @see #getInstance
     */
    public synchronized KeyProvider getKeyProvider(
            final String resourceID,
            Class keyProviderType)
    throws NullPointerException, ClassCastException, IllegalArgumentException {
        if (resourceID == null)
            throw new NullPointerException();

        synchronized (KeyManager.class) {
            KeyProvider provider = (KeyProvider) providers.get(resourceID);
            if (provider == null) {
                final Class subst = (Class) providerTypes.get(keyProviderType);
                if (subst != null)
                    keyProviderType = subst;
                try {
                    provider = (KeyProvider) keyProviderType.newInstance();
                } catch (InstantiationException failure) {
                    IllegalArgumentException iae = new IllegalArgumentException(
                            keyProviderType.getName());
                    iae.initCause(failure);
                    throw iae;
                } catch (IllegalAccessException failure) {
                    IllegalArgumentException iae = new IllegalArgumentException(
                            keyProviderType.getName());
                    iae.initCause(failure);
                    throw iae;
                }
                setKeyProvider(resourceID, provider);
            }

            return provider;
        }
    }

    /**
     * Sets the key provider programmatically.
     * <p>
     * <b>Warning</b>: This method replaces any key provider previously
     * associated with the given resource ID and installs it as the return
     * value for {@link #getKeyProvider}.
     * While this allows a reasonable level of flexibility, it may easily
     * confuse users if they have already been prompted for a key by the
     * previous provider before and may negatively affect the security if the
     * provider is not properly guarded by the application.
     * Use with caution only!
     * 
     * @param resourceID The resource identifier to associate the key
     *        provider with.
     *        For an RAES encrypted ZIP file, this must be the canonical
     *        path name of the archive file.
     * @param provider The key provider for <code>resourceID</code>.
     *        For an RAES encrypted ZIP file, this must be an instance of
     *        the {@link AesKeyProvider} interface.
     * @throws NullPointerException If <code>resourceID</code> or
     *         <code>provider</code> is <code>null</code>.
     * @throws IllegalStateException If mapping this instance is prohibited
     *         by a constraint in a subclass of {@link AbstractKeyProvider}.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    public void setKeyProvider(
            final String resourceID,
            final KeyProvider provider)
    throws NullPointerException, IllegalStateException {
        /*if (resourceID == null || provider == null)
            throw new NullPointerException();*/

        if (provider instanceof AbstractKeyProvider) {
            ((AbstractKeyProvider) provider).addToKeyManager(resourceID);
        } else {
            mapKeyProvider(resourceID, provider);
        }
    }
}
