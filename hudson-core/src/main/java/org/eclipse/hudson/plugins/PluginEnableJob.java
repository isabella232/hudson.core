/**
 * *****************************************************************************
 *
 * Copyright (c) 2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Winston Prakash
 *
 *****************************************************************************
 */
package org.eclipse.hudson.plugins;

import java.io.*;
import org.eclipse.hudson.plugins.InstalledPluginManager.InstalledPluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background Job to enable or disable a Job
 *
 * @author Winston Prakash
 */
public final class PluginEnableJob implements Runnable {

    private Logger logger = LoggerFactory.getLogger(PluginEnableJob.class);
    private final InstalledPluginInfo plugin;
    private boolean success;
    private String errorMsg;
    private boolean enable;

    public PluginEnableJob(InstalledPluginInfo plugin, boolean enable) {
        this.plugin = plugin;
        this.enable = enable;
    }

    public String getName() {
        return plugin.getShortName();
    }

    @Override
    public void run() {

        try {
            plugin.setEnable(enable);
        } catch (Exception exc) {
            logger.error(getName() + " installation unsuccessful", exc);
            success = false;
            errorMsg = getStackTrace(exc);
            return;
        } catch (Error err) {
            logger.error(getName() + " installation unsuccessful", err);
            success = false;
            errorMsg = getStackTrace(err);
            return;
        }
        success = true;
    }

    @Override
    public String toString() {
        return super.toString() + "[plugin=" + getName() + "]";
    }

    public boolean getStatus() {
        return success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public static String getStackTrace(Throwable throwable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);
        return result.toString();
    }
}
