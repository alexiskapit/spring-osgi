/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.osgi.extender.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.exporter.OsgiServicePropertiesResolver;

/**
 * Mock bundle useful for testing service dependencies.
 * 
 * @author Costin Leau
 * 
 */
public class DependencyMockBundle extends MockBundle {

	// bundles which depend on the current one
	protected List<Bundle> dependentOn = new ArrayList<Bundle>();

	// bundles on which the current bundle depends on
	protected List<Bundle> dependsOn = new ArrayList<Bundle>();

	private Map<Bundle, ServiceReference> inUseServices = new LinkedHashMap<Bundle, ServiceReference>();

	private Map<Bundle, ServiceReference> registeredServices = new LinkedHashMap<Bundle, ServiceReference>();

	public DependencyMockBundle() {
		super();
	}

	public DependencyMockBundle(BundleContext context) {
		super(context);
	}

	public DependencyMockBundle(Dictionary headers) {
		super(headers);
	}

	public DependencyMockBundle(String location, Dictionary headers, BundleContext context) {
		super(location, headers, context);
	}

	public DependencyMockBundle(String location) {
		super(location);
	}

	private Dictionary createProps(int index, int[] serviceRanking, long[] serviceId) {
		// set Properties
		Dictionary props = new Properties();

		props.put(Constants.SERVICE_RANKING, new Integer((index < serviceRanking.length ? serviceRanking[index]
				: serviceRanking[0])));
		long id = (index < serviceId.length ? serviceId[index] : serviceId[0]);
		if (id >= 0)
			props.put(Constants.SERVICE_ID, new Long(id));

		props.put(OsgiServicePropertiesResolver.BEAN_NAME_PROPERTY_KEY, new Long(id));

		return props;
	}

	/**
	 * Create one service reference returning the using bundle.
	 * 
	 * @param dependent
	 */
	public void setDependentOn(final Bundle[] dependents, int[] serviceRanking, long[] serviceId) {
		this.dependentOn.addAll(Arrays.asList(dependents));

		for (Bundle dependent : dependents) {
			if (dependent instanceof DependencyMockBundle) {
				((DependencyMockBundle) dependent).dependsOn.add(this);
			}
		}

		// initialise registered services
		registeredServices.clear();

		for (int i = 0; i < dependents.length; i++) {
			registeredServices.put(dependents[i], new MockServiceReference(DependencyMockBundle.this, createProps(i,
					serviceRanking, serviceId), null) {

				public Bundle[] getUsingBundles() {
					return DependencyMockBundle.this.dependentOn.toArray(new Bundle[dependentOn.size()]);
				}
			});
		}
	}

	public void setDependentOn(final Bundle[] dependent, int serviceRanking, long serviceId) {
		setDependentOn(dependent, new int[] { serviceRanking }, new long[] { serviceId });
	}

	public void setDependentOn(final Bundle[] dependent) {
		setDependentOn(dependent, 0, -1);
	}

	public void setDependentOn(Bundle dependent) {
		setDependentOn(new Bundle[] { dependent }, 0, -1);
	}

	public void setDependentOn(Bundle dependent, int serviceRanking, long serviceId) {
		setDependentOn(new Bundle[] { dependent }, serviceRanking, serviceId);
	}

	protected void setDependsOn(Bundle[] depends) {
		this.dependsOn.addAll(Arrays.asList(depends));

		// initialize InUseServices
		inUseServices.clear();

		final Bundle[] usingBundles = new Bundle[] { this };

		for (final Bundle dependencyBundle : dependsOn) {
			// make connection from the opposite side also
			if (dependencyBundle instanceof DependencyMockBundle) {
				((DependencyMockBundle) dependencyBundle).setDependentOn(this);
			}

			Properties props = new Properties();

			props.put(OsgiServicePropertiesResolver.BEAN_NAME_PROPERTY_KEY, new Long(System
					.identityHashCode(dependencyBundle)));

			inUseServices.put(dependencyBundle, new MockServiceReference() {
				public Bundle getBundle() {
					return dependencyBundle;
				}

				public Bundle[] getUsingBundles() {
					return usingBundles;
				}
			});
		}
	}

	protected void setDependsOn(Bundle depends) {
		setDependsOn(new Bundle[] { depends });
	}

	public ServiceReference[] getRegisteredServices() {
		return registeredServices.values().toArray(new ServiceReference[registeredServices.size()]);
	}

	public ServiceReference[] getServicesInUse() {
		return inUseServices.values().toArray(new ServiceReference[registeredServices.size()]);
	}

	@Override
	public void stop(int options) throws BundleException {
		if (dependentOn != null)
			for (Bundle dependent : dependentOn) {
				if (dependent instanceof DependencyMockBundle) {
					DependencyMockBundle dep = ((DependencyMockBundle) dependent);
					List<Bundle> list = dep.dependsOn;
					if (list != null)
						list.remove(this);
					dep.inUseServices.remove(dependent);
				}
			}
		dependentOn = null;

		if (dependsOn != null)
			for (Bundle dependent : dependsOn) {
				if (dependent instanceof DependencyMockBundle) {
					DependencyMockBundle dep = ((DependencyMockBundle) dependent);
					List<Bundle> list = dep.dependentOn;
					if (list != null)
						list.remove(this);
				}
			}
		dependsOn = null;
		inUseServices.clear();
		registeredServices.clear();
	}
}