/**
 * Tests for the Ehcache WebService
 * @author Greg Luck
 * @version $Id$
 */
package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.soap.jaxws.*;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import net.sf.ehcache.server.soap.jaxws.Status;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Before;
import static org.junit.Assert.*;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;

public class EhcacheWebServiceEndpointTest {
    private static EhcacheWebServiceEndpoint cacheService;
    private String cacheName = "sampleCache1";

    @BeforeClass
    public static void setup() {
        cacheService = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();
    }

    @Before
    public void zeroOutCache() throws CacheException_Exception, IllegalStateException_Exception {
        cacheService.removeAll(cacheName);
    }

    @Test
    public void testPing() {
        //invoke business method
        String result = cacheService.ping();
        assertEquals("pong", result);
    }

    @Test
    public void testGetCache() throws CacheException_Exception, NoSuchCacheException_Exception {
        Cache cache = cacheService.getCache("doesnotexist");
        assertNull(cache);

        cache = cacheService.getCache("sampleCache1");
        assertEquals("sampleCache1", cache.getName());
        assertEquals("rest/sampleCache1", cache.getUri());
        assertTrue(cache.getDescription().indexOf("sampleCache1") != -1);
    }

    @Test
    public void testAddCache() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, ObjectExistsException_Exception {

        cacheService.addCache("newcache1");
        Cache cache = cacheService.getCache("newcache1");
        assertNotNull(cache);

        try {
            cacheService.addCache("newcache1");
        } catch (SOAPFaultException e) {
            //expected
            assertTrue(e.getCause().getMessage().indexOf("Cache newcache1 already exists") != -1);
        }
    }

    @Test
    public void testRemoveCache() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, ObjectExistsException_Exception {

        cacheService.addCache("newcache2");
        Cache cache = cacheService.getCache("newcache2");
        assertNotNull(cache);

        cacheService.removeCache("newcache2");
        cache = cacheService.getCache("newcache2");
        assertNull(cache);

        //should not throw an exception
        cacheService.removeCache("newcache2");
        cache = cacheService.getCache("newcache2");
        assertNull(cache);
    }

    /**
     * Gets the cache names
     */
    @Test
    public void testCacheNames() throws IllegalStateException_Exception {
        List cacheNames = cacheService.cacheNames();
        //Other tests add caches to the CacheManager
        assertTrue(cacheNames.size() >= 6);
    }

    @Test
    public void testCacheStatus() throws CacheException_Exception, NoSuchCacheException_Exception {
        Status status = cacheService.getStatus("sampleCache1");
        assertTrue(status == Status.STATUS_ALIVE);
    }


    @Test
    public void testCachePutNull() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception {

        Element element = new Element();
        element.setKey("1");
        cacheService.put("sampleCache1", element);

        element = cacheService.get("sampleCache1", "1");
        boolean equals = Arrays.equals(null, element.getValue());
        assertTrue(equals);
    }

    /**
     * Tests get, getQuiet and put, putQuiet
     */
    @Test
    public void testCacheGetPut() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, IOException, IllegalArgumentException_Exception {

        Element element = new Element();
        element.setKey("2");
        byte[] bytes1 = new byte[]{1, 2, 3, 4, 5, 6};
        element.setValue(bytes1);

        cacheService.put("sampleCache1", element);
        element = cacheService.get("sampleCache1", "2");
        byte[] bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
        cacheService.remove("sampleCache1", "2");

        cacheService.putQuiet("sampleCache1", element);
        element = cacheService.get("sampleCache1", "2");
        bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
        cacheService.remove("sampleCache1", "2");

        cacheService.put("sampleCache1", element);
        element = cacheService.getQuiet("sampleCache1", "2");
        bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
        cacheService.remove("sampleCache1", "2");
    }

    //todo expired element
    //todo setting various eternal, ttl, tti

    /**
     * Test getKeys() and its veriants
     */
    @Test
    public void testGetKeys() throws NoSuchCacheException_Exception, CacheException_Exception, IllegalStateException_Exception {

        for (int i = 0; i < 1000; i++) {
            Element element = new Element();
            element.setKey(i);
            element.setValue(("value" + i).getBytes());

            cacheService.put("sampleCache1", element);
        }

        List keys = cacheService.getKeys("sampleCache1");
        assertEquals(1000, keys.size());

        keys = cacheService.getKeysWithExpiryCheck("sampleCache1");
        assertEquals(1000, keys.size());

        keys = cacheService.getKeysNoDuplicateCheck("sampleCache1");
        assertEquals(1000, keys.size());

    }


    @Test
    public void testRemove() throws NoSuchCacheException_Exception, CacheException_Exception, IllegalStateException_Exception {

        Element element = new Element();
        element.setKey("1");
        element.setValue(("value1").getBytes());
        cacheService.put("sampleCache1", element);

        assertEquals(1, cacheService.getSize("sampleCache1"));
    }


}