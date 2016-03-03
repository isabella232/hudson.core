/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Roy Varghese
 */
package hudson.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

/**
 * A cache for {@link TopLevelItems} object that are directly held by
 * the {@link Hudson} instance.
 * 
 * This class is package private.
 * 
 * @author Roy Varghese
 */
class TopLevelItemsCache {
    
    // Cache parameters
    // Seconds after which items in cache are removed. Time is reset on access.
    private static int EVICT_IN_SECONDS;
    
    // Initial cache capacity
    private static int INITIAL_CAPACITY;
    
    // Maximum number of cached entries.
    private static int MAX_ENTRIES;
    
    // Initialize from system properties if available
    {
        Integer val;
        
        val = Integer.getInteger("hudson.jobs.cache.evict_in_seconds");
        EVICT_IN_SECONDS = val == null? 60: val;
        
        val = Integer.getInteger("hudson.jobs.cache.initial_capacity");
        INITIAL_CAPACITY = val == null ? 1024: val;
        
        val = Integer.getInteger("hudson.jobs.cache.max_entries");
        MAX_ENTRIES = val == null ? 1024 : val;
    }
    
    final LoadingCache<LazyTopLevelItem.Key, TopLevelItem> cache;
    
    TopLevelItemsCache() { 
        
        cache = CacheBuilder.newBuilder()
                .initialCapacity(INITIAL_CAPACITY)
                .expireAfterAccess(EVICT_IN_SECONDS, TimeUnit.SECONDS)
                .maximumSize(MAX_ENTRIES)
                .softValues()
                .removalListener(new RemovalListener<LazyTopLevelItem.Key, TopLevelItem>() {

                    @Override
                    public void onRemoval(RemovalNotification<LazyTopLevelItem.Key, TopLevelItem> notification) {
                        // System.out.println("*** Removed from cache " + notification.getKey().name );
                    }
                    
                })
                .build(new CacheLoader<LazyTopLevelItem.Key, TopLevelItem>() {
                    
                    Map<String, Integer> map = new HashMap<String, Integer>();

                    @Override
                    public TopLevelItem load(LazyTopLevelItem.Key key) throws Exception {
                        try {
                            TopLevelItem item = (TopLevelItem) key.configFile.read();
                            item.onLoad(key.parent, key.name);
                            key.clearLoadErrorFlag();
                            return item;
                        }
                        catch (Throwable ex) {
                            System.err.println("ERROR===: Unable to load " +key.name);
                            key.setLoadErrorFlag();
                            return new BrokenTopLevelItem(key, ex);
                        }
                    }
                    
                });
        
        

    }
    
    
    
    TopLevelItem get(LazyTopLevelItem.Key key) {
        try {
            return cache.get(key);
        } catch (ExecutionException ex) {
            LoggerFactory.getLogger(TopLevelItemsCache.class.getName()).error("Error when retrieving item from cache", ex);
            return null;
        }

    }
    
    void put(LazyTopLevelItem.Key key, TopLevelItem item) {
        cache.put(key, item);
    }
    
}
