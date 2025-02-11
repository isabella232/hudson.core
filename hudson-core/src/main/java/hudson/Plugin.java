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

package hudson;

import com.thoughtworks.xstream.XStream;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.SaveableListener;
import hudson.util.TimeUnit2;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Base class of Hudson plugin.
 *
 * <p> A plugin needs to derive from this class.
 *
 * <p> One instance of a plugin is created by Hudson, and used as the entry
 * point to plugin functionality.
 *
 * <p> A plugin is bound to URL space of Hudson as
 * <tt>${rootURL}/plugin/foo/</tt>, where "foo" is taken from your plugin name
 * "foo.hpi". All your web resources in src/main/webapp are visible from this
 * URL, and you can also define Jelly views against your Plugin class, and those
 * are visible in this URL, too.
 *
 * <p> {@link Plugin} can have an optional <tt>config.jelly</tt> page. If
 * present, it will become a part of the system configuration page
 * (http://server/hudson/configure). This is convenient for exposing/maintaining
 * configuration that doesn't fit any {@link Descriptor}s.
 *
 * <p> Up until Hudson 1.150 or something, subclasses of {@link Plugin} required
 * <tt>@plugin</tt> javadoc annotation, but that is no longer a requirement.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.42
 */
public abstract class Plugin implements Saveable {

    /**
     * Set by the {@link PluginManager}. This points to the
     * {@link PluginWrapper} that wraps this {@link Plugin} object.
     */
    /*package*/ transient PluginWrapper wrapper;

    /**
     * Called when a plugin is loaded to make the {@link ServletContext} object
     * available to a plugin. This object allows plugins to talk to the
     * surrounding environment.
     *
     * <p> The default implementation is no-op.
     *
     * @param context Always non-null.
     *
     * @since 1.42
     */
    public void setServletContext(ServletContext context) {
    }

    /**
     * Called to allow plugins to initialize themselves.
     *
     * <p> This method is called after
     * {@link #setServletContext(ServletContext)} is invoked. You can also use
     * {@link Hudson#getInstance()} to access the singleton hudson instance,
     * although the plugin start up happens relatively early in the
     * initialization stage and not all the data are loaded in Hudson.
     *
     * <p> If a plugin wants to run an initialization step after all plugins and
     * extension points are registered, a good place to do that is
     * {@link #postInitialize()}. If a plugin wants to run an initialization
     * step after all the jobs are loaded, {@link ItemListener#onLoaded()} is a
     * good place.
     *
     * @throws Exception any exception thrown by the plugin during the
     * initialization will disable plugin.
     *
     * @since 1.42
     * @see ExtensionPoint
     * @see #postInitialize()
     */
    public void start() throws Exception {
    }

    /**
     * Called after {@link #start()} is called for all the plugins.
     *
     * @throws Exception any exception thrown by the plugin during the
     * initialization will disable plugin.
     */
    public void postInitialize() throws Exception {
    }

    /**
     * Called to orderly shut down Hudson.
     *
     * <p> This is a good opportunity to clean up resources that plugin started.
     * This method will not be invoked if the {@link #start()} failed
     * abnormally.
     *
     * @throws Exception if any exception is thrown, it is simply recorded and
     * shut-down of other plugins continue. This is primarily just a convenience
     * feature, so that each plugin author doesn't have to worry about catching
     * an exception and recording it.
     *
     * @since 1.42
     */
    public void stop() throws Exception {
    }

    /**
     * @since 1.233
     * @deprecated as of 1.305 override
     * {@link #configure(StaplerRequest,JSONObject)} instead
     */
    public void configure(JSONObject formData) throws IOException, ServletException, FormException {
    }

    /**
     * Handles the submission for the system configuration.
     *
     * <p> If this class defines <tt>config.jelly</tt> view, be sure to override
     * this method and persists the submitted values accordingly.
     *
     * <p> The following is a sample <tt>config.jelly</tt> that you can start
     * yours with:
     * <pre><xmp>
     * <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define" xmlns:l="/lib/layout" xmlns:t="/lib/hudson" xmlns:f="/lib/form">
     *   <f:section title="Locale">
     *     <f:entry title="${%Default Language}" help="/plugin/locale/help/default-language.html">
     *       <f:textbox name="systemLocale" value="${it.systemLocale}" />
     *     </f:entry>
     *   </f:section>
     * </j:jelly>
     * </xmp></pre>
     *
     * <p> This allows you to access data as
     * {@code formData.getString("systemLocale")}
     *
     * <p> If you are using this method, you'll likely be interested in using
     * {@link #save()} and {@link #load()}.
     *
     * @since 1.305
     */
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {
        configure(formData);
    }

    /**
     * This method serves static resources in the plugin under
     * <tt>hudson/plugin/SHORTNAME</tt>.
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, URISyntaxException {
        String path = req.getRestOfPath();

        if (path.length() == 0) {
            path = "/";
        }

        String requestPath = req.getRequestURI().substring(req.getContextPath().length());
        boolean staticLink = requestPath.startsWith("/static/");
        long expires = staticLink ? TimeUnit2.DAYS.toMillis(365) : -1;

        try {
            rsp.serveLocalizedFile(req, wrapper.baseResourceURL.toURI().resolve(new URI(null, '.' + path, null)).toURL(), expires);
        } catch (URISyntaxException x) {
            throw new IOException(x);
        }
    }

//
// Convenience methods for those plugins that persist configuration
//
    /**
     * Loads serializable fields of this instance from the persisted storage.
     *
     * <p> If there was no previously persisted state, this method is no-op.
     *
     * @since 1.245
     */
    protected void load() throws IOException {
        XmlFile xml = getConfigXml();
        if (xml.exists()) {
            xml.unmarshal(this);
        }
    }

    /**
     * Saves serializable fields of this instance to the persisted storage.
     *
     * @since 1.245
     */
    public void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }
        getConfigXml().write(this);
        SaveableListener.fireOnChange(this, getConfigXml());
    }

    /**
     * Controls the file where {@link #load()} and {@link #save()} persists
     * data.
     *
     * This method can be also overriden if the plugin wants to use a custom
     * {@link XStream} instance to persist data.
     *
     * @since 1.245
     */
    protected XmlFile getConfigXml() {
        return new XmlFile(Hudson.XSTREAM,
                new File(Hudson.getInstance().getRootDir(), wrapper.getShortName() + ".xml"));
    }

    /**
     * Dummy instance of {@link Plugin} to be used when a plugin didn't supply
     * one on its own.
     *
     * @since 1.321
     */
    public static final class DummyImpl extends Plugin {
    }
}
