/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.views.markers.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.SelectionProviderAction;

public class ActionTaskProperties extends SelectionProviderAction {

	private IWorkbenchPart part;

	public ActionTaskProperties(IWorkbenchPart part, ISelectionProvider provider) {
		super(provider, Messages.getString("propertiesAction.title")); //$NON-NLS-1$
		setEnabled(false);
		this.part = part;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (!isEnabled()) {
			return;
		}
		Object obj = getStructuredSelection().getFirstElement();
		if (!(obj instanceof IMarker)) {
			return;
		}
		IMarker marker = (IMarker) obj;
		DialogMarkerProperties dialog = new DialogTaskProperties(part.getSite().getShell());
		dialog.setMarker(marker);
		dialog.open();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(selection != null && selection.size() == 1);
	}
}
