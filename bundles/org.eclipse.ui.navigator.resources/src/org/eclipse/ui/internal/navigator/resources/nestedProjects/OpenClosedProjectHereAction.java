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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorMessages;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * @since 3.3
 *
 */
public class OpenClosedProjectHereAction extends Action {

	private CommonViewer viewer;
	private IFolder targetFolder;
	private IProject project;

	public OpenClosedProjectHereAction(IFolder targetFolder, IProject project, CommonViewer viewer) {
		super(WorkbenchNavigatorMessages.OpenProjectHere);
		this.targetFolder = targetFolder;
		this.project = project;
		this.viewer = viewer;
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(SharedImages.IMG_OBJ_PROJECT));
	}
	
	public void run() {
		try {
			project.open(new NullProgressMonitor());
			NestedProjectUtils.registerProjectShownInFolder(targetFolder, project);
			viewer.refresh(project);
			viewer.refresh(targetFolder.getParent());
			viewer.setSelection(new StructuredSelection(project));
		} catch (Exception ex) {
		}
	}
}
