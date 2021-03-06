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
package org.springframework.osgi.iandt.proxied.listener;

import java.awt.Shape;
import java.awt.geom.Area;

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.osgi.iandt.proxy.listener.Listener;

/**
 * @author Costin Leau
 */
public class ProxiedListenerTest extends BaseIntegrationTest {

	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/proxied/listener/service-import.xml" };
	}

	protected String[] getTestBundlesNames() {
		return new String[] { "org.springframework.osgi.iandt, proxy.listener," + getSpringDMVersion() };
	}

	public void testListenerProxy() throws Exception {
		System.out.println(Listener.class.getName());
		Object obj = new Area();
		ServiceRegistration reg = bundleContext.registerService(Shape.class.getName(), obj, null);
		reg.unregister();
	}
}
