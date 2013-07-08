/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package org.eclipse.hudson.init;

import hudson.model.Hudson;
import hudson.model.User;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.HudsonFailedToLoad;
import java.io.File;
import javax.servlet.ServletContext;
import org.slf4j.Logger;
import org.eclipse.hudson.WebAppController;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;

/**
 * Init runnable in public class so it can be constructed reflectively.
 * 
 * @since 3.1.0
 * @author Bob Foster
 */
public class InitialRunnable  implements Runnable {
        
        WebAppController controller;
        Logger logger;
        private File hudsonHomeDir;
        private ServletContext servletContext;
        private boolean restart;
        public InitialRunnable(WebAppController controller, Logger logger, File hudsonHomeDir, ServletContext servletContext, boolean restart) {
            this.controller = controller;
            this.logger = logger;
            this.hudsonHomeDir = hudsonHomeDir;
            this.servletContext = servletContext;
            this.restart = restart;
        }
        
        @Override
        public void run() {
            try {
                // Creating of the god object performs most of the booting muck
                Hudson hudson = new Hudson(hudsonHomeDir, servletContext, null, restart);

                //Now Hudson is fully loaded, reload Hudson Security Manager
                HudsonSecurityEntitiesHolder.setHudsonSecurityManager(new HudsonSecurityManager(hudsonHomeDir));

                // once its done, hook up to stapler and things should be ready to go
                controller.install(hudson);

                // trigger the loading of changelogs in the background,
                // but give the system 10 seconds so that the first page
                // can be served quickly
                Trigger.timer.schedule(new SafeTimerTask() {
                    public void doRun() {
                        User.getUnknown().getBuilds();
                    }
                }, 1000 * 10);
            } catch (Error e) {
                logger.error("Failed to initialize Hudson", e);
                controller.install(new HudsonFailedToLoad(e));
                throw e;
            } catch (Exception e) {
                logger.error("Failed to initialize Hudson", e);
                controller.install(new HudsonFailedToLoad(e));
            }
        }
    }
