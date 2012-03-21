/*******************************************************************************
 *
 * Copyright (c) 2011, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *  Winston Prakash
 *      
 *******************************************************************************/ 

package org.hudsonci.script;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Extension point for adding Scripting Support to Hudson.
 *
 * A default constructor is needed to create ScriptSupport in
 * the default configuration.
 *
 * @author Winston Prakash
 * @since 3.0.0
 * @see ScriptSupportDescriptor
 */
public abstract class ScriptSupport extends AbstractDescribableImpl<ScriptSupport> implements ExtensionPoint {
    
    public static String SCRIPT_GROOVY = "groovy";
    public static String SCRIPT_JAVASCRIPT = "javascript";
    public static String SCRIPT_SCALA = "scala";
    public static String SCRIPT_RUBY = "ruby";
    public static String SCRIPT_ERLANG = "erlang";
    public static String SCRIPT_PYTHON = "python";
    
    public static String DEFAULT = "SCRIPT_GROOVY";
    
    /**
     * Returns all the registered {@link ScriptSupport} descriptors.
     */
    public static DescriptorExtensionList<ScriptSupport, Descriptor<ScriptSupport>> all() {
        if (Hudson.getInstance() != null){
            return Hudson.getInstance().<ScriptSupport, Descriptor<ScriptSupport>>getDescriptorList(ScriptSupport.class);
        }else{
            return null;
        }
    }
    
    /**
     * Return true if the give scriptType is supported
     * @param scriptType
     * @return true if supported
     */
    public abstract boolean hasSupport(String scriptType); 
 
    
    /**
     * Return the type of Dynamic Language Supported ("groovy", "javascript", "scala", "ruby" etc)
     * @return 
     */
    public abstract String getType();
    
    /**
     * Evaluate the expression String and return the result
     * @param expression -  string to evaluate as expression
     * @return Object, evaluated value 
     */
    public abstract Object evaluateExpression(String expression);
    
    /**
     * Evaluate the expression String and return the result
     * @param expression -  string to evaluate as expression
     * @param variableMap - map of variables and corresponding object to bind to
     * @return Object, evaluated value 
     */
    public abstract Object evaluateExpression(String expression, Map<String, Object> variableMap);
    
    /**
     * Evaluate the script and write the result to print writer
     * @param script, string of script to evaluate
     * @param printWriter -  Standard output Print Writer
     */
    public abstract void evaluate(String script,  PrintWriter printWriter);
    
    /**
     * Evaluate the script and write the result to print writer
     * @param script, string of script to evaluate
     * @param variableMap - map of variables and corresponding object to bind to
     * @param printWriter -  Standard output Print Writer
     */
    public abstract void evaluate(String script, Map<String, Object> variableMap, PrintWriter printWriter);
    
    /**
     * Evaluate the script and write the result to print writer
     * @param classLoader - pass in the Class Loader to script JVM  
     * @param script, string of script to evaluate
     * @param variableMap - map of variables and corresponding object to bind to
     * @param printWriter -  Standard output Print Writer
     */
    public abstract void evaluate(ClassLoader classLoader, String script, Map<String, Object> variableMap, PrintWriter printWriter);
       

    @Override
    public ScriptSupportDescriptor getDescriptor() {
        return (ScriptSupportDescriptor)super.getDescriptor();
    }
    
    public static List<ScriptSupport> getAvailableScriptSupports() {
        List<ScriptSupport> scriptSupports = new ArrayList<ScriptSupport>();
        if (ScriptSupport.all() != null && !ScriptSupport.all().isEmpty()) {
            for (Descriptor<ScriptSupport> scriptSupport : ScriptSupport.all()) {
                try {
                    scriptSupports.add(scriptSupport.newInstance(null, null));
                } catch (FormException ex) {
                    Logger logger = LoggerFactory.getLogger(ScriptSupport.class);
                    logger.error("Failed to instantiate Script Support - " + scriptSupport.getDisplayName());
                }
            }
        }
        return scriptSupports;
    }
    
    public Logger getLogger(){
        return LoggerFactory.getLogger(ScriptSupport.class);
    }
}
