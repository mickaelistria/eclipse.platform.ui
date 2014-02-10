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

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * This class has 2 goals
 * <ul>
 * <li>hide folders when they are actually nested projects (folder is shown as project instead</li>
 * <li>Replace root projects by "ProjectReference" when the project are nested someplace else</li>
 * </ul>
 * @since 3.3
 *
 */
public class NestedProjectsFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IFolder) {
			if (NestedProjectUtils.isShownAsProject((IFolder)element)) {
				return false;
			}
		}
//		if (element instanceof IProject) {
//			IProject project = (IProject)element;
//			if (parentElement != null && NestedProjectUtils.isNested(project)) {
//				return false;
//			}
//		}
		return true;
	}

}
