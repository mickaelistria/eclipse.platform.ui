/*******************************************************************************
 * Copyright (c) 2007, 2014 Gunnar Wagenknecht and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mickael Istria (Red Hat Inc) - [102527] Reshaped UI
 ******************************************************************************/

package org.eclipse.ui.internal.ide.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;

/**
 * Project property page for viewing and modifying the project natures.
 * 
 * @since 3.3
 */
public class ProjectNaturesPage extends PropertyPage {
//	private static final int NATURES_LIST_MULTIPLIER = 30;

	private IProject project;

	private boolean modified = false;

	// widgets
	private ListViewer listViewer;
	
	private List<String> naturesIdsWorkingCopy;

	/**
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				IIDEHelpContextIds.PROJECT_NATURES_PROPERTY_PAGE);
		Font font = parent.getFont();

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(font);

		initialize();

		Label description = createDescriptionLabel(composite);
		description.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		listViewer = new ListViewer(composite);
		listViewer.getList().setFont(font);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;

		listViewer.getList().setLayoutData(data);

		listViewer.setLabelProvider(new NatureLabelProvider(this.project.getWorkspace()));
		listViewer.setContentProvider(new ArrayContentProvider());
//		listViewer.setComparator(getViewerComperator());
		try {
			this.naturesIdsWorkingCopy = Arrays.asList(project.getDescription().getNatureIds());
			
		} catch (CoreException ex) {
			IDEWorkbenchPlugin.getDefault().getLog().log(new Status(IStatus.WARNING,
					IDEWorkbenchPlugin.getDefault().getBundle().getSymbolicName(),
					"Error while loading project description for " + project.getName(), //$NON-NLS-1$
					ex));	
		}
		listViewer.setInput(this.naturesIdsWorkingCopy);
		
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(1, false));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButton.setText(IDEWorkbenchMessages.ProjectNaturesPage_addNature);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO
				ProjectNaturesPage.this.modified = true;
			}
		});
		final Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeButton.setText(IDEWorkbenchMessages.ProjectNaturesPage_removeNature);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection)listViewer.getSelection();
				for (Object item : selection.toList()) {
					String natureId = (String) item;
					naturesIdsWorkingCopy.remove(natureId);
				}
				ProjectNaturesPage.this.modified = true;
				listViewer.refresh();
			}
		});
		this.listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(!listViewer.getSelection().isEmpty());
			}
		});
		this.listViewer.setSelection(new StructuredSelection()); // Empty selection

		return composite;
	}

//	private static class ProjectNaturesContentProvider extends ArrayContentProvider {
//		public Object[] getChildren(Object input) {
//			if (! (input instanceof IProject)) {
//				return new Object[0];
//			}
//			
//			IProject project get
//			
//			// collect all the natures
//			IProjectNatureDescriptor[] natureDescriptors = ((IWorkspace) o)
//					.getNatureDescriptors();
//			List elements = new ArrayList(natureDescriptors.length);
//			Set natureIds = new HashSet(natureDescriptors.length);
//			for (int i = 0; i < natureDescriptors.length; i++) {
//				elements.add(natureDescriptors[i]);
//				natureIds.add(natureDescriptors[i].getNatureId());
//			}
//
//			// Add any natures that do not exist in the workbench currently
//			try {
//				String[] projectNatureIds = project.getDescription()
//						.getNatureIds();
//				for (int i = 0; i < projectNatureIds.length; i++) {
//					if (!natureIds.contains(projectNatureIds[i]))
//						elements.add(projectNatureIds[i]);
//				}
//			} catch (CoreException e) {
//				// Ignore core exceptions
//			}
//
//			return elements.toArray();
//		}
//
//		/* (non-Javadoc)
//		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
//		 */
//		public void dispose() {
//			// TODO Auto-generated method stub
//			
//		}
//
//		/* (non-Javadoc)
//		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
//		 */
//		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
//		}
//
//		/* (non-Javadoc)
//		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
//		 */
//		public Object[] getElements(Object inputElement) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}
//
	private static class NatureLabelProvider extends LabelProvider {
		private IWorkspace workspace;
		private Map<String, Image> natureImages;
		
		public NatureLabelProvider(IWorkspace workspace) {
			this.workspace = workspace;
			this.natureImages = new HashMap<String, Image>(workspace.getNatureDescriptors().length);
		}
		
