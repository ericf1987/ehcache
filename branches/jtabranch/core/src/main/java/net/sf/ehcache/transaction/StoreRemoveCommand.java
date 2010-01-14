package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StoreRemoveCommand implements StoreWriteCommand {

    private final Object key;

    public StoreRemoveCommand(final Object key) {
        this.key = key;
    }

    public void execute(final Store store) {
        store.remove(key);
    }
    
}
