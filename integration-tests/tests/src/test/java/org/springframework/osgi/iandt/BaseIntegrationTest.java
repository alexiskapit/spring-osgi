/*
 * Copyright 2006-2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.osgi.iandt;

import java.io.File;

import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.test.provisioning.ArtifactLocator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base test class used for improving performance of integration tests by
 * creating bundles only with the classes within a package as opposed to all
 * resources available in the target folder.
 * 
 * <p/> Additionally, the class checks for the presence Clover if a certain
 * property is set and uses a special setup to use the instrumented jars instead
 * of the naked ones.
 * 
 * @author Costin Leau
 * 
 */
public abstract class BaseIntegrationTest extends AbstractConfigurableBundleCreatorTests {

	private class CloverClassifiedArtifactLocator implements ArtifactLocator {

		private final ArtifactLocator delegate;


		public CloverClassifiedArtifactLocator(ArtifactLocator delegate) {
			this.delegate = delegate;
		}

		public Resource locateArtifact(String group, String id, String version, String type) {
			return parse(id + "-" + version, delegate.locateArtifact(group, id, version, type));
		}

		public Resource locateArtifact(String group, String id, String version) {
			return parse(id + "-" + version, delegate.locateArtifact(group, id, version));
		}

		private Resource parse(String id, Resource resource) {
			if (id.contains(SPRING_DM_PREFIX)) {
				try {
					String relativePath = "";
					// check if it's a relative file
					if (StringUtils.cleanPath(resource.getURI().toString()).contains("/target/")) {
						relativePath = "clover" + File.separator;
					}
					relativePath = relativePath + id + "-clover.jar";

					Resource res = resource.createRelative(relativePath);
					BaseIntegrationTest.this.logger.info("Using clover instrumented jar " + res.getDescription());
					return res;
				}
				catch (Exception ex) {
					throw new IllegalStateException(
						"Trying to find Clover instrumented class but none is available; disable clover or build the instrumented artifacts",
						ex);
				}
			}
			return resource;
		}
	}


	private static final String CLOVER_PROPERTY = "org.springframework.osgi.integration.testing.clover";

	private static final String CLOVER_PKG = "com_cenqua_clover";

	private static final String SPRING_DM_PREFIX = "spring-osgi";


	//	protected Manifest getManifest() {
	//		Manifest mf = super.getManifest();
	//		String imports = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
	//		if (!imports.contains("org.springframework.osgi.test.provisioning")) {
	//			imports = imports + ",org.springframework.osgi.test.provisioning,org.springframework.osgi.test";
	//			mf.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, imports);
	//		}
	//		System.out.println("new manifest is " + mf.getMainAttributes().entrySet());
	//		return mf;
	//	}

	protected String[] getBundleContentPattern() {
		String pkg = getClass().getPackage().getName().replace('.', '/').concat("/");
		String[] patterns = new String[] { BaseIntegrationTest.class.getName().replace('.', '/').concat("*.class"),
			pkg + "**/*" };
		return patterns;
	}

	private boolean isCloverEnabled() {
		return (System.getProperty(CLOVER_PROPERTY) != null);
	}

	protected String[] getTestFrameworkBundlesNames() {
		String[] names = super.getTestFrameworkBundlesNames();
		if (isCloverEnabled()) {
			return (String[]) ObjectUtils.addObjectToArray(names, "org.springframework.iandt,clover.bundle,"
					+ getSpringDMVersion());
		}
		return names;
	}

	protected ArtifactLocator getLocator() {
		ArtifactLocator defaultLocator = super.getLocator();
		// redirect to the clover artifacts
		if (isCloverEnabled()) {
			return new CloverClassifiedArtifactLocator(defaultLocator);
		}
		return defaultLocator;
	}
}
