/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
 */
package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;

import java.util.Set;

/**
 * A factory for {@link SoftLock}s
 * 
 * @author Ludovic Orban
 */
public interface SoftLockFactory {

    /**
     * Create a new, unlocked soft lock
     * @param transactionID the transaction ID under which the soft lock will operate
     * @param key the key of the Element this soft lock is protecting
     * @param newElement the new Element
     * @param oldElement the actual Element
     * @return the soft lock
     */
    SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement);

    /**
     * Get a Set of keys protected by soft locks which must not be visible to a transaction context
     * according to the isolation level.
     * @param transactionContext the transaction context
     * @return a Set of keys invisible to the context
     */
    Set<Object> getKeysInvisibleInContext(TransactionContext transactionContext);

}
