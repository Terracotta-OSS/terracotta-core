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


import java.lang.reflect.Array;
import java.util.Arrays;

import org.terracotta.agent.repkg.de.schlichtherle.util.ThreadLocalLong;

/**
 * This abstract class implements the base functionality required to be a
 * "friendly" {@link KeyProvider}.
 * Each instance of this class maintains a single key, which can be of any
 * run time type (it is just required to be an {@link Object}).
 * A clone of this key is returned on each call to {@link #getCreateKey}
 * and {@link #getOpenKey}.
 * Cloning is used for all array classes and all classes which properly
 * implement the {@link Cloneable} interface.
 * The class remains abstract because there is no meaningful template
 * implementation of the {@link #invalidOpenKeyImpl()} method.
 * <p>
 * Other than the key, this class is stateless.
 * Hence, instances may be shared among multiple protected resources,
 * causing them to use the same key.
 * However, this feature may be restricted by subclasses such as
 * {@link PromptingKeyProvider} for example.
 * <p>
 * This class is thread safe.
 *
 * @see KeyProvider
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4 (renamed from SharedKeyProvider)
 */
public abstract class AbstractKeyProvider implements KeyProvider {

    private Object key;

    private final ThreadLocalLong invalidated = new ThreadLocalLong();

    /**
     * Returns the single key maintained by this key provider.
     * Client applications should not call this method directly,
     * but rather call {@link #getOpenKey} or {@link #getCreateKey}.
     * It is intended to be used by subclasses and user interface classes only.
     *
     * @deprecated You should not use this method from this class.
     *             It will be moved to a subclass in the next major version release.
     */
    public synchronized Object getKey() {
        return key;
    }

    /**
     * Sets the single key maintained by this key provider.
     * Client applications should not call this method directly.
     * It is intended to be used by subclasses and user interface classes only.
     *
     * @deprecated You should not use this method from this class.
     *             It will be moved to a subclass in the next major version release.
     */
    public synchronized void setKey(Object key) {
        this.key = key;
    }

    /**
     * Forwards the call to {@link #getCreateKeyImpl}.
     *
     * @return A clone of the return value of <code>getCreateKeyImpl</code>.
     *         In case of an array, a shallow copy of the array is returned.
     * @throws UnknownKeyException If <code>getCreateKeyImpl</code> throws
     *         this or the returned key is <code>null</code>.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     * @see KeyProvider#getCreateKey
     */
    public Object getCreateKey() throws UnknownKeyException {
        final Object key = getCreateKeyImpl();
        if (key == null)
            throw new UnknownKeyException();
        return clone(key);
    }

    /**
     * Returns the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     *
     * @return A template for the <code>key</code> to use or <code>null</code>.
     * @throws UnknownKeyException If the required key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see KeyProvider#getCreateKey
     */
    protected Object getCreateKeyImpl() throws UnknownKeyException {
        return getKey();
    }

    /**
     * Forwards the call to {@link #getOpenKeyImpl} and enforces a three
     * seconds suspension penalty if {@link #invalidOpenKey} was called by
     * the same thread before.
     * Because this method is final, this qualifies the implementation in
     * this class as a "friendly" <code>KeyProvider</code> implementation,
     * even when subclassed.
     *
     * @return A clone of the return value of <code>getOpenKeyImpl</code>.
     *         In case of an array, a shallow copy of the array is returned.
     * @throws UnknownKeyException If <code>getOpenKeyImpl</code> throws
     *         this or the returned key is <code>null</code>.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     * @see KeyProvider#getOpenKey
     */
    public final Object getOpenKey() throws UnknownKeyException {
        try {
            final Object key = getOpenKeyImpl();
            if (key == null)
                throw new UnknownKeyException();
            return clone(key);
        } finally {
            enforceSuspensionPenalty();
        }
    }

    /**
     * Returns the key which should be used to open an existing protected
     * resource in order to access its contents.
     *
     * @return A template for the <code>key</code> to use or <code>null</code>.
     * @throws UnknownKeyException If the required key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see KeyProvider#getCreateKey
     */
    protected Object getOpenKeyImpl() throws UnknownKeyException {
        return getKey();
    }

    /**
     * This method logs the current time for the current thread, which
     * is later used by {@link #getOpenKey} to enforce the suspension penalty
     * and then calls {@link #invalidOpenKeyImpl}.
     * Because this method is final, this implementation qualifies as a
     * "friendly" <code>KeyProvider</code> implementation, even when subclassed.
     *
     * @see KeyProvider#invalidOpenKey
     */
    public final void invalidOpenKey() {
        invalidated.setValue(System.currentTimeMillis());
        invalidOpenKeyImpl();
    }

    /**
     * Sublasses must implement this method.
     *
     * @see KeyProvider#invalidOpenKey
     */
    protected abstract void invalidOpenKeyImpl();

