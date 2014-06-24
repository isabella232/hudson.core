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

import hudson.PluginManager.FailedPlugin;
import hudson.Util;
import hudson.model.Hudson;
import hudson.util.VersionNumber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility that provides information about the plugins that are already
 * installed
 *
 * @author Winston Prakash
 */
public final class InstalledPluginManager {

    private final Logger logger = LoggerFactory.getLogger(InstalledPluginManager.class);
    private final Map<String, InstalledPluginInfo> installedPluginInfos = new HashMap<String, InstalledPluginInfo>();
    private final File pluginsDir;

    public InstalledPluginManager(File dir) {
        pluginsDir = dir;
        loadInstalledPlugins();
    }

    public File getPluginsDir() {
        return pluginsDir;
    }

    public Set<String> getInstalledPluginNames() {
        return installedPluginInfos.keySet();
    }

    public InstalledPluginInfo getInstalledPlugin(String name) {
        return installedPluginInfos.get(name);
    }

    public boolean isInstalled(String name) {
        return installedPluginInfos.keySet().contains(name);
    }

    public void loadInstalledPlugins() {
        File[] hpiArchives = pluginsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("hpi") || name.endsWith("hpl");
            }
        });
        if ((hpiArchives != null) && (hpiArchives.length > 0)) {
            for (File archive : hpiArchives) {
                try {
                    InstalledPluginInfo installedPluginInfo = new InstalledPluginInfo(archive);
                    installedPluginInfos.put(installedPluginInfo.getShortName(), installedPluginInfo);
                } catch (IOException exc) {
                    logger.warn("Failed to load plugin ", exc);
                }
            }
        }
    }

    static final class Dependency {

        private final String shortName;
        private final String version;
        private final boolean optional;

        public Dependency(String s) {
            int idx = s.indexOf(':');
            if (idx == -1) {
                throw new IllegalArgumentException("Illegal dependency specifier " + s);
            }
            this.shortName = s.substring(0, idx);
            this.version = s.substring(idx + 1);

            boolean isOptional = false;
            String[] osgiProperties = s.split(";");
            for (int i = 1; i < osgiProperties.length; i++) {
                String osgiProperty = osgiProperties[i].trim();
                if (osgiProperty.equalsIgnoreCase("resolution:=optional")) {
                    isOptional = true;
                }
            }
            this.optional = isOptional;
        }

        public String getShortName() {
            return shortName;
        }

        public String getVersion() {
            return version;
        }

        public boolean isOptional() {
            return optional;
        }

        @Override
        public String toString() {
            return shortName + " (" + version + ")";
        }
    }

    public final static class InstalledPluginInfo {

        private final List<Dependency> dependencies = new ArrayList<Dependency>();

        private File hpiArchive;
        private String shortName;
        private String wikiUrl;
        private String longName;
        private String version;

        public InstalledPluginInfo(File archive) throws IOException {
            hpiArchive = archive;
            parseManifest();
        }

        void parseManifest() throws IOException {
            Manifest manifest;
            JarFile jarfile = null;

            boolean isLinked = hpiArchive.getName().endsWith(".hpl");
            if (isLinked) {
                // resolve the .hpl file to the location of the manifest file
                BufferedReader br = new BufferedReader(new FileReader(hpiArchive));
                String firstLine = br.readLine();
                if (firstLine.startsWith("Manifest-Version:")) {
                    // this is the manifest already
                } else {
                    // indirection
                    hpiArchive = resolve(hpiArchive, firstLine);
                }
                // then parse manifest
                FileInputStream in = new FileInputStream(hpiArchive);
                try {
                    manifest = new Manifest(in);
                } catch (IOException e) {
                    throw new IOException("Failed to load " + hpiArchive, e);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(br);
                }
            } else {
                jarfile = new JarFile(hpiArchive);
                manifest = jarfile.getManifest();
            }
            final Attributes attributes = manifest.getMainAttributes();

            shortName = attributes.getValue("Short-Name");
            if (shortName == null) {
                attributes.getValue("Extension-Name");
            }
            if (shortName == null) {
                shortName = hpiArchive.getName();
                int idx = shortName.lastIndexOf('.');
                if (idx >= 0) {
                    shortName = shortName.substring(0, idx);
                }
            }

            wikiUrl = attributes.getValue("Url");

            longName = attributes.getValue("Long-Name");
            if (longName == null) {
                longName = shortName;
            }

            version = attributes.getValue("Plugin-Version");
            if (version == null) {
                version = attributes.getValue("Implementation-Version");
            }

            String deps = attributes.getValue("Plugin-Dependencies");
            if (deps != null) {
                for (String dep : deps.split(",")) {
                    dependencies.add(new Dependency(dep));
                }
            }
            if (jarfile != null) {
                jarfile.close();
            }
        }

        public boolean isFailedToLoad() {

            for (FailedPlugin p : Hudson.getInstance().getPluginManager().getFailedPlugins()) {
                if (p.name.equals(shortName)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEnabled() {
            File disabledMarker = new File(hpiArchive.getPath() + ".disabled");
            return !disabledMarker.exists() && !isFailedToLoad();
        }

        public void setEnable(boolean enable) throws IOException {
            File disabledMarker = new File(hpiArchive.getPath() + ".disabled");
            if (enable && disabledMarker.exists()) {
                FileUtils.deleteQuietly(disabledMarker);
            }
            if (!enable && !disabledMarker.exists()) {
                FileUtils.touch(disabledMarker);
            }
        }

        public boolean isDowngrdable() throws IOException {
            File backupFile = Util.changeExtension(hpiArchive, ".bak");
            if (backupFile.exists()) {
                InstalledPluginInfo bakPluginInfo = new InstalledPluginInfo(backupFile);
                return !bakPluginInfo.version.trim().equals(version);
            }
            return false;
        }

        public String getBackupVersion() throws IOException {
            File backupFile = Util.changeExtension(hpiArchive, ".bak");
            if (backupFile.exists()) {
                InstalledPluginInfo bakPluginInfo = new InstalledPluginInfo(backupFile);
                return bakPluginInfo.version;
            }
            return "";
        }

        public boolean isPinned() {
            File pinnedMarker = new File(hpiArchive.getPath() + ".pinned");
            return pinnedMarker.exists();
        }

        public void unpin() {
            File pinnedMarker = new File(hpiArchive.getPath() + ".pinned");
            if (pinnedMarker.exists()) {
                FileUtils.deleteQuietly(pinnedMarker);
            }
        }

        public void downgade() throws IOException {
            File backupFile = Util.changeExtension(hpiArchive, ".bak");
            if (backupFile.exists()) {
                hpiArchive.delete();
            }
            if (!backupFile.renameTo(hpiArchive)) {
                throw new IOException("Failed to rename " + backupFile + " to " + hpiArchive);
            }
        }

        public VersionNumber getVersionNumber() {
            return new VersionNumber(version);
        }

        public boolean isOlderThan(VersionNumber v) {
            try {
                return getVersionNumber().compareTo(v) < 0;
            } catch (IllegalArgumentException e) {
                // Assume older
                return true;
            }
        }

        public String getLongName() {
            return longName;
        }

        public String getShortName() {
            return shortName;
        }

        public String getVersion() {
            return version;
        }

        public String getWikiUrl() {
            return wikiUrl;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }

        @Override
        public String toString() {
            return "[Plugin Name:" + shortName + " Long Name:" + longName + " Version:" + version + " Wiki:" + wikiUrl + "]";
        }

        private File resolve(File base, String relative) {
            File rel = new File(relative);
            if (rel.isAbsolute()) {
                return rel;
            } else {
                return new File(base.getParentFile(), relative);
            }
        }
    }
}
