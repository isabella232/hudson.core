/*
 * Copyright (c) 2015 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package hudson.util;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.hudson.init.InitialSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 *
 * @author Bob Foster
 */
public class ThreadLocalUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadLocalUtils.class);

    private static final boolean VERBOSE = true;

    /**
     * Remove thread locals created by instance class loaders.
     *
     * @param verbose true if log each time threadlocals found or removed; otherwise silent
     */
    public static void removeThreadLocals() {
        Set<ThreadLocal> threadLocals = getThreadLocals(getLoaders());
        for (ThreadLocal threadLocal : threadLocals) {
            if (VERBOSE) {
                Object value = threadLocal.get();
                LOGGER.info("Removing thread local with key ["+threadLocal+"] and value ["+value+"]");
            }
            threadLocal.remove();
        }
    }

    private static ClassLoader[] getLoaders() {
        ClassLoader[] loaders = new ClassLoader[2];
        loaders[0] = ThreadLocalUtils.class.getClassLoader();
        loaders[1] = InitialSetup.getHudsonContextClassLoader();
        return loaders;
    }

    private static Set<ThreadLocal> getThreadLocals(ClassLoader[] loaders) {
        Set<ThreadLocal> threadLocals = new HashSet<ThreadLocal>();
        try {
            Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Object threadLocalTable = threadLocalsField.get(Thread.currentThread());

            Class threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = threadLocalMapClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Object table = tableField.get(threadLocalTable);

            if (table == null) {
                return threadLocals;
            }

            Field referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);

            for (int i=0; i < Array.getLength(table); i++) {
                Object entry = Array.get(table, i);
                if (entry != null) {
                    ThreadLocal threadLocal = (ThreadLocal)referentField.get(entry);
                    if (threadLocal != null) {
                        Object value = threadLocal.get();
                        if (value instanceof SecurityContextImpl) {
                            // This may risk removing threadlocals added by other apps,
                            // but perhaps we are doing them a favor.
                            threadLocals.add(threadLocal);
                        } else {
                            ClassLoader tlClassLoader = threadLocal.getClass().getClassLoader();
                            for (ClassLoader cl : loaders) {
                                if (tlClassLoader == cl) {
                                    threadLocals.add(threadLocal);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            LOGGER.warn("Exception getting ThreadLocals in thread "+Thread.currentThread().getName(), e);
        }
        return threadLocals;
    }
}
