package net.sf.ehcache.config;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class CacheConfigurationTest {

    private CacheManager cacheManager;

    @Before
    public void setup() {
        this.cacheManager = CacheManager.getInstance();
    }

    @Test
    public void testDiskStorePath() {

        String name = "testTemp";
        String path = "c:\\something\\temp";

        CacheConfiguration cacheConfiguration = new CacheConfiguration(name, 0)
            .diskStorePath(path)
            .diskPersistent(true);
        cacheManager.addCache(new Cache(cacheConfiguration));
        Cache cache = cacheManager.getCache(name);
        assertThat(getDiskStorePath(cache), equalTo(path));
        cache.put(new Element("KEY", "VALUE"));
    }

    @Test
    public void testTransactionalMode() {
        CacheConfiguration configuration = new CacheConfiguration();
        assertEquals(CacheConfiguration.TransactionalMode.OFF, configuration.getTransactionalMode());
        try {
            configuration.setTransactionalMode(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        configuration.setTransactionalMode("local");
        assertEquals(CacheConfiguration.TransactionalMode.LOCAL, configuration.getTransactionalMode());
        try {
            configuration.transactionalMode(CacheConfiguration.TransactionalMode.OFF);
            fail("expected InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            // expected
        }
        try {
            configuration.setTransactionalMode(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        CacheConfiguration clone = configuration.clone();
        assertEquals(CacheConfiguration.TransactionalMode.LOCAL, clone.getTransactionalMode());
        try {
            clone.transactionalMode(CacheConfiguration.TransactionalMode.XA);
            fail("expected InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            // expected
        }
    }

    @Test
    public void testReadPercentageProperly() {
        CacheConfiguration configuration = new CacheConfiguration();
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), nullValue());
        configuration.setMaxBytesLocalOffHeap("12%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(12));
        configuration.setMaxBytesLocalOffHeap("99%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(99));
        configuration.setMaxBytesLocalOffHeap("100%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(100));
        configuration.setMaxBytesLocalOffHeap("0%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(0));
        try {
            configuration.setMaxBytesLocalOffHeap("101%");
            fail("This should throw an IllegalArgumentException, 101% is above 100%");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            configuration.setMaxBytesLocalOffHeap("-10%");
            fail("This should throw an IllegalArgumentException, -10% is below 0%");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testCanSetBothMaxWhenCacheNotRunning() {
        CacheConfiguration configuration = new CacheConfiguration();
        try {
            configuration.setMaxEntriesLocalHeap(10);
            configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES);
            configuration.setMaxEntriesLocalDisk(10);
            configuration.maxBytesLocalDisk(10, MemoryUnit.MEGABYTES);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            fail("Shouldn't have thrown an exception");
        }
    }

    @Test
    public void testMaxEntriesLocalDiskAndMaxElementsOnDiskAlias() {
        CacheConfiguration configuration = new CacheConfiguration().maxElementsOnDisk(10);
        assertThat(configuration.getMaxEntriesLocalDisk(), is(10L));
        assertThat(configuration.getMaxElementsOnDisk(), is(10));
        configuration.maxEntriesLocalDisk(20);
        assertThat(configuration.getMaxEntriesLocalDisk(), is(20L));
        assertThat(configuration.getMaxElementsOnDisk(), is(20));
    }

    @Test
    public void testCantSetMaxEntriesLocalDiskWhenClustered() {
        CacheConfiguration configuration = new CacheConfiguration("Test", 10)
            .maxEntriesLocalDisk(10).terracotta(new TerracottaConfiguration());
        try {
            cacheManager.addCache(new Cache(configuration));
            fail("This should throw InvalidConfigurationException");
        } catch (CacheException e) {
            assertThat(e.getMessage().contains("use maxElementsOnDisk instead"), is(true));
        }
    }

    private String getDiskStorePath(final Cache cache) {
        try {
            Field declaredField = Cache.class.getDeclaredField("diskStorePath");
            declaredField.setAccessible(true);
            return (String)declaredField.get(cache);
        } catch (Exception e) {
            throw new RuntimeException("Did you rename Cache.diskStorePath ?!", e);
        }
    }
}
