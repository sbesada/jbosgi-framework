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
package org.jboss.osgi.msc.plugin.internal;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jboss.osgi.msc.bundle.BundleManager;
import org.jboss.osgi.msc.bundle.HostBundle;
import org.jboss.osgi.msc.bundle.ModuleManager;
import org.jboss.osgi.msc.plugin.AbstractPlugin;
import org.jboss.osgi.msc.plugin.ModuleManagerPlugin;
import org.jboss.osgi.resolver.XModule;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.Bundle;

/**
 * The module manager plugin.
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Jul-2009
 */
public class ModuleManagerPluginImpl extends AbstractPlugin implements ModuleManagerPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(ModuleManagerPluginImpl.class);

   // The module manager
   private ModuleManager moduleManager;

   public ModuleManagerPluginImpl(BundleManager bundleManager)
   {
      super(bundleManager);
      moduleManager = new ModuleManager();
   }

   @Override
   public Module loadModule(XModule resModule) throws ModuleLoadException
   {
      if (resModule == null)
         throw new IllegalArgumentException("Null module");
      
      Module module;
      if (resModule.getModuleId() == 0)
      {
         module = moduleManager.createFrameworkModule(resModule);
      }
      else
      {
         // Get the root virtual file
         Bundle bundle = resModule.getAttachment(Bundle.class);
         HostBundle bundleState = HostBundle.assertBundleState(bundle);
         VirtualFile rootFile = bundleState.getRootFile();

         ModuleSpec moduleSpec = moduleManager.createModuleSpec(resModule, rootFile);
         module = moduleManager.createModule(moduleSpec);
         bundleState.setModule(module);
      }

      return module;
   }
}