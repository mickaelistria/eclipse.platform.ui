/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.ide.dialogs;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.ide.AboutBundleGroupData;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IHelpContextIds;

/**
 * Displays information about the product.
 *
 * PRIVATE  
 * 	this class is internal to the ide
 */
public class AboutDialog extends ProductInfoDialog {
	private final static int MAX_IMAGE_WIDTH_FOR_TEXT = 250;
	private final static int FEATURES_ID = IDialogConstants.CLIENT_ID + 1;
	private final static int PLUGINS_ID = IDialogConstants.CLIENT_ID + 2;
	private final static int INFO_ID = IDialogConstants.CLIENT_ID + 3;

	private String productName;
	private AboutBundleGroupData[] bundleGroupInfos;

	private ArrayList images = new ArrayList();
	private AboutFeaturesButtonManager buttonManager = new AboutFeaturesButtonManager();

    // TODO should the styled text be disposed? if not then it likely
    //      doesn't need to be a member
	private StyledText text;

	/**
	 * Create an instance of the AboutDialog for the given window.
	 */
	public AboutDialog(Shell parentShell) {
	    super(parentShell);

	    IProduct product = Platform.getProduct();
	    IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();
	    
		String productId = ""; //$NON-NLS-1$
        if (product == null)
            productName = IDEWorkbenchMessages
                    .getString("AboutDialog.defaultProductName"); //$NON-NLS-1$
        else {
            productId = product.getId();
            productName = product.getName();
        }

        // create a descriptive object for each BundleGroup, putting the primary
        // first if it can be found
        LinkedList groups = new LinkedList();
        if (providers != null)
            for (int i = 0; i < providers.length; ++i) {
                IBundleGroup[] bundleGroups = providers[i].getBundleGroups();
                for (int j = 0; j < bundleGroups.length; ++j) {
                    AboutBundleGroupData info = new AboutBundleGroupData(
                                bundleGroups[j]);

                    // if there's a bundle with the same id as the product,
                    // assume its the primary bundle and put it first
                    if (info.getId().equals(productId))
                        groups.addFirst(info);
                    else
                        groups.add(info);
                }
            }
        bundleGroupInfos = (AboutBundleGroupData[]) groups
                .toArray(new AboutBundleGroupData[0]);
	}