    /**
     * This hook may be overridden to reset this key provider instance.
     * The implementation in this class does nothing.
     */
    public void reset() {
        // Do NOT call this - it limits the reusability!
        //resetKey();
    }

    /**
     * Returns a clone of the key, which may be <code>null</code>.
     * If the key is an array, a shallow copy of the array is
     * returned.
     * When overriding this method, please consider that the key
     * may be <code>null</code>.
     *
     * @deprecated You should not use or override this method.
     *             This method will vanish in the next major version release.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     */
    protected Object cloneKey() {
        final Object key = getKey();
        if (key == null)
            return null; // the clone of null is null, right? :-)
        return clone(key);
    }

    /**
     * Returns a clone of the key.
     * If the key is an array, a shallow copy of the array is returned.
     *
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     */
    static Object clone(final Object key) {
        // Could somebody please explain to me why the clone method is
        // declared "protected" in Object and Cloneable is just a marker
        // interface?
        // And furthermore, why does clone() called via reflection on an
        // array throw a NoSuchMethodException?
        // Somehow, this design doesn't speak to me...
        final Class c = key.getClass();
        if (c.isArray()) {
            final int l = Array.getLength(key);
            final Object p = Array.newInstance(c.getComponentType(), l);
            System.arraycopy(key, 0, p, 0, l);
            return p;
        } else {
            try {
                return key.getClass().getMethod("clone", null).invoke(key, null);
            } catch (RuntimeException ex) {
                throw ex; // pass on
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Clears the data structure of the key itself and sets the
     * reference to it to <code>null</code>.
     * If the key is an array, the array is filled with zero values
     * before setting the reference to <code>null</code>.
     * When overwriting this method, please consider that the key
     * may be <code>null</code>.
     *
     * @deprecated You should not use or override this method.
     *             This method will vanish in the next major version release.
     */
    protected void resetKey() {
        final Object key = getKey();
        if (key == null)
            return;
        
        setKey(null);

        synchronized (key) {
            if (key instanceof byte[])
                Arrays.fill((byte[]) key, (byte) 0);
            else if (key instanceof char[])
                Arrays.fill((char[]) key, (char) 0);
            else if (key instanceof short[])
                Arrays.fill((short[]) key, (short) 0);
            else if (key instanceof int[])
                Arrays.fill((int[]) key, 0);
            else if (key instanceof long[])
                Arrays.fill((long[]) key, (long) 0);
            else if (key instanceof float[])
                Arrays.fill((float[]) key, (float) 0);
            else if (key instanceof double[])
                Arrays.fill((double[]) key, (double) 0);
            else if (key instanceof boolean[])
                Arrays.fill((boolean[]) key, false);
            else if (key instanceof Object[])
                Arrays.fill((Object[]) key, null);
        }
    }

    private void enforceSuspensionPenalty() {
        final long last = invalidated.getValue();
        long delay;
        InterruptedException interrupted = null;
        while ((delay = System.currentTimeMillis() - last) < MIN_KEY_RETRY_DELAY) {
            try {
                Thread.sleep(MIN_KEY_RETRY_DELAY - delay);
            } catch (InterruptedException ex) {
                interrupted = ex;
            }
        }
        if (interrupted != null)
            Thread.currentThread().interrupt();
    }

    /**
     * Maps this instance as the key provider for the given resource
     * identifier in the {@link KeyManager}.
     * <p>
     * The key manager will use this method whenever it adds a key provider
     * which is actually an instance of this class.
     * This allows subclasses to add additional behaviour or constraints
     * whenever an instance is mapped in the <code>KeyManager</code>.
     *
     * @param resourceID The resource identifier to map this instance for.
     *
     * @return The key provider previously mapped for the given resource
     *         identifier or <code>null</code> if no key provider was mapped.
     *
     * @throws NullPointerException If <code>resourceID</code> is
     *         <code>null</code>.
     * @throws IllegalStateException If mapping this instance is prohibited
     *         by a constraint in a subclass.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    protected KeyProvider addToKeyManager(String resourceID)
    throws NullPointerException, IllegalStateException {
        return KeyManager.mapKeyProvider(resourceID, this);
    }

    /**
     * Remove this instance as the key provider for the given resource
     * identifier from the map in the {@link KeyManager}.
     * <p>
     * The key manager will use this method whenever it adds a key provider
     * which is actually an instance of this class.
     * This allows subclasses to add additional behaviour or constraints
     * whenever an instance is unmapped in the <code>KeyManager</code>.
     *
     * @param resourceID The resource identifier to unmap this instance from.
     * @return The key provider previously mapped for the given resource
     *         identifier.
     * @throws NullPointerException If <code>resourceID</code> is
     *         <code>null</code>.
     * @throws IllegalStateException If unmapping this instance is prohibited
     *         by a constraint in a subclass.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    protected KeyProvider removeFromKeyManager(String resourceID)
    throws NullPointerException, IllegalStateException {
        return KeyManager.unmapKeyProvider(resourceID);
    }
}
