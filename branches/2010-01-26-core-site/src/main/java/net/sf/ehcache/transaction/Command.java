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

package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 *
 * @author nelrahma
 *
 */
public interface Command {
    public static final String NULL = "NULL";  
    public static final String PUT = "PUT";
    public static final String REMOVE = "REMOVE";
    public static final String EXPIRE_ALL_ELEMENTS = "EXPIRE_ALL_ELEMENTS";
    public static final String REMOVE_ALL = "REMOVE_ALL";
    
    public String getCommandName();
    
    boolean execute(Store store);
    
    public boolean isPut(Object key);

    public boolean isRemove(Object key);

}
