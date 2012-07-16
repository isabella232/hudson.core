/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *  Kohsuke Kawaguchi, Anton Kozak
 *
 *******************************************************************************/ 

package hudson;

import javax.tools.Diagnostic;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner6;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

/**
 * This is moved from maven-hpi-plugin for annotation processing. Discovers the
 * type of {@link Plugin} and generates "META-INF/services/hudson.Plugin". It's
 * used for manifest creation (Plugin-Class section) and the plugin loading.
 * Custom {@link Plugin} implementation is required for the -plugin
 * initialization, for example XStream aliases, etc
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("*")
@SuppressWarnings({"Since15"})
public class PluginMarkerAPT6 extends AbstractProcessor {

    protected static final String HUDSON_PLUGIN_QUAFIFIER = "hudson.Plugin";
    protected static final String META_INF_SERVICES_LOCATION = "META-INF/services/hudson.Plugin";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ElementScanner6<Void, Void> scanner = new ElementScanner6<Void, Void>() {
            @Override
            public Void visitType(TypeElement e, Void aVoid) {
                if (!e.getModifiers().contains(Modifier.ABSTRACT)) {
                    Element sc = processingEnv.getTypeUtils().asElement(e.getSuperclass());
                    if (sc != null && ((TypeElement) sc).getQualifiedName().contentEquals(HUDSON_PLUGIN_QUAFIFIER)) {
                        try {
                            write(e);
                        } catch (IOException x) {
                            StringWriter sw = new StringWriter();
                            x.printStackTrace(new PrintWriter(sw));
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sw.toString(), e);
                        }
                    }
                }

                return super.visitType(e, aVoid);
            }
        };

        for (Element e : roundEnv.getRootElements()) {
            scanner.scan(e, null);
        }

        return false;
    }

    /**
     * Writes plugin class name to the defined location.
     *
     * @param typeElement detected element.
     * @throws IOException exception.
     */
    private void write(TypeElement typeElement) throws IOException {
        FileObject f = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                "", META_INF_SERVICES_LOCATION);
        Writer w = new OutputStreamWriter(f.openOutputStream(), "UTF-8");
        try {
            w.write(typeElement.getQualifiedName().toString());
        } finally {
            w.close();
        }
    }
}