	/*
     * (non-Javadoc) Method declared on Dialog.
     */
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case FEATURES_ID:
            new AboutFeaturesDialog(getShell(), productName, bundleGroupInfos).open();
            break;
        case PLUGINS_ID:
            new AboutPluginsDialog(getShell(), productName).open();
            break;
        case INFO_ID:
            new SystemSummaryDialog(getShell()).open();
            break;
        default:
            super.buttonPressed(buttonId);
            break;
        }
    }

	public boolean close() {
        // dispose all images
        for (int i = 0; i < images.size(); ++i) {
            Image image = (Image) images.get(i);
            image.dispose();
        }

        return super.close();
    }

	/*
     * (non-Javadoc) Method declared on Window.
     */
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(IDEWorkbenchMessages.format("AboutDialog.shellTitle", //$NON-NLS-1$
                new Object[] { productName }));
        WorkbenchHelp.setHelp(newShell, IHelpContextIds.ABOUT_DIALOG);
    }

	/**
     * Add buttons to the dialog's button bar.
     * 
     * Subclasses should override.
     * 
     * @param parent
     *            the button bar composite
     */
    protected void createButtonsForButtonBar(Composite parent) {
        parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createButton(parent, FEATURES_ID, IDEWorkbenchMessages
                .getString("AboutDialog.featureInfo"), false); //$NON-NLS-1$
        createButton(parent, PLUGINS_ID, IDEWorkbenchMessages
                .getString("AboutDialog.pluginInfo"), false); //$NON-NLS-1$
        createButton(parent, INFO_ID, IDEWorkbenchMessages
                .getString("AboutDialog.systemInfo"), false); //$NON-NLS-1$

        Label l = new Label(parent, SWT.NONE);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout layout = (GridLayout) parent.getLayout();
        layout.numColumns++;
        layout.makeColumnsEqualWidth = false;

        Button b = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        b.setFocus();
    }

	/**
	 * Creates and returns the contents of the upper part 
	 * of the dialog (above the button bar).
	 *
	 * Subclasses should overide.
	 *
	 * @param parent  the parent composite to contain the dialog area
	 * @return the dialog area control
	 */
	protected Control createDialogArea(Composite parent) {
		final Cursor hand = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
        final Cursor busy = new Cursor(parent.getDisplay(), SWT.CURSOR_WAIT);
        setHandCursor(hand);
        setBusyCursor(busy);
        getShell().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                setHandCursor(null);
                hand.dispose();
                setBusyCursor(null);
                busy.dispose();
            }
        });

		// if there is product info (index 0), then brand the about box
		Image aboutImage = null;
        if (bundleGroupInfos.length > 0) {
        	AboutBundleGroupData productInfo = bundleGroupInfos[0];

	        ImageDescriptor imageDescriptor = null;
	        if (productInfo != null)
	        	imageDescriptor = productInfo.getAboutImage();
	        if (imageDescriptor != null)
	            aboutImage = imageDescriptor.createImage();

			// if the about image is small enough, then show the text
			if (aboutImage == null
                    || aboutImage.getBounds().width <= MAX_IMAGE_WIDTH_FOR_TEXT) {
	            String aboutText = null;
	            if (productInfo != null)
	            	aboutText = productInfo.getAboutText();
	            if (aboutText != null)
	            	setItem(scan(aboutText));
	        }

			if(aboutImage != null)
			    images.add(aboutImage);
		}

		// page group
		Composite outer = (Composite) super.createDialogArea(parent);
		outer.setSize(outer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		GridLayout layout = new GridLayout();
		outer.setLayout(layout);
		outer.setLayoutData(new GridData(GridData.FILL_BOTH));

		// the image & text	
		Composite topContainer = new Composite(outer, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = (aboutImage == null || getItem() == null ? 1 : 2);
		layout.marginWidth = 0;
		topContainer.setLayout(layout);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		topContainer.setLayoutData(data);

		//image on left side of dialog
		if (aboutImage != null) {
			Label imageLabel = new Label(topContainer, SWT.NONE);
			data = new GridData();
			data.horizontalAlignment = GridData.FILL;
			data.verticalAlignment = GridData.BEGINNING;
			data.grabExcessHorizontalSpace = false;
			imageLabel.setLayoutData(data);
			imageLabel.setImage(aboutImage);
		}

		if (getItem() != null) {
			// text on the right
			text = new StyledText(topContainer, SWT.MULTI | SWT.READ_ONLY);
			text.setCaret(null);
			text.setFont(parent.getFont());
			data = new GridData();
			data.horizontalAlignment = GridData.FILL;
			data.verticalAlignment = GridData.BEGINNING;
			data.grabExcessHorizontalSpace = true;
			text.setText(getItem().getText());
			text.setLayoutData(data);
			text.setCursor(null);
			text.setBackground(topContainer.getBackground());
			setLinkRanges(text, getItem().getLinkRanges());
			addListeners(text);
		}

		// horizontal bar
		Label bar = new Label(outer, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		bar.setLayoutData(data);

		// add image buttons for bundle groups that have them
		createFeatureImageButtonRow(outer);

		// spacer
		bar = new Label(outer, SWT.NONE);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		bar.setLayoutData(data);

		return outer;
	}

	private void createFeatureImageButtonRow(Composite parent) {
		// feature images
		Composite featureContainer = new Composite(parent, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.wrap = true;
		featureContainer.setLayout(rowLayout);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		featureContainer.setLayoutData(data);

		// create buttons for all the rest
		for (int i = 0; i < bundleGroupInfos.length; i++)
			createFeatureButton(featureContainer, bundleGroupInfos[i]);
	}

	private Button createFeatureButton(Composite parent,
            final AboutBundleGroupData info) {
        if (!buttonManager.add(info))
			return null;

        ImageDescriptor desc = info.getFeatureImage();
        Image featureImage = null;

	    Button button = new Button(parent, SWT.FLAT | SWT.PUSH);
        button.setData(info);
        featureImage = desc.createImage();
        images.add(featureImage);
        button.setImage(featureImage);
        button.setToolTipText(info.getProviderName());
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                AboutBundleGroupData[] groupInfos = buttonManager
                        .getRelatedInfos(info);
				AboutBundleGroupData selection = (AboutBundleGroupData) event.widget
                        .getData();

                AboutFeaturesDialog d = new AboutFeaturesDialog(getShell(),
                        productName, groupInfos);
                d.setInitialSelection(selection);
                d.open();
            }
        });

		return button;
	}
}
