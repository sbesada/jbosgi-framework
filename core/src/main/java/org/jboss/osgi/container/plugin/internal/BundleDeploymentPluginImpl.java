/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.container.plugin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoader;
import org.jboss.osgi.container.bundle.BundleManager;
import org.jboss.osgi.container.bundle.ModuleActivatorBridge;
import org.jboss.osgi.container.loading.VirtualFileResourceLoader;
import org.jboss.osgi.container.plugin.AbstractPlugin;
import org.jboss.osgi.container.plugin.BundleDeploymentPlugin;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.internal.OSGiManifestMetaData;
import org.jboss.osgi.modules.ModuleXmlParser;
import org.jboss.osgi.modules.ModuleXmlParser.ResourceLoaderFactory;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * A plugin the handles Bundle deployments.
 * 
 * @author thomas.diesler@jboss.com
 * @since 12-Jul-2010
 */
public class BundleDeploymentPluginImpl extends AbstractPlugin implements BundleDeploymentPlugin
{
   // Provide logging
   private static final Logger log = Logger.getLogger(BundleDeploymentPluginImpl.class);

   public BundleDeploymentPluginImpl(BundleManager bundleManager)
   {
      super(bundleManager);
   }

   /**
    * Create a {@link Deployment} from the given virtual file.
    * @param rootFile The root file pointing to one of the supported bundle formats
    * @param location The bundle location to be associated with the deployment  
    * @throws BundleException If the given root file does not 
    */
   public Deployment createDeployment(final VirtualFile rootFile, final String location) throws BundleException
   {
      if (rootFile == null)
         throw new IllegalArgumentException("Null rootFile");

      // Try valid OSGi Manifest
      try
      {
         BundleInfo info = BundleInfo.createBundleInfo(rootFile, location);
         String symbolicName = info.getSymbolicName();
         Version version = info.getVersion();
         Deployment dep = DeploymentFactory.createDeployment(rootFile, location, symbolicName, version);
         dep.addAttachment(BundleInfo.class, info);
         return dep;
      }
      catch (NumberFormatException rte)
      {
         throw new BundleException("Invalid OSGi version", rte);
      }
      catch (BundleException ex)
      {
         log.warn("Cannot process as OSGi deployment: " + location);
      }

      // Try module.xml
      try
      {
         VirtualFile child = rootFile.getChild("META-INF/module.xml");
         if (child != null)
         {
            InputStream inputStream = child.openStream();

            ResourceLoaderFactory factory = new ResourceLoaderFactory()
            {
               @Override
               public ResourceLoader getResourceLoader(String path, String name) throws IOException
               {
                  return new VirtualFileResourceLoader(rootFile, Collections.singletonList(path));
               }
            };
            
            ModuleSpec moduleSpec = ModuleXmlParser.parse(factory, inputStream);
            ModuleIdentifier identifier = moduleSpec.getIdentifier();
            String symbolicName = identifier.getArtifact();
            String version = identifier.getVersion();
            Deployment dep = DeploymentFactory.createDeployment(rootFile, location, symbolicName, Version.parseVersion(version));
            dep.addAttachment(ModuleSpec.class, moduleSpec);
            return dep;
         }
      }
      catch (IOException ex)
      {
         log.warn("Cannot process META-INF/module.xml: " + location, ex);
      }
      catch (ModuleLoadException ex)
      {
         log.warn("Cannot process META-INF/module.xml: " + location, ex);
      }

      throw new BundleException("Cannot process as OSGi deployment: " + rootFile);
   }

   @Override
   public OSGiMetaData createOSGiMetaData(Deployment dep) throws BundleException
   {
      OSGiMetaData metadata = null;

      // First check if the Deployment contains a valid BundleInfo
      BundleInfo info = dep.getAttachment(BundleInfo.class);
      if (info != null)
         metadata = toOSGiMetaData(info);

      // Secondly, we support deployments that contain ModuleSpec
      ModuleSpec moduleSpec = dep.getAttachment(ModuleSpec.class);
      if (metadata == null && moduleSpec != null)
         metadata = toOSGiMetaData(moduleSpec);

      if (metadata == null)
         throw new BundleException("Cannot construct OSGiMetaData from: " + dep);

      return metadata;
   }

   private OSGiMetaData toOSGiMetaData(BundleInfo info)
   {
      Manifest manifest = info.getManifest();
      return new OSGiManifestMetaData(manifest);
   }

   private OSGiMetaData toOSGiMetaData(ModuleSpec moduleSpec)
   {
      OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
      ModuleIdentifier identifier = moduleSpec.getIdentifier();
      builder.addBundleSymbolicName(identifier.getArtifact());
      builder.addBundleVersion(identifier.getVersion());

      // Set the module activator bridge
      if (moduleSpec.getMainClass() != null)
         builder.addBundleActivator(ModuleActivatorBridge.class);

      for (DependencySpec depSpec : moduleSpec.getDependencies())
      {
         ModuleIdentifier depid = depSpec.getModuleIdentifier();
         String name = depid.getArtifact();
         String version = depid.getVersion();
         boolean optional = depSpec.isOptional();

         // Require-Bundle
         StringBuffer buffer = new StringBuffer(name);
         if (version != null)
            buffer.append(";bundle-version=" + Version.parseVersion(version));
         if (optional == true)
            buffer.append(";resolution:=optional");
         builder.addRequireBundle(buffer.toString());
      }

      Manifest manifest = builder.getManifest();
      return new OSGiManifestMetaData(manifest);
   }
}