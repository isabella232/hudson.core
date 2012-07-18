/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Tom Huybrechts
 *
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.tasks.Builder;
import hudson.EnvVars;

import java.io.IOException;
import java.util.Map;

/**
 * Represents some resources that are set up for the duration of a build to be
 * torn down when the build is over.
 *
 * <p> This is often used to run a parallel server necessary during a build,
 * such as an application server, a database reserved for the build, X server
 * for performing UI tests, etc.
 *
 * <p> By having a plugin that does this, instead of asking each build script to
 * do this, we can simplify the build script. {@link Environment} abstraction
 * also gives you guaranteed "tear down" phase, so that such resource won't keep
 * running forever.
 *
 * @since 1.286
 */
public abstract class Environment {

    /**
     * Adds environmental variables for the builds to the given map.
     *
     * <p> If the {@link Environment} object wants to pass in information to the
     * build that runs, it can do so by exporting additional environment
     * variables to the map.
     *
     * <p> When this method is invoked, the map already contains the current
     * "planned export" list.
     *
     * @param env never null. This really should have been typed as
     * {@link EnvVars} but by the time we realized it it was too late.
     */
    public void buildEnvVars(Map<String, String> env) {
        // no-op by default
    }

    /**
     * Runs after the {@link Builder} completes, and performs a tear down.
     *
     * <p> This method is invoked even when the build failed, so that the clean
     * up operation can be performed regardless of the build result (for
     * example, you'll want to stop application server even if a build fails.)
     *
     * @param build The same {@link Build} object given to the set up method.
     * @param listener The same {@link BuildListener} object given to the set up
     * method.
     * @return true if the build can continue, false if there was an error and
     * the build needs to be aborted.
     * @throws IOException terminates the build abnormally. Hudson will handle
     * the exception and reports a nice error message.
     */
    public boolean tearDown(AbstractBuild build, BuildListener listener)
            throws IOException, InterruptedException {
        return true;
    }

    /**
     * Creates {@link Environment} implementation that just sets the variables
     * as given in the parameter.
     */
    public static Environment create(final EnvVars envVars) {
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.putAll(envVars);
            }
        };
    }
}
