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
*    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package hudson.tasks;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

/**
 * {@link BuildStep}s that perform the actual build.
 *
 * <p> To register a custom {@link Builder} from a plugin, put {@link Extension}
 * on your descriptor.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Builder extends BuildStepCompatibilityLayer implements BuildStep, Describable<Builder>, ExtensionPoint {

//
// these two methods need to remain to keep binary compatibility with plugins built with Hudson < 1.150
    
    /**
     * @since 3.3.0
     * Ability to temporarily disable a builder with out deleting it 
     * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=386082)
     */
    private boolean disabled = false;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
    /**
     * @since 3.3.0
     * Description of what this builder does as documentation
     * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=414876)
     */
    
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Default implementation that does nothing.
     */
    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    /**
     * Returns {@link BuildStepMonitor#NONE} by default, as {@link Builder}s
     * normally don't depend on its previous result.
     */
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public Descriptor<Builder> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Returns all the registered {@link Builder} descriptors.
     */
    // for backward compatibility, the signature is not BuildStepDescriptor
    public static DescriptorExtensionList<Builder, Descriptor<Builder>> all() {
        return Hudson.getInstance().<Builder, Descriptor<Builder>>getDescriptorList(Builder.class);
    }
}
