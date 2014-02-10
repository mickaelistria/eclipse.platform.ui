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

import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.internal.resources.ProjectDescriptionReader;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorMessages;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * @since 3.3
 *
 */
public class OpenNewProjectHereAction extends Action {

	private CommonViewer viewer;
	private IFolder targetFolder;

	public OpenNewProjectHereAction(IFolder targetFolder, CommonViewer viewer) {
		super(WorkbenchNavigatorMessages.OpenProjectHere);
		this.targetFolder = targetFolder;
		this.viewer = viewer;
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(SharedImages.IMG_OBJ_PROJECT));
	}
	
	public void run() {
		try {
			IProjectDescription desc = new ProjectDescriptionReader().read(targetFolder.getLocation().append(IProjectDescription.DESCRIPTION_FILE_NAME));
			desc.setLocation(targetFolder.getLocation());
			CreateProjectOperation operation = new CreateProjectOperation(desc, desc.getName());
			OperationHistoryFactory.getOperationHistory().execute(operation, null, null);
			IProject project = (IProject) operation.getAffectedObjects()[0];;
			NestedProjectUtils.registerProjectShownInFolder(targetFolder, project);
			viewer.refresh(targetFolder);
			viewer.refresh(targetFolder.getParent());
			viewer.setSelection(new StructuredSelection(project));
		} catch (Exception ex) {
		}
	}
}
