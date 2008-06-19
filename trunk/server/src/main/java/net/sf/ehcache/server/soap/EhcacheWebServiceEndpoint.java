/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.server.soap;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.server.ServerContext;
import net.sf.ehcache.server.jaxb.Cache;
import net.sf.ehcache.server.jaxb.Element;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * The Ehcache WebService provides selected CacheManager and Cache methods
 * <p/>
 * In JAX-WS RI 2.1.3 a service endpoint interface is no longer required therefore this class does
 * not implement an interface.
 * <p/>
 * Portable artifacts generated by JAX-WS RI 2.1.3 include zero or more JavaBean classes to aide in
 * the marshaling of method invocations and responses, as well as service-specific exceptions. The
 * portable artifacts be generated from source using apt.
 * <p/>
 * In document/literal wrapped mode, two JavaBeans are generated for each operation in the web service.
 * One bean is for invoking the other for the response. In all modes (rpc/literal and both document/literal modes),
 * one JavaBean is generated for each service-specific exception. All method parameters and return types
 * must be compatible with the JAXB 2.0 Java to XML Schema mapping definition.
 * <p/>
 * Keys must be of type String in this WebService API even though Ehcache can
 * use any Java Object as a key. The consequences are that only keys which are Strings are accessible
 * from this API.
 * <p/>
 * Element Representations are put into and returned from the cache. An Element Representation contains
 * the key, value, which must be a byte[], the value's MIME Type and metadata about the underlying Element.
 * See {@link Element} for details.
 * <p/>
 * Values are always byte arrays. The type of the byte[] is provided by the
 * MIME type. See {@link net.sf.ehcache.server.jaxb.Element#getMimeType}
 * <p/>
 * @author Greg Luck
 * @version $Id$
 */
@WebService()
public class EhcacheWebServiceEndpoint {

    private CacheManager manager = ServerContext.getCacheManager();

    @Resource
    private WebServiceContext context;


    /**
     * This method provides a noop monitoring WebMethod, which can be used for monitoring.
     *
     * @return "pong"
     */
    @WebMethod
    public String ping() {
        return "pong";
    }

    /**
     * Gets the user Principal
     *
     * @return the user principal
     */
    private Principal getUserPrincipal() {
        return context.getUserPrincipal();
    }

    /**
     * Gets a Cache representation. This call does not throw an exception if the cache is not found.
     * Most other methods in this endpoint do.
     * <p/>
     * @param cacheName the name of the cache to perform this operation on.
     * @return a representation of a Cache, if an object of type Cache exists by that name, else null
     * @throws net.sf.ehcache.CacheException if something goes wrong in the Ehcache core infrastructure
     */
    @WebMethod
    public Cache getCache(String cacheName) throws CacheException {

        Ehcache ehcache = manager.getCache(cacheName);
        if (ehcache != null) {
            return new Cache(ehcache);
        } else {
            return null;
        }
    }


    /**
     * Adds a {@link net.sf.ehcache.Ehcache} based on the defaultCache with the given name.
     * <p/>
     * Memory and Disk stores will be configured for it and it will be added
     * to the map of caches.
     * <p/>
     * Also notifies the CacheManagerEventListener after the cache was initialised and added.
     * <p/>
     * It will be created with the defaultCache attributes specified in ehcache.xml
     *
     * @param cacheName the name of the cache to perform this operation on. the name for the cache
     * @throws net.sf.ehcache.ObjectExistsException if the cache already exists.
     *                                       if the cache already exists
     * @throws net.sf.ehcache.CacheException if there was an error creating the cache.
     * @throws IllegalStateException         if the CacheManager is not alive
     */
    @WebMethod
    public void addCache(String cacheName) throws IllegalStateException, ObjectExistsException, CacheException {
        manager.addCache(cacheName);
    }


    /**
     * Remove a cache from the CacheManager. The cache is disposed of.
     * <p/>
     * This method can be called multiple times. If the cache does not exist no exception is thrown.
     * @param cacheName the name of the cache to perform this operation on. the cache name
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    @WebMethod
    public void removeCache(String cacheName) throws IllegalStateException {
        manager.removeCache(cacheName);
    }


    /**
     * Returns a list of the current cache names.
     *
     * @return an array of {@link String}s
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    @WebMethod
    public String[] cacheNames() throws IllegalStateException {
        return manager.getCacheNames();
    }


    /**
     * Gets the status attribute of the Ehcache
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @return The status value from the Status enum class
     */
    @WebMethod
    public net.sf.ehcache.server.soap.Status getStatus(String cacheName) {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return Status.fromCode(cache.getStatus().intValue());
    }


    /**
     * Put an element in the cache.
     * <p/>
     * Resets the access statistics on the element, which would be the case if it has previously been
     * gotten from a cache, and is now being put back.
     * <p/>
     * Also notifies the CacheEventListener that:
     * <ul>
     * <li>the element was put, but only if the Element was actually put.
     * <li>if the element exists in the cache, that an update has occurred, even if the element would be expired
     * if it was requested
     * </ul>
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param element   An object.
     * @throws net.sf.ehcache.CacheException if a general cache exception occurs
     * @throws NoSuchCacheException          if a cache named cacheName does not exist.
     */
    @WebMethod
    public void put(String cacheName, Element element) throws NoSuchCacheException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        cache.put(element.getEhcacheElement());
    }

    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(String, Object)} to peak into the Element to see its last access time with get
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param key       keys must be String in this WebServices API
     * @return the element, or null, if it does not exist.
     * @throws net.sf.ehcache.CacheException if a general cache exception occurs
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws NoSuchCacheException  if a cache named cacheName does not exist.
     */
    @WebMethod
    public Element get(String cacheName, String key) throws IllegalStateException, NoSuchCacheException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        net.sf.ehcache.Element element = cache.get(key);
        if (element != null) {
            return new Element(element);
        } else {
            return null;
        }
    }

    /**
     * Returns a list of all elements in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n). On a single cpu 1.8Ghz P4, approximately 8ms is required
     * for each 1000 entries.
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @return a list of {@link Object} keys
     * @throws net.sf.ehcache.CacheException if an Ehcache core exception occurs
     * @throws IllegalStateException if the cache is not alive
     */
    @WebMethod
    public List getKeys(String cacheName) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.getKeys();
    }

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed.
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param key keys must be Strings in this WebServices API
     * @return true if the element was removed, false if it was not found in the cache
     * @throws net.sf.ehcache.CacheException if an Ehcache core exception occurs
     */
    @WebMethod
    public boolean remove(String cacheName, String key) throws CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.remove(key);
    }

    /**
     * Returns a list of all elements in the cache. Only keys of non-expired
     * elements are returned.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n), where n is the number of elements in the cache. On
     * a 1.8Ghz P4, the time taken is approximately 200ms per 1000 entries. This method
     * is not synchronized, because it relies on a non-live list returned from {@link #getKeys(String cacheName)}
     * , which is synchronised, and which takes 8ms per 1000 entries. This way
     * cache liveness is preserved, even if this method is very slow to return.
     * <p/>
     * Consider whether your usage requires checking for expired keys. Because
     * this method takes so long, depending on cache settings, the list could be
     * quite out of date by the time you get it.
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws net.sf.ehcache.CacheException if something goes wrong in the underlying Ehcache core
     */
    @WebMethod
    public List getKeysWithExpiryCheck(String cacheName) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.getKeysWithExpiryCheck();
    }

    /**
     * Returns a list of all elements in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are not unique and may contain duplicates. If the cache is only
     * using the memory store, the list will be unique. If the disk store is being used
     * as well, it will likely contain duplicates, because of the internal store design.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(log n). On a single cpu 1.8Ghz P4, approximately 6ms is required
     * for 1000 entries and 36 for 50000.
     * <p/>
     * This is the fastest getKeys method
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws net.sf.ehcache.CacheException if something goes wrong in the underlying Ehcache core
     */
    @WebMethod
    public List getKeysNoDuplicateCheck(String cacheName) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.getKeysNoDuplicateCheck();
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * <p/>
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param key       a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException         if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws net.sf.ehcache.CacheException if something goes wrong in the underlying Ehcache core
     */
    @WebMethod
    public Element getQuiet(String cacheName, Object key) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        net.sf.ehcache.Element element = cache.getQuiet(key);
        return new Element(element);
    }

    /**
     * Put an element in the cache, without updating statistics, or updating listeners. This is meant to be used
     * in conjunction with {@link #getQuiet}
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param element   An object. Any element that can be passed in must be <code>Serializable</code> and can therefore fully participate
     *                  in distributed caching replication and the DiskStore.
     * @throws IllegalStateException         if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException      if the element is null
     * @throws net.sf.ehcache.CacheException if something goes wrong in the underlying Ehcache core
     */
    @WebMethod
    public void putQuiet(String cacheName, Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        cache.putQuiet(element.getEhcacheElement());
    }

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     * @param cacheName the name of the cache to perform this operation on.
     * @param key the cache key. This must be a String in this WebService API even though Ehcache can
     * use any Java Object or literal as a key.
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws net.sf.ehcache.CacheException if an exception happens in Ehcache core.
     */
    @WebMethod
    public boolean removeQuiet(String cacheName, String key) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.remove(key);
    }

    /**
     * Removes all cached items.
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @throws IllegalStateException         if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws net.sf.ehcache.CacheException if an exception happens in Ehcache core.
     */
    @WebMethod
    public void removeAll(String cacheName) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        cache.removeAll();

    }

    /**
     * Gets the size of the cache. This is a subtle concept. See below.
     * <p/>
     * The size is the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This number is the actual number of elements, including expired elements that have
     * not been removed.
     * <p/>
     * Expired elements are removed from the the memory store when
     * getting an expired element, or when attempting to spool an expired element to
     * disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired element,
     * or when the expiry thread runs, which is once every five minutes.
     * <p/>
     * To get an exact size, which would exclude expired elements, use {@link #getKeysWithExpiryCheck(String)}.size(),
     * although see that method for the approximate time that would take.
     * <p/>
     * To get a very fast result, use {@link #getKeysNoDuplicateCheck(String)}. If the disk store
     * is being used, there will be some duplicates.
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @return The size value
     * @throws IllegalStateException         if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws net.sf.ehcache.CacheException if an exception happens in Ehcache core.
     */
    @WebMethod
    public int getSize(String cacheName) throws IllegalStateException, CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.getSize();
    }

    /**
     * Gets an immutable Statistics object representing the Cache statistics at the time. How the statistics are calculated
     * depends on the statistics accuracy setting. The only aspect of statistics sensitive to the accuracy setting is
     * object size. How that is calculated is discussed below.
     * <h3>Best Effort Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     * <p/>
     * The size is the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This number is the actual number of elements, including expired elements that have
     * not been removed. Any duplicates between stores are accounted for.
     * <p/>
     * Expired elements are removed from the the memory store when
     * getting an expired element, or when attempting to spool an expired element to
     * disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired element,
     * or when the expiry thread runs, which is once every five minutes.
     * <p/>
     * <h3>Guaranteed Accuracy Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}.
     * <p/>
     * The number given may contain expired elements. In addition if the DiskStore is used it may contain some double
     * counting of elements. It takes 6ms for 1000 elements to execute. Time to execute is O(log n). 50,000 elements take
     * 36ms.
     *
     * @return the number of elements in the ehcache, with a varying degree of accuracy, depending on accuracy setting.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
//    @WebMethod
//    public Statistics getStatistics(String cacheName) throws IllegalStateException {
//        net.sf.ehcache.Cache cache = lookupCache(cacheName);
//        return cache.getStatistics();
//    }

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @param cacheName the name of the cache to perform this operation on. the name of the cache to perform this operation on.
     * @return one of {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}
     */
    @WebMethod
    public int getStatisticsAccuracy(String cacheName) {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        return cache.getStatisticsAccuracy();
    }

    /**
     * Resets statistics counters back to 0.
     *
     * @param cacheName the name of the cache to perform this operation on.
     */
    @WebMethod
    public void clearStatistics(String cacheName) {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        cache.clearStatistics();
    }

    /**
     * Warning: This method is related to the JSR107 specification, which is in draft. It is subject to change without notice.
     * <p/>
     * The load method provides a means to "pre load" the cache. This method will, asynchronously, load the specified
     * object into the cache using the associated cacheloader. If the object already exists in the cache, no action is
     * taken. If no loader is associated with the object, no object will be loaded into the cache. If a problem is
     * encountered during the retrieving or loading of the object, an exception should be logged. If the "arg" argument
     * is set, the arg object will be passed to the CacheLoader.load method. The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the load method. The storing of null values in the cache
     * is permitted, however, the get method will not distinguish returning a null stored in the cache and not finding
     * the object in the cache. In both cases a null is returned.
     * <p/>
     * The Ehcache native API provides similar functionality to loaders using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param key       key whose associated value to be loaded using the associated cacheloader if this cache doesn't contain it.
     * @throws net.sf.ehcache.CacheException if something goes wrong in the underlying Ehcache core
     */
    @WebMethod
    public void load(String cacheName, String key) throws CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        cache.load(key);
    }

    /**
     * Warning: This method is related to the JSR107 specification, which is in draft. It is subject to change without notice.
     * <p/>
     * The loadAll method provides a means to "pre load" objects into the cache. This method will, asynchronously, load
     * the specified objects into the cache using the default cache loader. If the an object already exists in the
     * cache, no action is taken. If no loader is associated with the object, no object will be loaded into the cache.
     * If a problem is encountered during the retrieving or loading of the objects, an exception (to be defined)
     * should be logged. The getAll method will return, from the cache, a Map of the objects associated with the
     * Collection of keys in argument "keys". If the objects are not in the cache, the associated cache loader will be
     * called. If no loader is associated with an object, a null is returned. If a problem is encountered during the
     * retrieving or loading of the objects, an exception (to be defined) will be thrown. If the "arg" argument is set,
     * the arg object will be passed to the CacheLoader.loadAll method. The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the loadAll method.
     * <p/>
     * keys - collection of the keys whose associated values to be loaded into this cache by using the associated
     * cacheloader if this cache doesn't contain them.
     * <p/>
     * The Ehcache native API provides similar functionality to loaders using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param keys  a list of keys which must be String in this WebServices API
     * @throws net.sf.ehcache.CacheException a general exception in the underlying Ehcache cache
     */
    @WebMethod
    public void loadAll(String cacheName, Collection<String> keys) throws CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        cache.loadAll(keys, null);
    }

    /**
     * Warning: This method is related to the JSR107 specification, which is in draft. It is subject to change without notice.
     * <p/>
     * The getAll method will return, from the cache, a Map of the objects associated with the Collection of keys in argument "keys".
     * If the objects are not in the cache, the default cache loader will be called. If no loader is associated with an object,
     * a null is returned. If a problem is encountered during the retrieving or loading of the objects, an exception will be thrown.
     * If the "arg" argument is set, the arg object will be passed to the CacheLoader.loadAll method. The cache will not dereference
     * the object. If no "arg" value is provided a null will be passed to the loadAll method. The storing of null values in the cache
     * is permitted, however, the get method will not distinguish returning a null stored in the cache and not finding the object in
     * the cache. In both cases a null is returned.
     * <p/>
     * <p/>
     * Note. If the getAll exceeds the maximum cache size, the returned map will necessarily be less than the number specified.
     * <p/>
     * Because this method may take a long time to complete, it is not synchronized. The underlying cache operations
     * are synchronized.
     * <p/>
     * The constructs package provides similar functionality using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param keys      a collection of keys to be returned/loaded
     * @return a HashMap populated from the Cache. If there are no elements, an empty Map is returned.
     * @throws net.sf.ehcache.CacheException if an Ehcache core exception occurs
     */
    @WebMethod
    public HashMap getAllWithLoader(String cacheName, Collection keys) throws CacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        //this is a little awkward. If the Ehcache implementation changes this will break.
        return (HashMap) cache.getAllWithLoader(keys, null);
    }

    /**
     * Warning: This method is related to the JSR107 specification, which is in draft. It is subject to change without notice.
     * <p/>
     * This method will return, from the cache, the object associated with
     * the argument "key".
     * <p/>
     * If the object is not in the cache, the default
     * cache loader will be called. That is either the CacheLoader passed in, or if null, the one associated with the cache.
     * If both are null, no load is performed and null is returned.
     * <p/>
     * Because this method may take a long time to complete, it is not synchronized. The underlying cache operations
     * are synchronized.
     *
     * @param cacheName the name of the cache to perform this operation on.
     * @param key       key whose associated value is to be returned.
     * @return an element if it existed or could be loaded, otherwise null
     * @throws NoSuchCacheException if the cacheName is not found
     */
    @WebMethod
    public Element getWithLoader(String cacheName, String key) throws NoSuchCacheException {
        net.sf.ehcache.Cache cache = lookupCache(cacheName);
        net.sf.ehcache.Element element = cache.getWithLoader(key, null, null);
        return new Element(element);
    }

    private net.sf.ehcache.Cache lookupCache(String cacheName) throws NoSuchCacheException {
        net.sf.ehcache.Cache cache = manager.getCache(cacheName);
        if (cache == null) {
            throw new NoSuchCacheException(MessageFormat.format("The cache named {0} does not exist.", cacheName));
        }
        return cache;
    }
}