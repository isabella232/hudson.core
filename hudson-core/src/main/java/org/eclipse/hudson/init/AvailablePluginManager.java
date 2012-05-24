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
 *   Winston Prakash
 *
 *****************************************************************************
 */
package org.eclipse.hudson.init;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.sf.json.JSONObject;

/**
 * Utility that provides information about the plugins available for
 * installation and updates during Initial Setup
 *
 * @author Winston Prakash
 */
public final class AvailablePluginManager {

    private final Map<String, AvailablePluginInfo> availablePluginInfos = new TreeMap<String, AvailablePluginInfo>(String.CASE_INSENSITIVE_ORDER);
    private URL initPluginsJsonUrl;

    AvailablePluginManager(URL pluginsJsonUrl) throws IOException {
        initPluginsJsonUrl = pluginsJsonUrl;
        parseJson();
    }

    public Set<String> getAvailablePluginNames() {
        return availablePluginInfos.keySet();
    }

    public AvailablePluginInfo getAvailablePlugin(String name) {
        return availablePluginInfos.get(name);
    }

    private void parseJson() throws IOException {
        StringWriter strWriter = new StringWriter();
        PrintWriter prtWriter = new PrintWriter(strWriter);
        BufferedReader in = new BufferedReader(new InputStreamReader(initPluginsJsonUrl.openStream()));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                prtWriter.println(line);
            }
        } finally {
            in.close();
        }
        JSONObject jsonObject = JSONObject.fromObject(strWriter.toString());
        for (Map.Entry<String, JSONObject> e : (Set<Map.Entry<String, JSONObject>>) jsonObject.getJSONObject("plugins").entrySet()) {
            availablePluginInfos.put(e.getKey(), new AvailablePluginInfo(e.getValue()));
        }
    }

    public final static class AvailablePluginInfo {

        private String name;
        private String version;
        private String downloadUrl;
        private String wikiUrl;
        private String displayName;
        private String description;
        private String[] categories;
        private boolean mandatory = false;
        private Map<String, String> dependencies = new HashMap<String, String>();

        public AvailablePluginInfo(JSONObject jsonObject) {
            parseJsonObject(jsonObject);
        }

        public String getDisplayName() {
            if (displayName != null) {
                return displayName;
            }
            return name;
        }

        public String[] getCategories() {
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

        public boolean isMandatory() {
            return mandatory;
        }

        @Override
        public String toString() {
            return "[Plugin Name:" + name + " Display Name:" + displayName + " Version:" + version + " Wiki Url:" + wikiUrl + "Download Url:" + downloadUrl + "]";
        }
        
        private void parseJsonObject(JSONObject jsonObject) {
            name = jsonObject.getString("name");
            version = jsonObject.getString("version");
            downloadUrl = jsonObject.getString("downloadUrl");
            wikiUrl = get(jsonObject, "wikiUrl");
            displayName = get(jsonObject, "displayName");
            description = get(jsonObject, "description");
            if (jsonObject.has("mandatory")) {
                mandatory = jsonObject.getBoolean("mandatory");
            } else {
                mandatory = false;
            }

            categories = jsonObject.has("categories") ? (String[]) jsonObject.getJSONArray("categories").toArray(new String[0]) : null;
            for (Object jo : jsonObject.getJSONArray("dependencies")) {
                JSONObject depObj = (JSONObject) jo;
                if (get(depObj, "name") != null
                        && !get(depObj, "name").equals("maven-plugin")
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
    }
}
