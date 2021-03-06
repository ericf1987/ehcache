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

package net.sf.ehcache.server.rest.resources;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.server.jaxb.Cache;
import net.sf.ehcache.server.jaxb.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * A CacheManagerResource which permits the following operations:
 * <p/>
 * <code>
 * GET /
 * <p/>
 * Lists the Caches in the CacheManager.
 * </code>
 * <p/>
 * e.g. <code>http://localhost:9090/ehcache/rest/</code>
 *
 * @author Greg Luck
 */
@Path("/")
@Produces("application/xml")
public class CachesResource {

    private static final Logger LOG = LoggerFactory.getLogger(CachesResource.class);

    private static final CacheManager MANAGER;

    static {
        MANAGER = CacheManager.getInstance();
    }


    /**
     * The full URI for the resource.
     * <p/>
     * e.g. <code>//http://localhost:9090/ehcache/rest/testCache</code>
     */
    @Context
    private UriInfo uriInfo;

    /**
     * The HTTP request
     */
    @Context
    private Request request;


    /**
     * Routes the request to a {@link CacheResource} if the path is <code>/ehcache/rest/{cache}</code>
     *
     * @param cache
     * @return
     */
    @Path("{cache}")
    public CacheResource getCacheResource(@PathParam("cache") String cache) {
        return new CacheResource(uriInfo, request, cache);
    }

    /**
     * GET method implementation. Lists the available caches
     *
     * @return
     */
    @GET
    public List<Cache> getCaches() {
        LOG.debug("GET Caches");

        String[] cacheNames = MANAGER.getCacheNames();

        List<Cache> cacheList = new ArrayList<Cache>();

        for (String cacheName : cacheNames) {
            Ehcache ehcache = MANAGER.getCache(cacheName);
            URI cacheUri = uriInfo.getAbsolutePathBuilder().path(cacheName).build().normalize();
            Cache cache = new Cache(cacheName, cacheUri.toString(), ehcache.toString(),
                    new Statistics(ehcache.getStatistics()), ehcache.getCacheConfiguration());
            cacheList.add(cache);
        }

        return cacheList;
    }

}
