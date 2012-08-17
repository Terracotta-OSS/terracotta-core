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

/**
 * A "friendly" implementation of {@link KeyProvider} which prompts the user
 * for a key for its protected resource, enforcing a three seconds suspension
 * penalty if a wrong key was provided.
 * The user is prompted via an instance of the {@link PromptingKeyProviderUI}
 * user interface which is determined by the default instance of
 * {@link PromptingKeyManager} as returned by {@link KeyManager#getInstance}.
 * <p>
 * Like its base class, this class does not impose a certain run time type
 * of the key.
 * It is actually the user interface implementation which determines the run
 * time type of the key provided by {@link #getCreateKey} and
 * {@link #getOpenKey}.
 * Because the user interface implementation is determined by the singleton
 * {@link PromptingKeyManager}, it is ultimately at the discretion of
 * the key manager which type of keys are actually provided by this class.
 * <p>
 * Unlike its base class, instances of this class cannot get shared
 * among multiple protected resources because each instance has a unique
 * {@link #getResourceID() resource identifier} associated with it.
 * Each try to share a key provider of this class among multiple protected
 * resources with the singleton {@link KeyManager} will be prosecuted and
 * sentenced with an {@link IllegalStateException} or, at the discretion of
 * this class, some other {@link RuntimeException}.
 * <p>
 * This class is thread safe.
 * 
 * @see PromptingKeyProviderUI
 * @see KeyProvider
 * @see PromptingKeyManager
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class PromptingKeyProvider extends AbstractKeyProvider {

    /**
     * Used to lock out prompting by multiple threads.
     * Note that the prompting methods in this class <em>must not</em> be
     * synchronized on this instance since this would cause the Swing
     * based default implementation of the key manager to dead lock.
     * This is because the GUI is run from AWT's Event Dispatching Thread,
     * which must call some methods of this instance while another thread
     * is waiting for the key manager to return from prompting.
     * Instead, the prompting methods use this object to lock out concurrent
     * prompting by multiple threads.
     */
    private final PromptingLock lock = new PromptingLock();

    /**
     * The resource identifier for the protected resource.
     */
    private String resourceID;

    /**
     * The user interface instance which is used to prompt the user for a key.
     */
    private PromptingKeyProviderUI ui;

    private State state = Reset.STATE;

    /**
     * Returns the unique resource identifier of the protected resource
     * for which this key provider is used.
     */
    public synchronized String getResourceID() {
        return resourceID;
    }

    final synchronized void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Returns the identifier which is used by the {@link PromptingKeyManager}
     * to look up an instance of the {@link PromptingKeyProviderUI} user
     * interface class which is then used to prompt the user for a key.
     * The implementation in this class returns the fully qualified name
     * of this class.
     * <p>
     * Subclasses which want to use a custom user interface should overwrite
     * this method to return the fully qualified name of their respective
     * class as the identifier and provide a custom
     * <code>PromptingKeyManager</code> which has registered a
     * <code>PromptingKeyProviderUI</code> class for this identifier.
     */
    protected String getUIClassID() {
        return "PromptingKeyProvider"; // support code obfuscation!
    }

    private synchronized final PromptingKeyProviderUI getUI() {
        return ui;
    }

    final synchronized void setUI(final PromptingKeyProviderUI ui) {
        this.ui = ui;
    }

    private synchronized final State getState() {
        return state;
    }

    private synchronized final void setState(final State state) {
        this.state = state;
    }

    /**
     * Returns a clone of the key which may be used to create a new
     * protected resource or entirely replace the contents of an already
     * existing protected resource.
     * Returns the key itself if cloning it fails for some reason.
     * If the key is an array, a shallow copy of the array is
     * returned.
     * <p>
     * If required or explicitly requested by the user, the user is prompted
     * for this key.
     *
     * @throws UnknownKeyException If the user has cancelled prompting or
     *         prompting has been disabled by the {@link PromptingKeyManager}.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     *
     * @see KeyProvider#getCreateKey
     */
    public final Object getCreateKeyImpl() throws UnknownKeyException {
        synchronized (lock) {
            return getState().getCreateKey(this);
        }
    }

    /**
     * Prompts for a key to create or entirely overwrite a protected resource.
     */
    private Object promptCreateKey()
    throws UnknownKeyException {
        PromptingKeyManager.ensurePrompting();

        final Object oldKey = getKey();

        try {
            final PromptingKeyProviderUI ui = getUI();
            ui.promptCreateKey(this);
        } catch (RuntimeException failure) {
            // If the cause is an UnkownKeyException, pass it on without
            // changing the state. This is used by the PromptingKeyProviderUI
            // class to indicate that key prompting has been interrupted, so
            // we could expect the cause to be a
            // KeyPromptingInterruptedException actually.
            final Throwable cause = failure.getCause();
            if (cause instanceof UnknownKeyException)
                throw (UnknownKeyException) cause;
            else
                throw failure;
        }

        resetKey(oldKey);

        final Object newKey = getKey();
        if (newKey != null) {
            setState(KeyChanged.STATE);
            return newKey;
        } else {
            setState(Cancelled.STATE);
            throw new KeyPromptingCancelledException();
        }
    }

    /**
     * Returns a clone of the key which may be used to open an
     * existing protected resource in order to access its contents.
     * Returns the key itself if cloning it fails for some reason.
     * If the key is an array, a shallow copy of the array is
     * returned.
     * <p>
     * If required, the user is prompted for this key.
     * <p>
     * This method enforces a three seconds suspension penalty if
     * {@link #invalidOpenKey} was called by the same thread before
     * in order to qualify as a "friendly" implementation.
     *
     * @throws UnknownKeyException If the user has cancelled prompting or
     *         prompting has been disabled by the {@link PromptingKeyManager}.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     *
     * @see KeyProvider#getOpenKey
     */
    protected final Object getOpenKeyImpl() throws UnknownKeyException {
        synchronized (lock) {
            return getState().getOpenKey(this);
        }
    }

    /**
     * Prompts for a key to open a protected resource.
     */
    private Object promptOpenKey(final boolean invalid)
    throws UnknownKeyException {
        PromptingKeyManager.ensurePrompting();

        final Object oldKey = getKey();

        final boolean changeKey;
        try {
            final PromptingKeyProviderUI ui = getUI();
            if (invalid)
                changeKey = ui.promptInvalidOpenKey(this);
            else
                changeKey = ui.promptUnknownOpenKey(this);
        } catch (RuntimeException failure) {
            // If the cause is an UnkownKeyException, pass it on without
            // changing the state. This is used by the PromptingKeyProviderUI
            // class to indicate that key prompting has been interrupted, so
            // we could expect the cause to be a
            // KeyPromptingInterruptedException actually.
            final Throwable cause = failure.getCause();
            if (cause instanceof UnknownKeyException)
                throw (UnknownKeyException) cause;
            else
                throw failure;
        }

        resetKey(oldKey);

        final Object newKey = getKey();
        if (newKey != null) {
            if (changeKey)
                setState(KeyChangeRequested.STATE);
            else
                setState(KeyProvided.STATE);
            return newKey;
        } else {
            setState(Cancelled.STATE);
            throw new KeyPromptingCancelledException();
        }
    }

    private void resetKey(final Object oldKey) {
        if (oldKey != null && oldKey != getKey()) {
            final Object newKey = getKey();
            try {
                setKey(oldKey);
                resetKey();
            } finally {
                setKey(newKey);
            }
        }
    }

    /**
     * Called to indicate that authentication of the key returned by
     * {@link #getOpenKey()} has failed and to request an entirely different
     * key.
     * The user is prompted for a new key on the next call to
     * {@link #getOpenKey}.
     * Note that the user may actually not be prompted at the next call to
     * {@link #getOpenKey} again if prompting has been disabled by the
     * {@link PromptingKeyManager} or this provider is in a state where
     * calling this method does not make any sense.
     *
     * @see KeyProvider#invalidOpenKey
     */
    protected final void invalidOpenKeyImpl() {
        synchronized (lock) {
            getState().invalidOpenKey(this);
        }
    }

    /**
     * Resets this key provider if and only if prompting for a key has been
     * cancelled.
     * It is safe to call this method while another thread is actually
     * prompting for a key.
     */
    final void resetCancelledPrompt() {
        getState().resetCancelledPrompt(this);
    }

    /**
     * Resets this key provider and finally calls {@link #onReset()}.
     */
    public synchronized final void reset() {
        setState(Reset.STATE);
        try {
            resetKey();
        } finally {
            onReset();
        }
    }

    /**
     * This hook is run after {@link #reset()} has been called.
     * This method is called from the constructor in the class
     * {@link AbstractKeyProvider}.
     * The implementation in this class does nothing.
     * May be overwritten by subclasses.
     */
    protected void onReset() {
    }

    /**
     * Like the super class implementation, but throws an
     * {@link IllegalStateException} if this instance is already mapped for
     * another resource identifier.
     *
     * @throws IllegalStateException If this instance is already mapped for
     *         another resource identifier or mapping is prohibited
     *         by a constraint in a subclass. In the latter case, please refer
     *         to the subclass documentation for more information.
     */
    protected synchronized KeyProvider addToKeyManager(final String resourceID)
    throws NullPointerException, IllegalStateException {
        final String oldResourceID = getResourceID();
        if (oldResourceID != null && !oldResourceID.equals(resourceID))
            throw new IllegalStateException(
                    "PromptingKeyProvider instances cannot be shared!");
        final KeyProvider provider = super.addToKeyManager(resourceID);
        setResourceID(resourceID);

        return provider;
    }

    protected synchronized KeyProvider removeFromKeyManager(final String resourceID)
    throws NullPointerException, IllegalStateException {
        final KeyProvider provider = super.removeFromKeyManager(getResourceID());
        setResourceID(null);

        return provider;
    }

    //
    // Shared (flyweight) state member classes.
    //

    private abstract static class State {
        public abstract Object getCreateKey(PromptingKeyProvider provider)
        throws UnknownKeyException;

        public abstract Object getOpenKey(PromptingKeyProvider provider)
        throws UnknownKeyException;

        public void invalidOpenKey(PromptingKeyProvider provider) {
        }

        public void resetCancelledPrompt(PromptingKeyProvider provider) {
        }
    }

    private static class Reset extends State {
        private static final State STATE = new Reset();

        public Object getCreateKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            return provider.promptCreateKey();
        }

        public Object getOpenKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            return provider.promptOpenKey(false);
        }

        public void invalidOpenKey(PromptingKeyProvider provider) {
        }
    }

    private static class KeyInvalidated extends Reset {
        private static final State STATE = new KeyInvalidated();

        public Object getOpenKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            return provider.promptOpenKey(true);
        }
    }

    private static class KeyProvided extends State {
        private static final State STATE = new KeyProvided();

        public Object getCreateKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            return provider.getKey();
        }

        public Object getOpenKey(PromptingKeyProvider provider) {
            return provider.getKey();
        }

        public void invalidOpenKey(PromptingKeyProvider provider) {
            provider.setState(KeyInvalidated.STATE);
        }
    }

    private static class KeyChangeRequested extends KeyProvided {
        private static final State STATE = new KeyChangeRequested();

        public Object getCreateKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            return provider.promptCreateKey();
        }
    }

    private static class KeyChanged extends KeyProvided {
        private static final State STATE = new KeyChanged();

        public void invalidOpenKey(PromptingKeyProvider provider) {
        }
    }

    private static class Cancelled extends State {
        private static final State STATE = new Cancelled();

        public Object getCreateKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            throw new KeyPromptingCancelledException();
        }

        public Object getOpenKey(PromptingKeyProvider provider)
        throws UnknownKeyException {
            throw new KeyPromptingCancelledException();
        }

        public void invalidOpenKey(PromptingKeyProvider provider) {
        }

        public void resetCancelledPrompt(PromptingKeyProvider provider) {
            provider.reset();
        }
    }

    private static class PromptingLock { }
}
