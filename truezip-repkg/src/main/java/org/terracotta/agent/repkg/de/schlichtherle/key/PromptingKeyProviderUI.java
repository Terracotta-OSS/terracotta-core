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
 * Used by {@link PromptingKeyProvider}s for the actual prompting of the user
 * for a key (a password for example) which is required to access a protected
 * resource.
 * This interface is not depending on any particular user interface techology,
 * so prompting could be implemented using Swing, the console, a web page
 * or any other user interface technology.
 * <p>
 * Implementations of this interface are instantiated and maintained by the
 * {@link PromptingKeyManager} and are shared between different
 * {@link PromptingKeyProvider} instances.
 * Hence, implementations of this interface <em>must</em> be thread safe
 * and should have no side effects!
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public interface PromptingKeyProviderUI {

    /**
     * Prompts the user for the key which may be used to create a new
     * protected resource or entirely replace the contents of an already
     * existing protected resource.
     * <p>
     * Upon return, the implementation is expected to update the common key
     * in <code>provider</code>.
     * Upon return, if <code>provider.getKey()</code> returns <code>null</code>,
     * prompting for the key is assumed to have been cancelled by the user.
     * In this case, the current and each subsequent call to
     * {@link KeyProvider#getOpenKey} or {@link KeyProvider#getCreateKey}
     * by the client results in an {@link UnknownKeyException} and the user
     * is not prompted anymore until the provider is reset by the
     * {@link KeyManager}.
     * Otherwise, the key is used as the common key, a clone of which is
     * provided to the client upon request.
     * <p>
     * <b>Hint:</b> If the user cancels the dialog, it is recommended to
     * leave the provider's <code>key</code> property simply unmodified.
     * This causes the old key to be reused and allows the client to
     * continue its operation as if the user would not have requested to
     * change the key.
     * <p>
     * Since TrueZIP 6.4, an implementation may also throw a
     * {@link RuntimeException} with any kind of {@link UnknownKeyException}
     * as its cause.
     * This will trigger the calling method in the
     * <code>PromptingKeyProvider</code> class to unwrap and pass on the
     * cause without changing its state.
     * This may be useful if prompting was interrupted by a call to
     * {@link Thread#interrupt} while waiting on the Event Dispatch Thread.
     * In this case, another try to prompt the user should have the chance to
     * succeed instead of being cancelled without actually prompting the user
     * again.
     * To trigger this behaviour, the implementation should simply throw any
     * kind of <code>RuntimeException</code> with a
     * {@link KeyPromptingInterruptedException} as its cause.
     * 
     * @param provider The default key provider to store the result in.
     * @throws RuntimeException with an {@link UnknownKeyException} as its
     *         cause if the implementation does not want the key provider's
     *         state to be changed.
     */
    // TODO: Add UnknownKeyException to the signature of this method in TrueZIP 7.
    void promptCreateKey(PromptingKeyProvider provider);

    /**
     * Prompts the user for the key which may be used to open an existing
     * protected resource in order to access its contents.
     * <p>
     * Upon return, the implementation is expected to update the common key
     * in <code>provider</code>.
     * Upon return, if <code>provider.getKey()</code> returns <code>null</code>,
     * prompting for the key is assumed to have been cancelled by the user.
     * In this case, the current and each subsequent call to
     * {@link KeyProvider#getOpenKey} or {@link KeyProvider#getCreateKey}
     * by the client results in an {@link UnknownKeyException} and the user
     * is not prompted anymore until the provider is reset by the
     * {@link KeyManager}.
     * Otherwise, the key is used as the common key, a clone of which is
     * provided to the client upon request.
     * <p>
     * Since TrueZIP 6.4, an implementation may also throw a
     * {@link RuntimeException} with any kind of {@link UnknownKeyException}
     * as its cause.
     * This will trigger the calling method in the
     * <code>PromptingKeyProvider</code> class to unwrap and pass on the
     * cause without changing its state.
     * This may be useful if prompting was interrupted by a call to
     * {@link Thread#interrupt} while waiting on the Event Dispatch Thread.
     * In this case, another try to prompt the user should have the chance to
     * succeed instead of being cancelled without actually prompting the user
     * again.
     * To trigger this behaviour, the implementation should simply throw any
     * kind of <code>RuntimeException</code> with a
     * {@link KeyPromptingInterruptedException} as its cause.
     * 
     * @param provider The key provider to store the result in.
     * @return <code>true</code> if the user has requested to change the
     *         provided key.
     * @throws RuntimeException with an {@link UnknownKeyException} as its
     *         cause if the implementation does not want the key provider's
     *         state to be changed.
     */
    // TODO: Add UnknownKeyException to the signature of this method in TrueZIP 7.
    boolean promptUnknownOpenKey(PromptingKeyProvider provider);

    /**
     * Prompts the user for the key which may be used to open an existing
     * protected resource in order to access its contents.
     * This is called if the key returned by a previous call to
     * {@link #promptUnknownOpenKey} is invalid.
     * <p>
     * Upon return, the implementation is expected to update the common key
     * in <code>provider</code>.
     * Upon return, if <code>provider.getKey()</code> returns <code>null</code>,
     * prompting for the key is assumed to have been cancelled by the user.
     * In this case, the current and each subsequent call to
     * {@link KeyProvider#getOpenKey} or {@link KeyProvider#getCreateKey}
     * by the client results in an {@link UnknownKeyException} and the user
     * is not prompted anymore until the provider is reset by the
     * {@link KeyManager}.
     * Otherwise, the key is used as the common key, a clone of which is
     * provided to the client upon request.
     * <p>
     * Since TrueZIP 6.4, an implementation may also throw a
     * {@link RuntimeException} with any kind of {@link UnknownKeyException}
     * as its cause.
     * This will trigger the calling method in the
     * <code>PromptingKeyProvider</code> class to unwrap and pass on the
     * cause without changing its state.
     * This may be useful if prompting was interrupted by a call to
     * {@link Thread#interrupt} while waiting on the Event Dispatch Thread.
     * In this case, another try to prompt the user should have the chance to
     * succeed instead of being cancelled without actually prompting the user
     * again.
     * To trigger this behaviour, the implementation should simply throw any
     * kind of <code>RuntimeException</code> with a
     * {@link KeyPromptingInterruptedException} as its cause.
     * 
     * @param provider The key provider to store the result in.
     * @return <code>true</code> if the user has requested to change the
     *         provided key.
     * @throws RuntimeException with an {@link UnknownKeyException} as its
     *         cause if the implementation does not want the key provider's
     *         state to be changed.
     */
    // TODO: Add UnknownKeyException to the signature of this method in TrueZIP 7.
    boolean promptInvalidOpenKey(PromptingKeyProvider provider);
}
