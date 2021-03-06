package org.jboss.test.osgi.framework.localization;
/*
 * #%L
 * JBossOSGi Framework
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Locale;

import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.osgi.testing.OSGiFrameworkTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Test the Localization
 *
 * @author thomas.diesler@jboss.com
 * @since 25-Jan-2010
 */
public class LocalizationTestCase extends OSGiFrameworkTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testHostLocalization() throws Exception {
        Bundle host = installBundle(getHostArchive("simple-hostA"));
        assertBundleState(Bundle.INSTALLED, host.getState());

        // Test default locale
        Dictionary<String, String> headers = host.getHeaders();
        String bundleName = headers.get(Constants.BUNDLE_NAME);
        assertEquals("English Bundle Name", bundleName);

        // Test explicit default locale
        headers = host.getHeaders(null);
        bundleName = headers.get(Constants.BUNDLE_NAME);
        assertEquals("English Bundle Name", bundleName);

        // Test raw headers
        headers = host.getHeaders("");
        bundleName = headers.get(Constants.BUNDLE_NAME);
        assertEquals("%bundle-name", bundleName);

        host.uninstall();
        assertBundleState(Bundle.UNINSTALLED, host.getState());

        // Test default locale after uninstall
        headers = host.getHeaders();
        bundleName = headers.get(Constants.BUNDLE_NAME);
        assertEquals("English Bundle Name", bundleName);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFragmentLocalization() throws Exception {
        Bundle host = installBundle(getHostArchive("localization-hostB"));
        Bundle frag = installBundle(getFragmentArchive("localization-hostB"));

        host.start();
        assertBundleState(Bundle.ACTIVE, host.getState());
        assertBundleState(Bundle.RESOLVED, frag.getState());

        // Test explicit locale
        Dictionary<String, String> headers = host.getHeaders(Locale.GERMAN.toString());
        String bundleName = headers.get(Constants.BUNDLE_NAME);
        assertEquals("Deutscher Bundle Name", bundleName);

        host.uninstall();
        assertBundleState(Bundle.UNINSTALLED, host.getState());

        frag.uninstall();
        assertBundleState(Bundle.UNINSTALLED, frag.getState());

        // Test default locale after uninstall
        headers = host.getHeaders();
        bundleName = headers.get(Constants.BUNDLE_NAME);
        assertEquals("English Bundle Name", bundleName);
    }

    private JavaArchive getHostArchive(String hostName) {
        // Bundle-SymbolicName: localization-simple-host
        // Bundle-Name: %bundle-name
        // Include-Resource: OSGI-INF/l10n/bundle_en.properties=OSGI-INF/l10n/bundle_en.properties
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, hostName);
        archive.addAsResource(getResourceFile("localization/OSGI-INF/l10n/bundle_en.properties"), "OSGI-INF/l10n/bundle_en.properties");
        archive.setManifest(new Asset() {

            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleName("%bundle-name");
                return builder.openStream();
            }
        });
        return archive;
    }

    private JavaArchive getFragmentArchive(final String hostName) {
        // Bundle-SymbolicName: localization-simple-frag
        // Fragment-Host: localization-simple-host
        // Include-Resource: OSGI-INF/l10n/bundle_de.properties=OSGI-INF/l10n/bundle_de.properties
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "localization-fragment");
        archive.addAsResource(getResourceFile("localization/OSGI-INF/l10n/bundle_de.properties"), "OSGI-INF/l10n/bundle_de.properties");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addFragmentHost(hostName);
                return builder.openStream();
            }
        });
        return archive;
    }
}