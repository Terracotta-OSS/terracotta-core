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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An abstract {@link KeyManager} which prompts the user for a key if required.
 * <p>
 * This class maintains a map of user interface classes for the
 * <code>PromptingKeyProvider</code> class and each of its subclasses which
 * require an individual user interface.
 * The particular user interface classes are determined by a subclass of this
 * key manager. This enables the subclass to determine which user interface
 * technology should actually be used to prompt the user for a key.
 * For example, the implementation in the class
 * {@link org.terracotta.agent.repkg.de.schlichtherle.key.passwd.swing.PromptingKeyManager} uses Swing
 * to prompt the user for either a password or a key file.
 * <p>
 * Subclasses must use the method {@link #mapPromptingKeyProviderUIType} to
 * register a user interface class for a particular user interface class
 * identifier (the value returned by {@link PromptingKeyProvider#getUIClassID}).
 * This is best done in the constructor of the subclass.
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
public abstract class PromptingKeyManager extends KeyManager {

    private static volatile boolean prompting = true;

    private final Map providerUITypes = new HashMap();

    /**
     * Constructs a new <code>PromptingKeyManager</code>.
     * This instance maps the following key provider types using
     * {@link KeyManager#mapKeyProviderType}:
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>forKeyProviderType</th>
     *   <th>useKeyProviderType</th>
     * </tr>
     * <tr>
     *   <td>KeyProvider.class</td>
     *   <td>PromptingKeyProvider.class</td>
     * </tr>
     * <tr>
     *   <td>AesKeyProvider.class</td>
     *   <td>PromptingAesKeyProvider.class</td>
     * </tr>
     * </table>
     */
    public PromptingKeyManager() {
        mapKeyProviderType(KeyProvider.class, PromptingKeyProvider.class);
        mapKeyProviderType(AesKeyProvider.class, PromptingAesKeyProvider.class);
    }

    //
    // Static methods:
    //

    /**
     * Returns <code>true</code> if and only if prompting mode is enabled.
     * This is a class property.
     * <p>
     * Note that subclasses might add additional behaviour to both
     * {@link #isPrompting} and {@link #setPrompting} through the default
     * key manager instance (see {@link #getInstance}).
     * Regardless, an application may safely assume that
     * <code>isPrompting()</code> reflects the actual behaviour of the API
     * in this package although it may not reflect the parameter value of
     * the last call to <code>setPrompting(boolean)</code>.
     * 
     * @return Whether or not the user will be prompted for a key if required.
     *
     * @see #setPrompting
     */
    public static boolean isPrompting() {
        KeyManager manager = getInstance();
        return manager instanceof PromptingKeyManager
                && ((PromptingKeyManager) manager).isPromptingImpl();
    }

    /**
     * Called by {@link #isPrompting} on the default key manager instance in
     * order to implement its behaviour and allow subclasses to override it.
     * Subclasses should call the implementation in this class when
     * overriding this method.
     * 
     * @see #setPromptingImpl
     * @see #getInstance
     *
     * @since TrueZIP 6.1
     */
    protected boolean isPromptingImpl() {
        return prompting;
    }

    /**
     * Enables or disables prompting mode.
     * If prompting mode is enabled, the user will be prompted for a key when
     * a {@link PromptingKeyProvider} is first requested to provide a key
     * for the respective resource.
     * If prompting mode is disabled, all attempts to prompt the user will
     * result in an {@link UnknownKeyException} until prompting mode is
     * enabled again.
     * <p>
     * This is a class property.
     * <p>
     * Note that subclasses might add additional behaviour to both
     * {@link #isPrompting} and {@link #setPrompting} through the default
     * key manager instance (see {@link #getInstance}).
     * Regardless, an application may safely assume that
     * <code>isPrompting()</code> reflects the actual behaviour of the API
     * in this package although it may not reflect the parameter value of
     * the last call to <code>setPrompting(boolean)</code>.
     * 
     * @param prompting The value of the property <code>prompting</code>.
     *
     * @see #isPrompting
     */
    public static void setPrompting(boolean prompting) {
        KeyManager manager = getInstance();
        if (manager instanceof PromptingKeyManager)
                ((PromptingKeyManager) manager).setPromptingImpl(prompting);
    }

    /**
     * Called by {@link #isPrompting} on the default key manager instance in
     * order to implement its behaviour and allow subclasses to override it.
     * Subclasses should call the implementation in this class when
     * overriding this method.
     * 
     * @see #isPromptingImpl
     * @see #getInstance
     *
     * @since TrueZIP 6.1
     */
    protected void setPromptingImpl(boolean prompting) {
        PromptingKeyManager.prompting = prompting;
    }

    static void ensurePrompting()
    throws KeyPromptingDisabledException {
        KeyManager manager = getInstance();
        if (manager instanceof PromptingKeyManager)
                ((PromptingKeyManager) manager).ensurePromptingImpl();
    }

    /**
     * Called by some methods in the {@link PromptingKeyProvider} class in
     * order to ensure that prompting mode is enabled.
     * This method may be overridden by subclasses in order to throw a more
     * detailed exception.
     * <p>
     * The implementation in this class is equivalent to:
     * <pre>
        if (!isPromptingImpl())
            throw new KeyPromptingDisabledException();
     * </pre>
     *
     * @since TrueZIP 6.1
     */
    protected void ensurePromptingImpl()
    throws KeyPromptingDisabledException {
        if (!isPromptingImpl())
            throw new KeyPromptingDisabledException();
    }

    /**
     * Resets all cancelled key prompts, forcing a new prompt on the next
     * call to {@link PromptingKeyProvider#getOpenKey()} or
     * {@link PromptingKeyProvider#getCreateKey()}.
     * Of course, this call only affects instances of
     * {@link PromptingKeyProvider}.
     */
    public static void resetCancelledPrompts() {
        forEachKeyProvider(new KeyProviderCommand() {
            public void run(String resourceID, KeyProvider provider) {
                if (provider instanceof PromptingKeyProvider)
                    ((PromptingKeyProvider) provider).resetCancelledPrompt();
            }
        });
    }

    //
    // Instance stuff:
    //

    /**
     * @deprecated Use {@link #mapPromptingKeyProviderUIType(String, Class)
     * mapPromptingKeyProviderUIType(uiClassID, uiClass)} instead.
     */
    protected final void register(
            final String uiClassID,
            final Class uiClass) {
        mapPromptingKeyProviderUIType(uiClassID, uiClass);
    }

    /**
     * Subclasses must use this method to register a user interface class
     * for a particular user interface class identifier as returned by
     * {@link PromptingKeyProvider#getUIClassID}.
     * This is best done in the constructor of the subclass.
     *
     * @param uiClassID The identifier of the user interface class.
     * @param uiClass The class of the user interface. This must have
     *
     * @throws NullPointerException If any of the parameters is
     *         <code>null</code>.
     * @throws IllegalArgumentException If the runtime type of uiClass is not
     *         {@link PromptingKeyProviderUI} or a subclass or does not provide
     *         a public constructor with no parameters.
     *
     * @see #getKeyProvider(String, Class)
     *
     * @since TrueZIP 6.1
     */
    protected synchronized final void mapPromptingKeyProviderUIType(
            final String uiClassID,
            final Class uiClass) {
        if (uiClassID == null)
            throw new NullPointerException("uiClassID");
        if (!PromptingKeyProviderUI.class.isAssignableFrom(uiClass))
            throw new IllegalArgumentException(
                    uiClass.getName() + " must be PromptingKeyProviderUI or a subclass!");
        try {
            uiClass.getConstructor(null);
        } catch (NoSuchMethodException noPublicNullaryConstructor) {
            final IllegalArgumentException iae = new IllegalArgumentException(
                    uiClass.getName() + " (no public nullary constructor)");
            iae.initCause(noPublicNullaryConstructor);
            throw iae;
        }
        providerUITypes.put(uiClassID, uiClass);
    }

    /**
     * Behaves like the super class implementation, but adds additional
     * behaviour in case the resulting key provider is an instance of
     * {@link PromptingKeyProvider}.
     * In this case, the appropriate user interface instance is determined
     * and installed in the key provider before it is returned.
     *
     * @see KeyManager#getKeyProvider(String, Class)
     */
    public KeyProvider getKeyProvider(
            final String resourceID,
            final Class keyProviderType)
    throws NullPointerException, ClassCastException, IllegalArgumentException {
        final KeyProvider provider
                = super.getKeyProvider(resourceID, keyProviderType);

        if (provider instanceof PromptingKeyProvider) {
            final PromptingKeyProvider pkp = (PromptingKeyProvider) provider;
            pkp.setUI(getUI(pkp.getUIClassID()));
        }

        return provider;
    }

    private synchronized PromptingKeyProviderUI getUI(final String uiClassID) {
        final Object value = providerUITypes.get(uiClassID);

        final PromptingKeyProviderUI pkpui;
        if (value instanceof Class) {
            try {
                pkpui = (PromptingKeyProviderUI) ((Class) value).newInstance();
            } catch (InstantiationException failure) {
                throw new UndeclaredThrowableException(failure);
            } catch (IllegalAccessException failure) {
                throw new UndeclaredThrowableException(failure);
            }
            providerUITypes.put(uiClassID, pkpui);
        } else if (value != null) {
            pkpui = (PromptingKeyProviderUI) value;
        } else { // value == null
            throw new IllegalArgumentException(uiClassID +
                    " (unknown user interface for PromptingKeyProvider)");
        }

        return pkpui;
    }
}
