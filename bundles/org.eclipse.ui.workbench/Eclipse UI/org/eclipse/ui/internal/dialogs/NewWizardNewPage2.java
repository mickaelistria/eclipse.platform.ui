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
package org.eclipse.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.activities.ws.ActivityMessages;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * New wizard selection tab that allows the user to select a registered 'New'
 * wizard to be launched.
 * 
 * NOTE: This is still under construction.
 */
class NewWizardNewPage2
	implements ISelectionChangedListener, IDoubleClickListener {

	private static final class RootElementProxy
		extends WorkbenchAdapter
		implements IAdaptable {
		private WizardCollectionElement[] elements;

		public RootElementProxy(WizardCollectionElement element) {
			super();
			//If the element has no wizard then it is an empty category
			//and we should collapse
			if (element.getWizards().length == 0) {
				Object[] children = element.getChildren(null);
				elements = new WizardCollectionElement[children.length];
				System.arraycopy(children, 0, elements, 0, elements.length);
			} else
				elements = new WizardCollectionElement[] { element };
		}

		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return this;
			return null;
		}

		public Object[] getChildren(Object o) {
			return elements;
		}
	}

	// id constants
	private final static String DIALOG_SETTING_SECTION_NAME = "NewWizardSelectionPage."; //$NON-NLS-1$

	private final static int SIZING_LISTS_HEIGHT = 200;
	private final static int SIZING_LISTS_WIDTH = 150;
	private final static String STORE_EXPANDED_FILTERED_CATEGORIES_ID = DIALOG_SETTING_SECTION_NAME + "STORE_EXPANDED_FILTERED_CATEGORIES_ID"; //$NON-NLS-1$
	private final static String STORE_EXPANDED_UNFILTERED_CATEGORIES_ID = DIALOG_SETTING_SECTION_NAME + "STORE_EXPANDED_UNFILTERED_CATEGORIES_ID"; //$NON-NLS-1$
	private final static String STORE_SELECTED_FILTERED_ID = DIALOG_SETTING_SECTION_NAME + "STORE_SELECTED_FILTERED_ID"; //$NON-NLS-1$
	private final static String STORE_SELECTED_UNFILTERED_ID = DIALOG_SETTING_SECTION_NAME + "STORE_SELECTED_UNFILTERED_ID"; //$NON-NLS-1$
	private static final String UNFILTERED_TAB_SELECTED = DIALOG_SETTING_SECTION_NAME + ".UNFILTERED_TAB_SELECTED"; //$NON-NLS-1$
	private TreeViewer filteredViewer, unfilteredViewer;
	private NewWizardSelectionPage page;

	//Keep track of the wizards we have previously selected
	private Hashtable selectedWizards = new Hashtable();
	private IDialogSettings settings;

	private TabFolder tabFolder;
	private Text unfilteredDescriptionText, filteredDescriptionText;
	private WizardCollectionElement wizardCategories;

	/**
	 * Create an instance of this class
	 */
	public NewWizardNewPage2(
		NewWizardSelectionPage mainPage,
		IWorkbench aWorkbench,
		WizardCollectionElement wizardCategories) {
		this.page = mainPage;
		this.wizardCategories = wizardCategories;
	}

	/**
	 * @since 3.0
	 */
	public void activate() {
		page.setDescription(WorkbenchMessages.getString("NewWizardNewPage.description")); //$NON-NLS-1$
	}
	/**
	 * Create this tab's visual components
	 * 
	 * @param parent Composite
	 * @return Control
	 */
	protected Control createControl(Composite parent) {

		Font wizardFont = parent.getFont();
		// top level group
		Composite outerContainer = new Composite(parent, SWT.NONE);
		outerContainer.setFont(wizardFont);

		if (WorkbenchActivityHelper.isFiltering()) {
			outerContainer.setLayout(new FillLayout());
			tabFolder = new TabFolder(outerContainer, SWT.NONE);
			tabFolder.setFont(parent.getFont());

			Composite container = new Composite(tabFolder, SWT.NONE);
			container.setLayout(new GridLayout(2, true));
			filteredViewer = createViewer(container, true);
			filteredDescriptionText = createDescriptionText(container);
			createTab(container, true);

			container = new Composite(tabFolder, SWT.NONE);
			container.setLayout(new GridLayout(2, true));
			unfilteredViewer = createViewer(container, false);
			unfilteredDescriptionText = createDescriptionText(container);
			createTab(container, false);

			// flipping tabs updates the selected node
			tabFolder.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (tabFolder.getSelectionIndex() == 0) {
						selectionChanged(
							new SelectionChangedEvent(
								filteredViewer,
								filteredViewer.getSelection()));
					} else {
						selectionChanged(
							new SelectionChangedEvent(
								unfilteredViewer,
								unfilteredViewer.getSelection()));
					}
				}
			});
		} else {
			outerContainer.setLayout(new GridLayout(2, true));
			unfilteredViewer = createViewer(outerContainer, false);
			unfilteredDescriptionText = createDescriptionText(outerContainer);
		}

		updateDescriptionText(""); //$NON-NLS-1$

		// wizard actions pane...create SWT table directly to
		// get single selection mode instead of multi selection.
		restoreWidgetValues();

		// we only set focus if a selection was restored
		//		if (!filteredViewer.getSelection().isEmpty())
		//			filteredViewer.getTree().setFocus();
		return outerContainer;
	}

	/**
	 * Create a new description text control.
	 * 
	 * @param parent the parent <code>Composite</code>.
	 * @return the <code>Text</code> control.
	 * @since 3.0
	 */
	private Text createDescriptionText(Composite parent) {
		ScrolledComposite scroller =
			new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
		scroller.setBackground(filteredViewer.getControl().getBackground());
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = SIZING_LISTS_WIDTH;
		scroller.setLayoutData(data);
		scroller.setContent(unfilteredDescriptionText);

		Text text = new Text(scroller, SWT.READ_ONLY | SWT.WRAP);
		text.setBackground(filteredViewer.getControl().getBackground());
		return text;
	}

	/**
	 * Create a <code>TabItem</code> in the <code>TabFolder</code>.
	 * 
	 * @param container the <code>Composite</code> to set as the tab control.
	 * @param filtering whether this tab is for filtered or unfiltered
	 *            controls.
	 * @since 3.0
	 */
	private void createTab(Composite container, boolean filtering) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setControl(container);
		tabItem.setText(filtering ? ActivityMessages.getString("ActivityFiltering.filtered") //$NON-NLS-1$
		: ActivityMessages.getString("ActivityFiltering.unfiltered")); //$NON-NLS-1$

	}

	/**
	 * Create a new viewer in the parent.
	 * 
	 * @param parent the parent <code>Composite</code>.
	 * @param filtering whether the viewer should be filtering based on
	 *            activities.
	 * @return <code>TreeViewer</code>
	 * @since 3.0
	 */
	private TreeViewer createViewer(Composite parent, boolean filtering) {
		// category tree pane...create SWT tree directly to
		// get single selection mode instead of multi selection.
		Tree tree =
			new Tree(
				parent,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		TreeViewer treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new WizardContentProvider(filtering));
		treeViewer.setLabelProvider(new WorkbenchLabelProvider());
		treeViewer.setSorter(NewWizardCollectionSorter2.INSTANCE);
		treeViewer.addSelectionChangedListener(this);
		treeViewer.addDoubleClickListener(this);
		if (wizardCategories.getParent(wizardCategories) == null) {
			treeViewer.setInput(wizardCategories);
		} else
			treeViewer.setInput(new RootElementProxy(wizardCategories));
		tree.setFont(parent.getFont());

		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = SIZING_LISTS_WIDTH;

		boolean needsHint = DialogUtil.inRegularFontMode(tree.getParent());

		//Only give a height hint if the dialog is going to be too small
		if (needsHint) {
			data.heightHint = SIZING_LISTS_HEIGHT;
		}

		tree.setLayoutData(data);
		return treeViewer;
	}
	/**
	 * Create a viewer pane in this group for the passed viewer.
	 * 
	 * @param parent org.eclipse.swt.widgets.Composite
	 * @param width int
	 * @param height int
	 */
	protected Composite createViewPane(
		Composite parent,
		int width,
		int height) {
		Composite paneWindow = new Composite(parent, SWT.BORDER);
		GridData spec = new GridData(GridData.FILL_BOTH);
		spec.widthHint = width;
		spec.heightHint = height;
		paneWindow.setLayoutData(spec);
		paneWindow.setLayout(new FillLayout());
		return paneWindow;
	}
	/**
	 * A wizard in the wizard viewer has been double clicked. Treat it as a
	 * selection.
	 */
	public void doubleClick(DoubleClickEvent event) {
		selectionChanged(
			new SelectionChangedEvent(
				event.getViewer(),
				event.getViewer().getSelection()));
		page.advanceToNextPage();
	}
	/**
	 * Expands the wizard categories in this page's category viewer that were
	 * expanded last time this page was used. If a category that was previously
	 * expanded no longer exists then it is ignored.
	 */
	protected void expandPreviouslyExpandedCategories() {
		expandViewer(unfilteredViewer, STORE_EXPANDED_UNFILTERED_CATEGORIES_ID);

		if (WorkbenchActivityHelper.isFiltering()) {
			expandViewer(filteredViewer, STORE_EXPANDED_FILTERED_CATEGORIES_ID);
		}
	}

	/**
	 * Load the viewer expansion state for the given viewer from preferences.
	 * 
	 * @param viewer the <code>TreeViewer</code> to update.
	 * @param key the preference key to use.
	 * @since 3.0
	 */
	private void expandViewer(TreeViewer viewer, String key) {
		String[] expandedCategoryPaths = settings.getArray(key);
		if (expandedCategoryPaths == null || expandedCategoryPaths.length == 0)
			return;

		List categoriesToExpand = new ArrayList(expandedCategoryPaths.length);

		for (int i = 0; i < expandedCategoryPaths.length; i++) {
			WizardCollectionElement category =
				wizardCategories.findChildCollection(
					new Path(expandedCategoryPaths[i]));
			if (category != null) // ie.- it still exists
				categoriesToExpand.add(category);
		}

		if (!categoriesToExpand.isEmpty())
			viewer.setExpandedElements(categoriesToExpand.toArray());
	}

	/**
	 * Returns the single selected object contained in the passed
	 * selectionEvent, or <code>null</code> if the selectionEvent contains
	 * either 0 or 2+ selected objects.
	 */
	protected Object getSingleSelection(IStructuredSelection selection) {
		return selection.size() == 1 ? selection.getFirstElement() : null;
	}

	/**
	 * Set self's widgets to the values that they held last time this page was
	 * open
	 *  
	 */
	protected void restoreWidgetValues() {

		//		String[] expandedCategoryPaths =
		//			settings.getArray(STORE_EXPANDED_CATEGORIES_ID);
		//		if (expandedCategoryPaths == null)
		//			return; // no stored values
		//
		expandPreviouslyExpandedCategories();
		selectPreviouslySelected();
	}

	/**
	 * Save the expansion state of the provided viewer.
	 * 
	 * @param viewer the <code>TreeViewer</code> to save selection from.
	 * @param key the preference key to use.
	 * @since 3.0
	 */
	private void saveViewerExpansion(TreeViewer viewer, String key) {
		Object[] expandedElements = viewer.getExpandedElements();
		List expandedElementPaths = new ArrayList(expandedElements.length);
		for (int i = 0; i < expandedElements.length; ++i) {
			if (expandedElements[i] instanceof WizardCollectionElement)
				expandedElementPaths.add(
					((WizardCollectionElement) expandedElements[i])
						.getPath()
						.toString());
		}
		settings.put(
			key,
			(String[]) expandedElementPaths.toArray(
				new String[expandedElementPaths.size()]));

	}

	/**
	 * Save the selection of the provided viewer.
	 * 
	 * @param viewer the <code>TreeViewer</code> to save selection from.
	 * @param key the preference key to use.
	 * @since 3.0
	 */
	private void saveViewerSelection(TreeViewer viewer, String key) {

		Object selected =
			getSingleSelection((IStructuredSelection) viewer.getSelection());

		if (selected != null) {
			if (selected instanceof WizardCollectionElement)
				settings.put(
					key,
					((WizardCollectionElement) selected).getPath().toString());
			else // else its a wizard
				settings.put(key, ((WorkbenchWizardElement) selected).getID());
		}
	}
	/**
	 * Store the current values of self's widgets so that they can be restored
	 * in the next instance of self
	 *  
	 */
	public void saveWidgetValues() {
		storeExpandedCategories();
		storeSelectedCategoryAndWizard();
	}
	/**
	 * The user selected either new wizard category(s) or wizard element(s).
	 * Proceed accordingly.
	 * 
	 * @param newSelection ISelection
	 */
	public void selectionChanged(SelectionChangedEvent selectionEvent) {
		page.setErrorMessage(null);
		page.setMessage(null);

		Object selectedObject =
			getSingleSelection(
				(IStructuredSelection) selectionEvent.getSelection());

		if (selectedObject instanceof WizardCollectionElement) {
			updateCategorySelection((WizardCollectionElement) selectedObject);
		} else if (selectedObject instanceof WorkbenchWizardElement) {
			updateWizardSelection((WorkbenchWizardElement) selectedObject);
		}
	}

	/**
	 * Selects the wizard category and wizard in this page that were selected
	 * last time this page was used. If a category or wizard that was
	 * previously selected no longer exists then it is ignored.
	 */
	protected void selectPreviouslySelected() {
		boolean unfilteredSelected =
			settings.getBoolean(UNFILTERED_TAB_SELECTED);

		updateViewerSelection(unfilteredViewer, STORE_SELECTED_UNFILTERED_ID);

		if (WorkbenchActivityHelper.isFiltering()) {
			updateViewerSelection(filteredViewer, STORE_SELECTED_FILTERED_ID);
			if (unfilteredSelected) {
				tabFolder.setSelection(1);
				selectionChanged(
					new SelectionChangedEvent(
						unfilteredViewer,
						unfilteredViewer.getSelection()));
			}
		}

	}

	/**
	 * Set the dialog store to use for widget value storage and retrieval
	 * 
	 * @param settings IDialogSettings
	 */
	public void setDialogSettings(IDialogSettings settings) {
		this.settings = settings;
	}

	/**
	 * Stores the collection of currently-expanded categories in this page's
	 * dialog store, in order to recreate this page's state in the next
	 * instance of this page.
	 */
	protected void storeExpandedCategories() {
		saveViewerExpansion(
			unfilteredViewer,
			STORE_EXPANDED_UNFILTERED_CATEGORIES_ID);
		if (WorkbenchActivityHelper.isFiltering())
			saveViewerExpansion(
				filteredViewer,
				STORE_EXPANDED_FILTERED_CATEGORIES_ID);
	}

	/**
	 * Stores the currently-selected element in this page's dialog store, in
	 * order to recreate this page's state in the next instance of this page.
	 */
	protected void storeSelectedCategoryAndWizard() {

		saveViewerSelection(unfilteredViewer, STORE_SELECTED_UNFILTERED_ID);

		if (WorkbenchActivityHelper.isFiltering()) {
			saveViewerSelection(filteredViewer, STORE_SELECTED_FILTERED_ID);

			settings.put(
				UNFILTERED_TAB_SELECTED,
				tabFolder.getSelectionIndex() == 1 ? true : false);
		}
	}

	/**
	 * @param selectedCategory
	 */
	private void updateCategorySelection(WizardCollectionElement selectedCategory) {
		page.selectWizardNode(null);
	}

	/**
	 * Update the current description control with the provided message.
	 * 
	 * @param string the new message
	 * @since 3.0
	 */
	private void updateDescriptionText(String string) {
		if (unfilteredViewer.getControl().isFocusControl())
			updateDescriptionText(unfilteredDescriptionText, string);
		else if (filteredViewer != null)
			updateDescriptionText(filteredDescriptionText, string);
	}

	/**
	 * @param text the <code>Text</code> widget to update.
	 * @param string the new message
	 * @since 3.0
	 */
	private void updateDescriptionText(Text text, String string) {
		if (text != null && !text.isDisposed()) {
			text.setText(string);
			text.setSize(text.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
	}

	/**
	 * Load the viewer selection for the given viewer from preferences.
	 * 
	 * @param viewer the <code>TreeViewer</code> to update.
	 * @param key the preference key to use.
	 * @since 3.0
	 */
	private void updateViewerSelection(TreeViewer viewer, String key) {
		String selectedId = (String) settings.get(key);
		if (selectedId == null)
			return;

		Object selected =
			wizardCategories.findChildCollection(new Path(selectedId));

		if (selected == null) {
			selected = wizardCategories.findWizard(selectedId, true);

			if (selected == null)
				// if we cant find either a category or a wizard, abort.
				return;
		}
		StructuredSelection selection = new StructuredSelection(selected);
		viewer.setSelection(selection);
	}
	/**
	 * @param selectedObject
	 */
	private void updateWizardSelection(WorkbenchWizardElement selectedObject) {
		WorkbenchWizardNode selectedNode;

		if (selectedWizards.containsKey(selectedObject)) {
			selectedNode =
				(WorkbenchWizardNode) selectedWizards.get(selectedObject);
		} else {
			selectedNode =
				new WorkbenchWizardNode(
					page,
					(WorkbenchWizardElement) selectedObject) {
				public IWorkbenchWizard createWizard() throws CoreException {
					return (INewWizard) wizardElement
						.createExecutableExtension();
				}
			};
			selectedWizards.put(selectedObject, selectedNode);
		}

		page.selectWizardNode(selectedNode);

		updateDescriptionText(selectedObject.getDescription());
	}
}
