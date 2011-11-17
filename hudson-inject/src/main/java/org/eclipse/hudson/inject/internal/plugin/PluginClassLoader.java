/*******************************************************************************
 *
 * Copyright (c) 2010-2011 Sonatype, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *   
 *     
 *
 *******************************************************************************/ 

package org.eclipse.hudson.inject.internal.plugin;

import hudson.PluginWrapper;
import org.aspectj.weaver.loadtime.WeavingURLClassLoader;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Plugin class-loader.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 1.397
 */
public class PluginClassLoader
    extends WeavingURLClassLoader
{
    private PluginWrapper plugin;

    public PluginClassLoader(final List<URL> urls, final ClassLoader parent) {
        super(urls.toArray(new URL[urls.size()]), parent);

        // Some optional classes might not exist, so want to avoid excessive tracing
        Logger.getLogger(WeavingURLClassLoader.class.getName()).setLevel(Level.OFF);
    }

    public PluginWrapper getPlugin() {
        checkState(plugin != null);
        return plugin;
    }

    void setPlugin(final PluginWrapper plugin) {
        checkState(this.plugin == null);
        this.plugin = checkNotNull(plugin);
    }

    @Override
    public String toString() {
        return "PluginClassLoader{" +
            (plugin != null ? plugin.getShortName() : "???") +
            '}';
    }
}