		@Override
		public String getText(Object element) {
			IProjectNatureDescriptor nature = null;
			if (element instanceof IProjectNatureDescriptor) {
				nature = (IProjectNatureDescriptor) element;
			} else if (element instanceof String) {
				String natureId = (String) element;
				nature = this.workspace.getNatureDescriptor(natureId);
				if (nature == null) {
					return getMissingNatureLabel(natureId);
				}
			} else {
				return "Not a valid nature input " + element.toString(); //$NON-NLS-1$
			}
			return getNatureDescriptorLabel((IProjectNatureDescriptor) element);
		}
		
		@Override
		public Image getImage(Object element) {
			String natureId = null;
			if (element instanceof IProjectNatureDescriptor) {
				natureId = ((IProjectNatureDescriptor) element).getNatureId();
			} else if (element instanceof String) {
				natureId = (String) element;
			} else {
				// TODO find an error icon
				return null;
			}
			if (this.workspace.getNatureDescriptor(natureId) != null) {
				if (!natureImages.containsKey(natureId)) {
					this.natureImages.put(natureId, IDEWorkbenchPlugin.getDefault().getProjectImageRegistry().getNatureImage(natureId).createImage());
				}
				return natureImages.get(natureId);
			} else {
				// TODO warning logo
				return null;
			}
			
		}
		
		protected String getMissingNatureLabel(String natureId) {
			return NLS.bind(
					IDEWorkbenchMessages.ProjectNaturesPage_missingNatureText,
					natureId);
		}

		protected String getNatureDescriptorLabel(
				IProjectNatureDescriptor natureDescriptor) {
			String label = natureDescriptor.getLabel();
			if (label.trim().length() == 0)
				return natureDescriptor.getNatureId();
			return label;
		}
		
		@Override
		public void dispose() {
			for (Image image : natureImages.values()) {
				image.dispose();
			}
			super.dispose();
		}

	}
	
//	private ViewerComparator getViewerComperator() {
//		return new ViewerComparator(new Comparator() {
//
//			public int compare(Object element1, Object element2) {
//				int val1 = 0;
//				int val2 = 0;
//
//				if (element1 instanceof IProjectNatureDescriptor) {
//					element1 = getNatureDescriptorLabel((IProjectNatureDescriptor) element1);
//				} else {
//					val1 += element1.getClass().hashCode();
//				}
//				if (element2 instanceof IProjectNatureDescriptor) {
//					element2 = getNatureDescriptorLabel((IProjectNatureDescriptor) element1);
//				} else {
//					val2 += element2.getClass().hashCode();
//				}
//
//				int result = val1 - val2;
//				if (result != 0)
//					return result;
//
//				return String.valueOf(element1).compareTo(
//						String.valueOf(element2));
//			}
//
//		});
//	}

	/**
	 * Handle the exception thrown when saving.
	 * 
	 * @param e
	 *            the exception
	 */
	protected void handle(InvocationTargetException e) {
		IStatus error;
		Throwable target = e.getTargetException();
		if (target instanceof CoreException) {
			error = ((CoreException) target).getStatus();
		} else {
			String msg = target.getMessage();
			if (msg == null) {
				msg = IDEWorkbenchMessages.Internal_error;
			}
			error = new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH,
					1, msg, target);
		}
		ErrorDialog.openError(getControl().getShell(), null, null, error);
	}

	/**
	 * Initializes a ProjectReferencePage.
	 */
	private void initialize() {
		project = (IProject) getElement().getAdapter(IResource.class);
		noDefaultAndApplyButton();
		setDescription(NLS.bind(IDEWorkbenchMessages.ProjectNaturesPage_label,
				project.getName()));
	}

	/**
	 * @see PreferencePage#performOk
	 */
	public boolean performOk() {
		if (!this.modified) {
			return true;
		}

		// set nature ids
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {

				try {
					IProjectDescription description = project.getDescription();
					description.setNatureIds(ProjectNaturesPage.this.naturesIdsWorkingCopy.toArray(new String[ProjectNaturesPage.this.naturesIdsWorkingCopy.size()]));
					project.setDescription(description, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			new ProgressMonitorJobsDialog(getControl().getShell()).run(true,
					true, runnable);
		} catch (InterruptedException e) {
			// Ignore interrupted exceptions
		} catch (InvocationTargetException e) {
			handle(e);
			return false;
		}
		return true;
	}
}
