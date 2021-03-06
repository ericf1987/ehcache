/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import net.sf.ehcache.store.Store;

public class CacheAccessor {

  private final Cache cache;

  public CacheAccessor(Cache cache) {
    this.cache = cache;
  }

  public Store getStore() {
    return cache.getStore();
  }


  public static CacheAccessor newCacheAccessor(Cache cache) {
    return new CacheAccessor(cache);
  }

}
