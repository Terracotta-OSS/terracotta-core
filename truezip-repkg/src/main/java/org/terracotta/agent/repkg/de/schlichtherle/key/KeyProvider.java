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
 * A general purpose interface used by client applications to retrieve a
 * key which is required to create or open a protected resource.
 * Both the key and the protected resources may be virtually anything:
 * The minimum requirement for a key is just that it's an {@link Object}.
 * Protected resources are not even explicitly modelled in this interface.
 * So in order to use it, an instance must be associated with a protected
 * resource by a third party - this is the job of the {@link KeyManager} class.
 * Because the protected resource is not modelled within this interface,
 * it is at the discretion of the provider implementation whether its
 * instances may or may not be shared among protected resources.
 * If they do, then all associated protected resources share the same key.
 * <p>
 * For the following examples, it helps if you think of the protected
 * resource being an encrypted file and the key being a password.
 * Of course, this interface also works with certificate based encryption.
 * <p>
 * Once an instance has been associated to a protected resource, the client
 * application is assumed to use the key for two basic operations:
 * <ol>
 * <li>A key is required in order to create a new protected resource or
 *     entirely replace its contents.
 *     This implies that the key does not need to be authenticated.
 *     For this purpose, client applications call the method {@link #getCreateKey}.
 * <li>A key is required in order to open an already existing protected
 *     resource for access to its contents.
 *     This implies that the key needs to be authenticated by the client
 *     application.
 *     For this purpose, client applications call the method {@link #getOpenKey},
 *     followed by a call to {@link #invalidOpenKey} if the authentication of
 *     the returned key failed for some reason.
 * </ol>
 * If the same resource is accessed multiple times, these basic operations
 * are guaranteed to return a key which compares {@link Object#equals equal},
 * but is not necessarily the same.
 * In fact, the standard implementations in this package try to return a
 * clone of the key wherever possible for maximum security.
 * Failing that, the same key is returned.
 * <p>
 * From a client application's perspective, the two basic operations may be
 * executed in no particular order. Following are some typical use cases:
 * <ol>
 * <li>A new protected resource needs to be created.
 *     In this case, just the first basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     completely replaced.
 *     Hence there is no need to retrieve and authenticate the existing key.
 *     In this case, just the first basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     read, but not changed.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated.
 *     In this case, just the second basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     read and then only partially updated with new contents.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated.
 *     Because the contents are only partially updated, changing the key
 *     is not possible.
 *     In this case, just the second basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     read and then entirely replaced with new contents.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated before it is probably (at the provider's discretion)
 *     replaced with a new key.
 *     In this case, the second and then the first operation is applied.
 * </ol>
 * As you can see in the last example, it is at the discretion of the key
 * provider whether or not {@link #getCreateKey} returns a key which compares
 * equal to the key returned by {@link #getOpenKey} or returns a completely
 * different (new) key.
 * Ideally, a brave provider implementation would allow the user to control this.
 * In fact, this is the behaviour of the {@link PromptingKeyProvider} in
 * this package and its user interface class(es).
 * <p>
 * Note that provider implementations must be thread safe.
 * This allows clients to use the same provider by multiple threads
 * concurrently.
 *
 * @see KeyManager
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.0
 * @version @version@
 */
public interface KeyProvider {

    /**
     * The minimum delay between subsequent attempts to authenticate a key
     * in milliseconds.
     * More specifically, this is the minimum delay between the call to
     * {@link #invalidOpenKey} and a subsequent {@link #getOpenKey} by the
     * same thread.
     */
    int MIN_KEY_RETRY_DELAY = 3 * 1000;
    
    /**
     * Returns the key which may be used to create a new protected resource or
     * entirely replace the contents of an already existing protected resource.
     * Hence, the key does not need to be authenticated.
     * <p>
     * For each call to this method an object is returned which compares
     * {@link Object#equals equal} to the previously returned object, but is
     * not necessarily the same.
     * 
     * @return A clone of the key object.
     *         If the key does not support cloning or cloning fails for some
     *         reason, the key object itself is returned.
     *         <code>null</code> is never returned.
     *
     * @throws UnknownKeyException If the required key is unknown.
     *         At the provider implementation's discretion, this may mean that
     *         prompting for the key has been disabled or cancelled by the user.
     */
    Object getCreateKey() throws UnknownKeyException;

    /**
     * Returns the key which may be used to open an existing protected resource
     * in order to access its contents.
     * Hence, the key needs to be authenticated.
     * If the authentication fails, {@link #invalidOpenKey} must be called
     * immediately to indicate this situation.
     * <p>
     * Unless {@link #invalidOpenKey} is called, on each call to this method
     * an object is returned which compares {@link Object#equals equal} to
     * the previously returned object, but is not necessarily the same.
     * <p>
     * <b>Important:</b> From a client application's perspective, a
     * <code>KeyProvider</code> is not trustworthy!
     * Hence, the key returned by this method must not only get authenticated,
     * but the client application should also throttle the pace for the
     * return from a subsequent call to this method if the key is invalid
     * in order to protect the client application from an exhaustive search
     * for the correct key.
     * As a rule of thumb, at least three seconds should pass between the
     * immediate call to {@link #invalidOpenKey} and the return from the
     * subsequent call to this method.
     * "Friendly" implementations of this interface should duplicate this
     * behaviour in order to protect client applications which do not obeye
     * these considerations against abuses of the key provider implementation.
     * Note that <code>invalidOpenKey()</code> must still be called
     * immediately by the client application, so that other threads are not
     * negatively affected by the suspension penalty.
     * For the same reason, "friendly" implementations should enforce the
     * suspension penalty for the local thread only.
     * 
     * @return A clone of the key object.
     *         If the key does not support cloning or cloning fails for some
     *         reason, the key object itself is returned.
     *         <code>null</code> is never returned.
     * @throws UnknownKeyException If the required key is unknown.
     *         At the provider implementation's discretion, this may mean that
     *         prompting for the key has been disabled or cancelled by the user.
     * @see #MIN_KEY_RETRY_DELAY
     */
    Object getOpenKey() throws UnknownKeyException;

    /**
     * Called to indicate that authentication of the key returned by
     * {@link #getOpenKey()} has failed and to request an entirely different
     * key.
     * Whether or not an entirely different key is provided on the next call
     * to {@link #getOpenKey} is at the discretion of the provider's
     * implementation and its instance's state.
     */
    void invalidOpenKey();
}
