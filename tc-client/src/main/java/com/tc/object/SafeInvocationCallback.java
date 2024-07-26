/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.InvocationCallback;

public interface SafeInvocationCallback<R> extends InvocationCallback<R> {

    static <R> SafeInvocationCallback<R> safe(InvocationCallback<R> callback) {
        if (callback instanceof SafeInvocationCallback<?>) {
            return (SafeInvocationCallback<R>) callback;
        } else {
            return new Guard<>(callback);
        }
    }

    class Guard<R> implements SafeInvocationCallback<R> {
        private static final Logger LOGGER = LoggerFactory.getLogger(SafeInvocationCallback.class);

        private final InvocationCallback<R> untrustedCallback;

        private Guard(InvocationCallback<R> untrustedCallback) {
            this.untrustedCallback = untrustedCallback;
        }

        @Override
        public void sent() {
            try {
                untrustedCallback.sent();
            } catch (Exception t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw exception", t);
            } catch (Throwable t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw throwable", t);
            }
        }

        @Override
        public void received() {
            try {
                untrustedCallback.received();
            } catch (Exception t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw exception", t);
            } catch (Throwable t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw throwable", t);
            }
        }

        @Override
        public void result(R response) {
            try {
                untrustedCallback.result(response);
            } catch (Exception t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw exception", t);
            } catch (Throwable t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw throwable", t);
            }
        }

        @Override
        public void failure(Throwable failure) {
            try {
                untrustedCallback.failure(failure);
            } catch (Exception t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw exception", t);
            } catch (Throwable t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw throwable", t);
            }
        }

        @Override
        public void complete() {
            try {
                untrustedCallback.complete();
            } catch (Exception t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw exception", t);
            } catch (Throwable t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw throwable", t);
            }
        }

        @Override
        public void retired() {
            try {
                untrustedCallback.retired();
            } catch (Exception t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw exception", t);
            } catch (Throwable t) {
                LOGGER.warn("User-provided callback [" + untrustedCallback + "] threw throwable", t);
            }
        }
    }
}
