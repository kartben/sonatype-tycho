/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package p1.test;

import java.util.Collections;
import java.util.Enumeration;

import junit.framework.TestCase;
import p1.Activator;

public class ATest extends TestCase {

	public void testSuccess() {
		assertEquals("p1.Activator", Activator.class.getName());
		Enumeration<String> ss = Activator.getDefault().getBundle().getEntryPaths("/");
		System.out.println(ss);
		assertNotNull("resource.txt not found", Activator.getDefault().getBundle().getResource("/resource.txt"));
		assertNotNull("resource-fragment.txt not found", Activator.getDefault().getBundle().getResource("/resource-fragment.txt"));
	}
}
