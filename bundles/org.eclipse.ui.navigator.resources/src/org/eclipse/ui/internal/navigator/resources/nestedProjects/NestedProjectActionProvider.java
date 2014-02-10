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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
 * @since 3.3
 *
 */
public class NestedProjectActionProvider extends CommonActionProvider {

	private CommonViewer viewer;

	@Override
	public void init(ICommonActionExtensionSite anActionSite) {
		this.viewer = (CommonViewer)anActionSite.getStructuredViewer();
	}


	public void fillContextMenu(IMenuManager aMenu) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		if (selection.size() != 1) {
			return;	
		}
		Object object = selection.getFirstElement();
		if (! (object instanceof IFolder)) {
			return;	
		}
		IFolder folder = (IFolder) object;
		if (! folder.getFile(IProjectDescription.DESCRIPTION_FILE_NAME).exists()) {
			return;
		}
		
		for (IProject project : folder.getWorkspace().getRoot().getProjects()) {
			if (project.getLocation().equals(folder.getLocation())) {
				Action action = null;
				if (project.isOpen()) {
					action = new ShowProjectHereAction(project, folder, this.viewer);
				} else {
					action = new OpenClosedProjectHereAction(folder, project, this.viewer);
				}
				aMenu.insertAfter(ICommonMenuConstants.GROUP_OPEN, action);
				return;
			}
		}
		aMenu.insertAfter(ICommonMenuConstants.GROUP_OPEN, new OpenNewProjectHereAction(folder, viewer));
	}
}
