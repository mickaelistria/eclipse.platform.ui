/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. (mistria) - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.dialogs;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;

/**
 * @since 3.4
 *
 */
public class ImportFolderAsProjectWizardPage extends WizardDataTransferPage {

	
	private File location;
	private String projectName;
	private WorkingSetGroup workingSetGroup;
	
	private Text locationText;
	private Text projectNameText;
	private Button referenceRadio;
	private Button copyRadio;
	private ControlDecoration locationFieldDecorator;
	private ControlDecoration projectNameFieldDecorator;
	
	/**
	 * @param pageName
	 */
	public ImportFolderAsProjectWizardPage(String pageName) {
		super(pageName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite content = new Composite(parent, SWT.NONE);
		content.setLayout(new GridLayout(1, false));
		Composite fieldsComposite = new Composite(content, SWT.NONE);
		fieldsComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fieldsComposite.setLayout(new GridLayout(3, false));
		createPlainLabel(fieldsComposite, DataTransferMessages.FileImport_selectSourceTitle);
		locationText = new Text(fieldsComposite, SWT.BORDER);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		Button browseButton = new Button(fieldsComposite, SWT.PUSH);
		browseButton.setText(IDEWorkbenchMessages.WizardImportPage_browse2);
		createPlainLabel(fieldsComposite, IDEWorkbenchMessages.WizardNewProjectCreationPage_nameLabel);
		projectNameText = new Text(fieldsComposite, SWT.BORDER);
		projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
		createSpacer(content);
		createPlainLabel(content, DataTransferMessages.DirectoryImportAsProject_strategy);
		referenceRadio = new Button(content, SWT.RADIO);
		referenceRadio.setText(DataTransferMessages.DirectoryImportAsProject_reference);
		copyRadio = new Button(content, SWT.RADIO);
		copyRadio.setText(DataTransferMessages.DirectoryImportAsProject_copy);
		createSpacer(content);
		workingSetGroup = new WorkingSetGroup(content, null, new String[] { "org.eclipse.ui.resourceWorkingSetPage" } ); //$NON-NLS-1$
		
		this.locationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				location = new File(locationText.getText());
				if (location.exists()) {
					if (projectNameText.getText().length() <= 0) {
						projectNameText.setText(location.getName());
					}
				}
				updatePageCompletion();
			}
		});
		this.projectNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				projectName = projectNameText.getText();
				updatePageCompletion();
			}
		});
		final DirectoryDialog browseDialog = new DirectoryDialog(getShell());
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String location = browseDialog.open();
				if (location != null) {
					locationText.setText(location);
				}
				updatePageCompletion();
			}
		});
		
		this.referenceRadio.setSelection(true);
		// disable copy for now because it's not supported
		this.copyRadio.setEnabled(false);
		updatePageCompletion();
		setControl(content);
	}

	/**
	 * 
	 */
	protected void updatePageCompletion() {
		StringBuilder errorMessages = new StringBuilder();
		// Location
		String locationMessage = null;
		if (location == null || !location.isDirectory() || !location.isAbsolute()) {
			locationMessage = DataTransferMessages.DirectoryImportAsProject_sourceIsNotAbsolutePathToDirectory;
		} else if (new File(this.location, IProjectDescription.DESCRIPTION_FILE_NAME).exists()) {
			locationMessage = DataTransferMessages.DirectoryImportAsProject_directoryAlreadyAnEclipseProject;
		}
		if (locationMessage != null) {
			errorMessages.append(locationMessage);
			if (this.locationFieldDecorator == null) {
				this.locationFieldDecorator = new ControlDecoration(this.locationText, SWT.TOP | SWT.LEFT);
			}
			this.locationFieldDecorator.setDescriptionText(locationMessage);
			this.locationFieldDecorator.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_ERROR));
		} else {
			if (this.locationFieldDecorator != null) {
				this.locationFieldDecorator.dispose();
				this.locationFieldDecorator = null;
			}
		}
		// project name
		String projectNameMessage = null;
		if (projectName == null) {
			projectNameMessage = DataTransferMessages.DirectoryImportAsProject_specifyProjectName;
		} else {
			IStatus validation = ResourcesPlugin.getWorkspace().validateName(this.projectName, IResource.PROJECT);
			if (validation.getCode() != IStatus.OK) {
				projectNameMessage = validation.getMessage();
			} else {
				if (ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName).exists()) {
					projectNameMessage = DataTransferMessages.DirectoryImportAsProject_projectAlreadyExists;
				}
			}
		}
		if (projectNameMessage != null) {
			if (errorMessages.length() > 0) {
				errorMessages.append('\n');
			}
			errorMessages.append(projectNameMessage);
			if (this.projectNameFieldDecorator == null) {
				this.projectNameFieldDecorator = new ControlDecoration(this.projectNameText, SWT.TOP | SWT.LEFT);
			}
			this.projectNameFieldDecorator.setDescriptionText(projectNameMessage);
			this.projectNameFieldDecorator.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_ERROR));
		} else {
			if (this.projectNameFieldDecorator != null) {
				this.projectNameFieldDecorator.dispose();
				this.projectNameFieldDecorator = null;
			}
		}
		
		if (errorMessages.length() > 0) {
			setErrorMessage(errorMessages.toString());
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	/**
	 * @return project handle
	 */
	public IProject getProjectHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName);
	}

	/**
	 * @return the location of selected directory to import as project
	 */
	public File getLocation() {
		return this.location;
	}
	
	/**
	 * @return whether project is to be copied (true) or referenced (false)
	 */
	public boolean isCopyStrategy() {
		return this.copyRadio.getSelection();
	}

	/**
	 * @return selectted working sets
	 */
	public IWorkingSet[] getSelectedWorkingSets() {
		return this.workingSetGroup.getSelectedWorkingSets();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	public void handleEvent(Event event) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#allowNewContainerName()
	 */
	protected boolean allowNewContainerName() {
		return true;
	}

}
