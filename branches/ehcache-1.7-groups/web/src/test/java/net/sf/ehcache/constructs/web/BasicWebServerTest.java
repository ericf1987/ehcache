/**
 *  Copyright 2003-2009 Luck Consulting Pty Ltd
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

package net.sf.ehcache.constructs.web;

import com.meterware.httpunit.WebResponse;
import org.junit.Test;

/**
 * Tests that the test Orion server is properly installed and running.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: BasicWebServerTest.java 796 2008-10-09 02:39:03Z gregluck $
 */
public class BasicWebServerTest extends AbstractWebTest {

    /**
     * Tests Orion supplied page
     */
    @Test
    public void testOrionDefaultPage() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/index.html");
        assertResponseOk(response);
    }

    /**
     * Tests that we have enough of Orion to use a JSP
     */
    @Test
    public void testBasicJsp() throws Exception {
        WebResponse response = getResponseFromAcceptGzipRequest("/Login.jsp");
        assertResponseGood(response, true);
    }

    /**
     * Tests that a page which is not   in the cache filter pattern is not cached.
     */
    @Test
    public void testUncachedPageIsNotCached() throws Exception {
        assertResponseGoodAndNotCached("/Login.jsp", true);
    }

}
