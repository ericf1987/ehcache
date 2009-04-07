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

package net.sf.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Less Frequently Used (LFU) implementation of the memory store. Actually keeping track of the least used, then the
 * second least and so on is very expensive, 3 or 4 orders of magnitude more expensive than the others policies. Costs are
 * incurred on put, get and remove.
 * <p/>
 * Instead this implementation does not quarantee that
 * the element removed will actually be the least used. Rather, it lets you make statements about confidence intervals
 * against the likelihood that an element is in some lowest percentile of the hit count distribution. Costs are only
 * incurred on overflow when an element to be evicted must be selected.
 * <p/>
 * For those with a statistical background the branch of stats which deals with this is hypothesis testing and
 * the Student's T distribution. The higher your sample the greater confidence you can have in a hypothesis, in
 * this case whether or not the "lowest" value lies in the bottom half or quarter of the distribution. Adding
 * samples rapidly increases confidence but the return from extra sampling rapidly diminishes.
 * <p/>
 * Cost is not affected much by sample size, indicating it is probably the iteration that is causing most of the
 * time. If we had access to the array backing Map, all would work very fast.
 * <p/>
 * A 99.99% confidence interval can be achieved that the "lowest" element is actually in the bottom quarter of the
 * hit count distribution with a sample size of 30, which is the default.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class LfuMemoryStore extends MemoryStore {

    private static final Logger LOG = Logger.getLogger(LfuMemoryStore.class.getName());

    /**
     * This is the default from {@link ConcurrentHashMap}. It should never be used, because
     * we size the map to the max size of the store.
     */
    private static final float DEFAULT_LOAD_FACTOR = .75f;

    /**
     * Set optimisation for 100 concurrent threads.
     */
    private static final int CONCURRENCY_LEVEL = 100;


    private Policy policy;

    /**
     * Constructor for the LfuMemoryStore object.
     */
    protected LfuMemoryStore(Ehcache cache, Store diskStore) {
        super(cache, diskStore);
        policy = new LfuPolicy();
        map = new ConcurrentHashMap(cache.getCacheConfiguration().getMaxElementsInMemory(), DEFAULT_LOAD_FACTOR, CONCURRENCY_LEVEL);
    }


    /**
     * Puts an element into the cache.
     */
    public final synchronized void doPut(Element elementJustAdded) {
        if (isFull()) {
            removeLfuElement(elementJustAdded);
        }
    }


    private void removeLfuElement(Element elementJustAdded) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Cache is full. Removing LFU element ...");
        }

        // First element of the sorted list is the candidate for the removal
        Element element = findRelativelyUnused(elementJustAdded);
        //this CAN happen rarely. Let the store get one bigger
        if (element == null) {
            return;
        }

        // If the element is expired remove
        if (element.isExpired()) {
            remove(element.getObjectKey());
            notifyExpiry(element);
            return;
        }

        evict(element);
        remove(element.getObjectKey());
    }

    /**
     * Find a "relatively" unused element, but not the element just added.
     */
    final Element findRelativelyUnused(Element elementJustAdded) {
        Element[] elements = sampleElements(map.size());
        //can be null. Let the cache get bigger by one.
        return policy.selectedBasedOnPolicy(elements, elementJustAdded);
    }

    /**
     * Uses random numbers to sample the entire map.
     *
     * @return an array of sampled elements
     */
     Element[] sampleElements(int size) {
        int[] offsets = LfuPolicy.generateRandomSample(size);
        Element[] elements = new Element[offsets.length];
        Iterator iterator = map.values().iterator();
        for (int i = 0; i < offsets.length; i++) {
            for (int j = 0; j < offsets[i]; j++) {
                //fast forward
                try {
                    iterator.next();
                } catch (NoSuchElementException e) {
                    //e.printStackTrace();
                }
            }

            try {
                elements[i] = ((Element) iterator.next());
            } catch (NoSuchElementException e) {
                //e.printStackTrace();
            }
        }
        return elements;
    }

}





