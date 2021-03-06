/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * @author cdennis
 */
public class NoTcConfigStandaloneCacheTest extends AbstractCacheTestBase {

  public NoTcConfigStandaloneCacheTest(TestConfig testConfig) {
    super("no-tcconfig-cache-test.xml", testConfig);
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    if ((exitCode == 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

    FileReader fr = null;
    boolean cacheException = false;
    boolean tcConfig = false;
    try {
      fr = new FileReader(output);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains("CacheException")) cacheException = true;
        if (st.contains("<terracottaConfig>")) tcConfig = true;
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
      } catch (Exception e) {
        //
      }
    }

    if (!cacheException) { throw new AssertionError("Expected Exception"); }

    if (!tcConfig) { throw new AssertionError("Expected <terracottaConfig>"); }
  }

}
