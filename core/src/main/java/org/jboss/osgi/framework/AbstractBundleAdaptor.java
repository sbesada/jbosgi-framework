package org.jboss.osgi.framework;
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

import static org.jboss.osgi.framework.internal.FrameworkMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XEnvironment;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;

/**
 * An abstract implementation that adapts a {@link Module} to a {@link Bundle}
 *
 * @author thomas.diesler@jboss.com
 * @since 30-May-2012
 */
public class AbstractBundleAdaptor implements XBundle {

    private final BundleContext context;
    private final XBundleRevision brev;
    private final Module module;
    private long lastModified;
    private int bundleState;

    public AbstractBundleAdaptor(BundleContext context, Module module, XBundleRevision brev) {
        if (context == null)
            throw MESSAGES.illegalArgumentNull("context");
        if (module == null)
            throw MESSAGES.illegalArgumentNull("module");
        if (brev == null)
            throw MESSAGES.illegalArgumentNull("brev");
        this.context = context;
        this.module = module;
        this.brev = brev;
        this.lastModified = System.currentTimeMillis();
        this.bundleState = Bundle.RESOLVED;
    }

    @Override
    public long getBundleId() {
        Long bundleId = brev.getAttachment(Long.class);
        return bundleId != null ? bundleId.longValue() : -1;
    }

    @Override
    public String getLocation() {
        return module.getIdentifier().toString();
    }

    @Override
    public String getSymbolicName() {
        return module.getIdentifier().getName();
    }

    @Override
    public int getState() {
        return bundleState;
    }

    @Override
    public Version getVersion() {
        String slot = module.getIdentifier().getSlot();
        try {
            return Version.parseVersion(slot);
        } catch (IllegalArgumentException ex) {
            return Version.emptyVersion;
        }
    }

    @Override
    public String getCanonicalName() {
        return getSymbolicName() + ":" + getVersion();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return module.getClassLoader().loadClass(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T adapt(Class<T> type) {
        T result = null;
        if (type == Module.class) {
            result = (T) module;
        } else if (type == BundleRevision.class) {
            result = (T) brev;
        }
        return result;
    }

    @Override
    public void start(int options) throws BundleException {
        throw MESSAGES.unsupportedBundleOpertaion(this);
    }

    @Override
    public void start() throws BundleException {
        throw MESSAGES.unsupportedBundleOpertaion(this);
    }

    @Override
    public void stop(int options) throws BundleException {
        throw MESSAGES.unsupportedBundleOpertaion(this);
    }

    @Override
    public void stop() throws BundleException {
        throw MESSAGES.unsupportedBundleOpertaion(this);
    }

    @Override
    public void update(InputStream input) throws BundleException {
        throw MESSAGES.unsupportedBundleOpertaion(this);
    }

    @Override
    public void update() throws BundleException {
        throw MESSAGES.unsupportedBundleOpertaion(this);
    }

    @Override
    public void uninstall() throws BundleException {
        XBundle sysbundle = (XBundle) context.getBundle();
        // Uninstall from the environment
        XEnvironment env = sysbundle.adapt(XEnvironment.class);
        env.uninstallResources(getBundleRevision());
        // Remove from the module loader
        BundleManager bundleManager = sysbundle.adapt(BundleManager.class);
        ServiceContainer serviceContainer = bundleManager.getServiceContainer();
        ServiceController<?> service = serviceContainer.getRequiredService(IntegrationService.MODULE_LOADER_PLUGIN);
        ModuleLoaderPlugin provider = (ModuleLoaderPlugin) service.getValue();
        provider.removeModule(brev, module.getIdentifier());
        bundleState = Bundle.UNINSTALLED;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getHeaders() {
        return getHeaders(null);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getHeaders(String locale) {
        // [TODO] Add support for manifest header related APIs on Module adaptors
        // https://issues.jboss.org/browse/JBOSGI-567
        return new Hashtable();
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        return null;
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        return null;
    }

    @Override
    public boolean hasPermission(Object permission) {
        return false;
    }

    @Override
    public URL getResource(String name) {
        return getBundleRevision().getResource(name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getResources(String name) throws IOException {
        return getBundleRevision().getResources(name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getEntryPaths(String path) {
        return getBundleRevision().getEntryPaths(path);
    }

    @Override
    public URL getEntry(String path) {
        return getBundleRevision().getEntry(path);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration findEntries(String path, String filePattern, boolean recurse) {
        return getBundleRevision().findEntries(path, filePattern, recurse);
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public BundleContext getBundleContext() {
        return context;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map getSignerCertificates(int signersType) {
        return Collections.emptyMap();
    }

    @Override
    public boolean isResolved() {
        return true;
    }

    @Override
    public boolean isFragment() {
        return getBundleRevision().isFragment();
    }

    @Override
    public XBundleRevision getBundleRevision() {
        return brev;
    }

    @Override
    public List<XBundleRevision> getAllBundleRevisions() {
        return Collections.singletonList(brev);
    }

    @Override
    public int hashCode() {
        return (int) getBundleId() * 51;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XBundle == false)
            return false;
        if (obj == this)
            return true;

        XBundle other = (XBundle) obj;
        return getBundleId() == other.getBundleId();
    }

    @Override
    public String toString() {
        return getCanonicalName();
    }
}
