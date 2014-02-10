/*******************************************************************************
 * Copyright (c) 2014 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.internal.navigator.resources.nestedProjects;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

/**
 * @since 3.3
 *
 */
public class NestedProjectUtils {

	private static Map<IProject, IFolder> placeholders;

	public static void registerProjectShownInFolder(IFolder folder, IProject project) {
		if (placeholders == null) {
			placeholders = new HashMap<IProject, IFolder>();
		}
		placeholders.put(project, folder);
	}

	public static boolean isShownAsProject(IFolder folder) {
		return placeholders != null && placeholders.containsValue(folder);
	}

	public static boolean isShownAsNested(IProject element) {
		return placeholders != null && placeholders.containsValue(element);
	}

	/**
	 * @param element
	 * @return
	 */
	public static IFolder getAssociatedFolder(IProject element) {
		if (placeholders != null) {
			return placeholders.get(element);
		}
		return null;
	}
	
}
