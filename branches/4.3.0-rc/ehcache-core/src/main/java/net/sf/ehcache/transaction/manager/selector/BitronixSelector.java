/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.transaction.manager.selector;

import net.sf.ehcache.transaction.xa.EhcacheXAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

/**
 * A Selector for the Bitronix Transaction Manager.
 *
 * @author Ludovic Orban
 */
public class BitronixSelector extends FactorySelector {
    private static final Logger LOG = LoggerFactory.getLogger(BitronixSelector.class);

    /**
     * Constructor
     */
    public BitronixSelector() {
        super("Bitronix", "bitronix.tm.TransactionManagerServices", "getTransactionManager");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerResource(EhcacheXAResource ehcacheXAResource, boolean forRecovery) {
        String uniqueName = ehcacheXAResource.getCacheName();
        try {

            Class producerClass = Class.forName("bitronix.tm.resource.ehcache.EhCacheXAResourceProducer");

            Class[] signature = new Class[] {String.class, XAResource.class};
            Object[] args = new Object[] {uniqueName, ehcacheXAResource};
            Method method = producerClass.getMethod("registerXAResource", signature);
            method.invoke(null, args);
        } catch (Exception e) {
            LOG.error("unable to register resource of cache " + uniqueName + " with BTM", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterResource(EhcacheXAResource ehcacheXAResource, boolean forRecovery) {
        String uniqueName = ehcacheXAResource.getCacheName();
        try {
            Class producerClass = Class.forName("bitronix.tm.resource.ehcache.EhCacheXAResourceProducer");

            Class[] signature = new Class[] {String.class, XAResource.class};
            Object[] args = new Object[] {uniqueName, ehcacheXAResource};
            Method method = producerClass.getMethod("unregisterXAResource", signature);
            method.invoke(null, args);
        } catch (Exception e) {
            LOG.error("unable to unregister resource of cache " + uniqueName + " with BTM", e);
        }
    }
}
