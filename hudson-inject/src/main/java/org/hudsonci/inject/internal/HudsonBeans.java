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

package org.hudsonci.inject.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import hudson.ExtensionFinder;
import hudson.PluginManager;
import hudson.model.Hudson;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import org.hudsonci.inject.SmoothieContainer;
import org.hudsonci.inject.internal.extension.ExtensionLocator;
import org.sonatype.inject.Parameters;

/**
 * Just satisfies dependencies required by CDI.
 * This class <strong>should not</strong> be used.
 *
 * @author Kaz Nishimura
 */
@ApplicationScoped
public class HudsonBeans {

    @Produces
    private PluginManager getPluginManager() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    private Hudson getHudson() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    private List<ExtensionFinder> getExtensionFinders() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    private SmoothieContainer getSmoothieContainer() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    @Named("default")
    private ExtensionLocator getDefaultExtensionLocator() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    private Injector getInjector() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    private Set<Service> getServices() {
        throw new RuntimeException("CDI is not supported");
    }

    @Produces
    @Parameters
    private Map<String, String> getParametersMap() {
        throw new RuntimeException("CDI is not supported");
    }
}
