/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.hibernate.management;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link EhcacheHibernateMBeanRegistration}.
 * Also implements {@link CacheManagerEventListener}. Deregisters mbeans when the associated cachemanager is shutdown.
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public class EhcacheHibernateMBeanRegistrationImpl implements EhcacheHibernateMBeanRegistration, CacheManagerEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheHibernateMBeanRegistrationImpl.class.getName());

    private static final int MAX_MBEAN_REGISTRATION_RETRIES = 50;
    private String cacheManagerClusterUUID;
    private String registeredCacheManagerName;
    private Status status = Status.STATUS_UNINITIALISED;
    private volatile EhcacheHibernate ehcacheHibernate;

    /**
     * {@inheritDoc}
     */
    public synchronized void registerMBeanForCacheManager(final CacheManager manager, final String sessionFactoryName) throws Exception {
        String name = null;
        if (sessionFactoryName == null) {
            name = manager.getName();
        } else {
            name = "".equals(sessionFactoryName.trim()) ? manager.getName() : sessionFactoryName;
        }
        registerBean(name, manager);
    }

    private void registerBean(String name, CacheManager manager) throws Exception {
        ehcacheHibernate = new EhcacheHibernate(manager);
        int tries = 0;
        boolean success = false;
        Exception exception = null;
        cacheManagerClusterUUID = manager.getClusterUUID();
        do {
            this.registeredCacheManagerName = name;
            if (tries != 0) {
                registeredCacheManagerName += "_" + tries;
            }
            try {
                // register the CacheManager MBean
                MBeanServer mBeanServer = getMBeanServer();
                mBeanServer.registerMBean(ehcacheHibernate, EhcacheHibernateMbeanNames.getCacheManagerObjectName(cacheManagerClusterUUID,
                        registeredCacheManagerName));
                success = true;
                break;
            } catch (InstanceAlreadyExistsException e) {
                success = false;
                exception = e;
            }
            tries++;
        } while (tries < MAX_MBEAN_REGISTRATION_RETRIES);
        if (!success) {
            throw new Exception("Cannot register mbean for CacheManager with name" + manager.getName() + " after "
                    + MAX_MBEAN_REGISTRATION_RETRIES + " retries. Last tried name=" + registeredCacheManagerName, exception);
        }
        status = status.STATUS_ALIVE;
    }

    private MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * {@inheritDoc}
     */
    public void enableHibernateStatisticsSupport(SessionFactory sessionFactory) {
        ehcacheHibernate.enableHibernateStatistics(sessionFactory);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose() throws CacheException {
        Set<ObjectName> registeredObjectNames = null;

        try {
            // CacheManager MBean
            registeredObjectNames = getMBeanServer().queryNames(
                    EhcacheHibernateMbeanNames.getCacheManagerObjectName(cacheManagerClusterUUID, registeredCacheManagerName), null);
        } catch (MalformedObjectNameException e) {
            LOG.warn("Error querying MBeanServer. Error was " + e.getMessage(), e);
        }
        if (registeredObjectNames != null) {
            for (ObjectName objectName : registeredObjectNames) {
                try {
                    getMBeanServer().unregisterMBean(objectName);
                } catch (Exception e) {
                    LOG.warn("Error unregistering object instance " + objectName + " . Error was " + e.getMessage(), e);
                }
            }
        }
        status = Status.STATUS_SHUTDOWN;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Status getStatus() {
        return status;
    }

    /**
     * No-op in this case
     */
    public void init() throws CacheException {
        // no-op
    }

    /**
     * No-op in this case
     */
    public void notifyCacheAdded(String cacheName) {
        // no-op
    }

    /**
     * No-op in this case
     */
    public void notifyCacheRemoved(String cacheName) {
        // no-op
    }

}
