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

import hudson.ProxyConfiguration;
import hudson.model.Messages;
import hudson.util.TextFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

/**
 * Utility that manages Update Site and local copy of Update Site. Provides
 * information about plugins available for installation and their updates
 *
 * @since 3.0.0
 * @author Winston Prakash
 */
public final class UpdateSiteManager {

    public static final String MANDATORY = "mandatory";
    public static final String FEATURED = "featured";
    public static final String RECOMMENDED = "recommended";
    private Map<String, AvailablePluginInfo> availablePluginInfos = new TreeMap<String, AvailablePluginInfo>(String.CASE_INSENSITIVE_ORDER);
    private final String updateServer = System.getProperty("updateServer", "http://hudson-ci.org/update-center3/");
    private String updateSiteUrl = updateServer + "update-center.json";
    private ProxyConfiguration proxyConfig;
    private final String id = "default";
    private File hudsonHomeDir;
    private Set<String> pluginCategories = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    public UpdateSiteManager(String id, File homeDir, ProxyConfiguration proxyConfig) throws IOException {
        hudsonHomeDir = homeDir;
        this.proxyConfig = proxyConfig;
        refresh();
    }

    public String getId() {
        return id;
    }

    public Set<AvailablePluginInfo> searchPlugins(String patternStr, boolean searchDescription) {
        Set<AvailablePluginInfo> availablePlugins = new TreeSet<AvailablePluginInfo>();

        if ((patternStr != null) && !"".equals(patternStr)) {

            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Set<String> availablePluginNames = getAvailablePluginNames();
            for (String pluginName : availablePluginNames) {
                AvailablePluginInfo availablePlugin = getAvailablePlugin(pluginName);
                Matcher matcher;
                String displayName = availablePlugin.getDisplayName();
                if ((displayName != null) && !"".equals(displayName)) {
                    matcher = pattern.matcher(displayName);
                    if (matcher.find()) {
                        availablePlugins.add(availablePlugin);
                    }
                }
                String description = availablePlugin.getDescription();
                if (searchDescription && (description != null) && !"".equals(description)) {
                    matcher = pattern.matcher(description);
                    if (matcher.find()) {
                        availablePlugins.add(availablePlugin);
                    }
                }
            }
        }
        return availablePlugins;
    }

    public List<AvailablePluginInfo> getAvailablePlugins(String pluginType) {
        List<AvailablePluginInfo> availablePlugins = new ArrayList<AvailablePluginInfo>();
        Set<String> availablePluginNames = getAvailablePluginNames();
        for (String pluginName : availablePluginNames) {
            AvailablePluginInfo availablePlugin = getAvailablePlugin(pluginName);
            String type = availablePlugin.getType();
            if (pluginType.equals(type)) {
                availablePlugins.add(availablePlugin);
            }
        }
        return availablePlugins;
    }

    public List<AvailablePluginInfo> getCategorizedAvailablePlugins(String pluginType, String category) {
        List<AvailablePluginInfo> availablePlugins = new ArrayList<AvailablePluginInfo>();
        for (AvailablePluginInfo plugin : getAvailablePlugins(pluginType)) {
            if (plugin.isBelongToCategory(category)) {
                availablePlugins.add(plugin);
            }
        }
        return availablePlugins;
    }

    public void verifyUpdateSite(String remoteUrl) throws IOException {
        InputStream inputStream;
        try {
            URL updateCenterRemoteUrl = new URL(remoteUrl);
            inputStream = proxyConfig.openUrl(updateCenterRemoteUrl).getInputStream();
        } catch (Exception exc) {
            throw new IOException("Could not connect to  " + remoteUrl + ". "
                    + "If you are behind a firewall set HTTP proxy and try again.");
        }
        String jsonStr = IOUtils.toString(inputStream);
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("updateCenter.post(")) {
            jsonStr = jsonStr.substring("updateCenter.post(".length());
        } else {
            throw new IOException(remoteUrl + " does not seem to be a valid update site");
        }
        if (jsonStr.endsWith(");")) {
            jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf(");"));
        }
        parseJson(jsonStr);
    }

    public void setUpdateSiteUrl(String remoteUrl) {
        updateSiteUrl = remoteUrl;
    }

    public Set<String> getPluginCategories() {
        return pluginCategories;
    }

    public void refreshFromUpdateSite() throws IOException {
        InputStream inputStream;
        try {
            URL updateCenterRemoteUrl = new URL(getUpdateSiteUrl());
            inputStream = proxyConfig.openUrl(updateCenterRemoteUrl).getInputStream();
        } catch (Exception exc) {
            throw new IOException("Could not connect to  " + getUpdateSiteUrl() + ". "
                    + "If you are behind a firewall set HTTP proxy and try again.");
        }
        String jsonStr = IOUtils.toString(inputStream);
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("updateCenter.post(")) {
            jsonStr = jsonStr.substring("updateCenter.post(".length());
        }
        if (jsonStr.endsWith(");")) {
            jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf(");"));
        }
        availablePluginInfos = parseJson(jsonStr);
        getLocalCacheFile().write(jsonStr);
    }

    public String getUpdateSiteUrl() throws IOException {
        return updateSiteUrl;
    }

    public Set<String> getAvailablePluginNames() {
        return availablePluginInfos.keySet();
    }

    public AvailablePluginInfo getAvailablePlugin(String name) {
        return availablePluginInfos.get(name);
    }

    public String getCategoryDisplayName(String category) {
        if (category == null) {
            return Messages.UpdateCenter_PluginCategory_misc();
        }
        try {
            return (String) Messages.class.getMethod(
                    "UpdateCenter_PluginCategory_" + category.replace('-', '_')).invoke(null);
        } catch (Exception ex) {
            return Messages.UpdateCenter_PluginCategory_unrecognized(category);
        }
    }
    
    public void refresh() throws IOException{
        if (getLocalCacheFile().exists()) {
            availablePluginInfos = parseJson(getLocalCacheFile().readTrim());
        }
    }

    private Map<String, AvailablePluginInfo> parseJson(String jsonString) throws IOException {
        Map<String, AvailablePluginInfo> pluginInfos = new TreeMap<String, AvailablePluginInfo>(String.CASE_INSENSITIVE_ORDER);
        try {
            JSONObject jsonObject = JSONObject.fromObject(jsonString);
            for (Map.Entry<String, JSONObject> e : (Set<Map.Entry<String, JSONObject>>) jsonObject.getJSONObject("plugins").entrySet()) {
                AvailablePluginInfo pluginInfo = new AvailablePluginInfo(e.getValue());
                if (!"disabled".equals(pluginInfo.getType())) {
                    pluginInfos.put(e.getKey(), pluginInfo);
                }
            }
        } catch (Exception exc) {
            System.out.println(jsonString);
            throw new IOException("Incorrect Update Center JSON. " + exc.getLocalizedMessage());
        }
        return pluginInfos;

    }
    
    AvailablePluginInfo createAvailablePluginInfo(String name, String version, String displayName, String wikiUrl){
        return new AvailablePluginInfo(name, version, displayName, wikiUrl);
    }

    public final class AvailablePluginInfo implements Comparable<AvailablePluginInfo> {

        private String name;
        private String version;
        private String downloadUrl;
        private String wikiUrl;
        private String displayName;
        private String description = "";
        private String type;
        private List<String> categories = new ArrayList<String>();
        private String requiredCore;
        private String compatibleSinceVersion;
        private Map<String, String> dependencies = new HashMap<String, String>();

        public AvailablePluginInfo(JSONObject jsonObject) {
            parseJsonObject(jsonObject);
        }
        
        public AvailablePluginInfo(String name, String version, String displayName, String wikiUrl){
            this.name = name;
            this.version = version;
            this.displayName = displayName;
            this.wikiUrl = wikiUrl;
        }

        public String getDisplayName() {
            if (displayName != null) {
                return displayName;
            }
            return name;
        }

        public List<String> getCategories() {
            return categories;
        }

        public Map<String, String> getDependencies() {
            return dependencies;
        }

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getWikiUrl() {
            return wikiUrl;
        }

        public String getVersion() {
            return version;
        }

        public String getType() {
            return type;
        }

        public String getRequiredCore() {
            return requiredCore;
        }

        public String getCompatibleSinceVersion() {
            return compatibleSinceVersion;
        }

        @Override
        public String toString() {
            return "[Plugin Name:" + name + " Display Name:" + displayName + " Version:" + version + " Wiki Url:" + wikiUrl + "Download Url:" + downloadUrl + "]";
        }

        private void parseJsonObject(JSONObject jsonObject) {
            name = jsonObject.getString("name");
            version = jsonObject.getString("version");
            downloadUrl = jsonObject.getString("url");
            wikiUrl = get(jsonObject, "wiki");
            displayName = get(jsonObject, "title");
            description = get(jsonObject, "excerpt");
            requiredCore = get(jsonObject, "requiredCore");
            compatibleSinceVersion = get(jsonObject, "compatibleSinceVersion");
            type = get(jsonObject, "type");

            if ((type == null) || "".equals(type)) {
                type = "others";
            }

            if (jsonObject.has("labels")) {
                String[] labels = (String[]) jsonObject.getJSONArray("labels").toArray(new String[0]);
                for (String label : labels) {
                    String category = getCategoryDisplayName(label);
                    if (!pluginCategories.contains(category)) {
                        pluginCategories.add(category);
                    }
                    categories.add(category);
                }
            } else {
                categories.add("Uncategorized");
            }

            for (String category : categories) {
                if (!pluginCategories.contains(category)) {
                    pluginCategories.add(category);
                }
            }
            for (Object jo : jsonObject.getJSONArray("dependencies")) {
                JSONObject depObj = (JSONObject) jo;
                if (get(depObj, "name") != null
                        && get(depObj, "optional").equals("false")) {
                    dependencies.put(get(depObj, "name"), get(depObj, "version"));
                }
            }
        }

        private String get(JSONObject o, String prop) {
            if (o.has(prop)) {
                String value = o.getString(prop);
                if (!"null".equals(value) && !"\"null\"".equals(value)) {
                    return value;
                }
            }
            return null;
        }

        boolean isBelongToCategory(String category) {
            if (categories != null) {
                for (String cat : categories) {
                    if (category.equalsIgnoreCase(cat)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int compareTo(AvailablePluginInfo t) {
            return this.getName().compareToIgnoreCase(t.getName());
        }
    }

    private TextFile getLocalCacheFile() {
        return new TextFile(new File(hudsonHomeDir,
                "updates/" + getId() + ".json"));
    }
}
