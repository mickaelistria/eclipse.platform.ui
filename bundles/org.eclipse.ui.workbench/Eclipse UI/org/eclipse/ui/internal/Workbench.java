package org.eclipse.ui.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.Configuration;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.window.WindowManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IMarkerHelpRegistry;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.dialogs.WelcomeEditorInput;
import org.eclipse.ui.internal.keybindings.KeyBindingManager;
import org.eclipse.ui.internal.keybindings.Path;
import org.eclipse.ui.internal.misc.Assert;
import org.eclipse.ui.internal.model.WorkbenchAdapterBuilder;
import org.eclipse.ui.internal.registry.AcceleratorConfiguration;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.VersionedIdentifier;

/**
 * The workbench class represents the top of the ITP user interface.  Its primary
 * responsability is the management of workbench windows and other ISV windows.
 */
public class Workbench implements IWorkbench, IPlatformRunnable, IExecutableExtension {
	private static final String VERSION_STRING[] = { "0.046", "2.0" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String P_PRODUCT_INFO = "productInfo"; //$NON-NLS-1$
	private static final String DEFAULT_PRODUCT_INFO_FILENAME = "product.ini"; //$NON-NLS-1$
	private static final String DEFAULT_WORKBENCH_STATE_FILENAME = "workbench.xml"; //$NON-NLS-1$
	private static final int RESTORE_CODE_OK = 0;
	private static final int RESTORE_CODE_RESET = 1;
	private static final int RESTORE_CODE_EXIT = 2;
	private static final String WELCOME_EDITOR_ID = "org.eclipse.ui.internal.dialogs.WelcomeEditor";  //$NON-NLS-1$
	private static final String INSTALLED_FEATURES = "installedFeatures";
	

	private WindowManager windowManager;
	private EditorHistory editorHistory;
	private PerspectiveHistory perspHistory;
	private boolean runEventLoop;
	private boolean isStarting = false;
	private boolean isClosing = false;
	private IPluginDescriptor startingPlugin; // the plugin which caused the workbench to be instantiated
	private String productInfoFilename;
	private AboutInfo aboutInfo;
	private AboutInfo[] featuresInfo;
	private AboutInfo[] newFeaturesInfo;
	private String[] commandLineArgs;
	private Window.IExceptionHandler handler;
	private AcceleratorConfiguration acceleratorConfiguration;
	private Object returnCode;
	private ListenerList windowListeners = new ListenerList();
	
	/**
	 * Workbench constructor comment.
	 */
	public Workbench() {
		super();
		WorkbenchPlugin.getDefault().setWorkbench(this);
	}
	/**
	 * See IWorkbench
	 */
	public void addWindowListener(IWindowListener l) {
		windowListeners.add(l);
	}	
	/**
	 * See IWorkbench
	 */
	public void removeWindowListener(IWindowListener l) {
		windowListeners.remove(l);
	}
	/**
	 * Fire window opened event.
	 */	
	protected void fireWindowOpened(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener)list[i]).windowOpened(window);
		}
	}		
	/**
	 * Fire window closed event.
	 */
	protected void fireWindowClosed(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener)list[i]).windowClosed(window);
		}
	}
	/**
	 * Fire window activated event.
	 */	
	protected void fireWindowActivated(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener)list[i]).windowActivated(window);
		}
	}
	/**
	 * Fire window deactivated event.
	 */	
	protected void fireWindowDeactivated(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener)list[i]).windowDeactivated(window);
		}
	}		
	/**
	 * Get the extenders from the registry and adds them to the 
	 * extender manager.
	 */
	private void addAdapters() {
		WorkbenchAdapterBuilder builder = new WorkbenchAdapterBuilder();
		builder.registerAdapters();
	}
	/**
	 * Close the workbench
	 *
	 * Assumes that busy cursor is active.
	 */
	private boolean busyClose(final boolean force) {
		isClosing = true;
		Platform.run(new SafeRunnable() {
			public void run() {
				XMLMemento mem = recordWorkbenchState();
				//Save the IMemento to a file.
				saveWorkbenchState(mem);
			}
			public void handleException(Throwable e) {
				String message;
				if (e.getMessage() == null) {
					message = WorkbenchMessages.getString("ErrorClosingNoArg"); //$NON-NLS-1$
				} else {
					message = WorkbenchMessages.format("ErrorClosingOneArg", new Object[] { e.getMessage()}); //$NON-NLS-1$
				}

				if (!MessageDialog.openQuestion(null, WorkbenchMessages.getString("Error"), message)) //$NON-NLS-1$
					isClosing = false;
			}
		});
		if (!isClosing && !force)
			return false;

		isClosing = saveAllEditors(!force);
		Platform.run(new SafeRunnable(WorkbenchMessages.getString("ErrorClosing")) { //$NON-NLS-1$
			public void run() {
				if(isClosing || force)
					isClosing = windowManager.close();
			}
		});

		if (!isClosing && !force)
			return false;

		if (WorkbenchPlugin.getPluginWorkspace() != null)
			disconnectFromWorkspace();

		runEventLoop = false;
		return true;
	}
	
	/*
	 * @see IWorkbench.saveAllEditors(boolean)	 */
	public boolean saveAllEditors(boolean confirm) {
		final boolean finalConfirm = confirm;
		final boolean [] result = new boolean[1];
		result[0] = true;
		
		Platform.run(new SafeRunnable(WorkbenchMessages.getString("ErrorClosing")) { //$NON-NLS-1$
			public void run() {
				//Collect dirtyEditors
				ArrayList dirtyEditors = new ArrayList();
				ArrayList dirtyEditorsInput = new ArrayList();
				IWorkbenchWindow windows[] = getWorkbenchWindows();
				for (int i = 0; i < windows.length; i++) {
					IWorkbenchPage pages[] = windows[i].getPages();
					for (int j = 0; j < pages.length; j++) {
						WorkbenchPage page = (WorkbenchPage)pages[j];
						IEditorPart editors[] = page.getDirtyEditors();
						for (int k = 0; k < editors.length; k++) {
							IEditorPart editor = editors[k];
							if(editor.isDirty()) {
								if(!dirtyEditorsInput.contains(editor.getEditorInput())) {
									dirtyEditors.add(editor);
									dirtyEditorsInput.add(editor.getEditorInput());
								}
							}
						}
					}
				}
				if(dirtyEditors.size() > 0) {
					IWorkbenchWindow w = getActiveWorkbenchWindow();
					if(w == null)
						w = windows[0];
					result[0] = EditorManager.saveAll(dirtyEditors,finalConfirm,w);					
				}			
			}
		});
	return result[0];
	}
	/**
	 * Opens a new workbench window and page with a specific perspective.
	 *
	 * Assumes that busy cursor is active.
	 */
	private IWorkbenchWindow busyOpenWorkbenchWindow(String perspID, IAdaptable input) throws WorkbenchException {
		// Create a workbench window (becomes active window)
		WorkbenchWindow newWindow = new WorkbenchWindow(this, getNewWindowNumber());
		newWindow.create(); // must be created before adding to window manager
		windowManager.add(newWindow);

		// Create the initial page.
		newWindow.busyOpenPage(perspID, input);

		// Open after opening page, to avoid flicker.
		newWindow.open();

		return newWindow;
	}

	/**
	 * Check if the -newUpdates command line argument is present
	 * and if so, open the udpates dialog
	 */
	private void checkUpdates() {
		boolean newUpdates = false;
		for (int i = 0; i < commandLineArgs.length; i++) {
			if (commandLineArgs[i].equalsIgnoreCase("-newUpdates")) {//$NON-NLS-1$
				newUpdates = true;
				break;
			}
		}
		
		if (newUpdates)		
			showUpdatesDialog();
	}
	/**
	 * Show the new updates dialog
	 */
	private void showUpdatesDialog() {
		Shell shell = null;
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) // should never be null
			shell = window.getShell();
		if (MessageDialog.openQuestion(
				shell, 
				WorkbenchMessages.getString("Updates.title"), //$NON-NLS-1$
				WorkbenchMessages.getString("Updates.message"))) {	 //$NON-NLS-1$
			try {
				SiteManager.handleNewChanges();
			} catch (CoreException ex) {
				WorkbenchPlugin.log("Problem opening update manager", ex.getStatus()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Closes the workbench.
	 */
	public boolean close() {
		return close(EXIT_OK);
	}
	/**
	 * Closes the workbench, returning the given return code from the run method.
	 */
	public boolean close(Object returnCode) {
		return close(returnCode,false);
	}
	/**
	 * Closes the workbench, returning the given return code from the run method.
	 */
	public boolean close(Object returnCode,final boolean force) {
		this.returnCode = returnCode;
		final boolean[] ret = new boolean[1];
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				ret[0] = busyClose(force);
			}
		});
		return ret[0];
	}
	/**
	 * Connect to the core workspace.
	 */
	private void connectToWorkspace() {
		// Nothing to do right now.
	}
	/**
	 * Disconnect from the core workspace.
	 */
	private void disconnectFromWorkspace() {
		//Save the workbench.
		final MultiStatus status = new MultiStatus(WorkbenchPlugin.PI_WORKBENCH, 1, WorkbenchMessages.getString("ProblemSavingWorkbench"), null); //$NON-NLS-1$
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					status.merge(ResourcesPlugin.getWorkspace().save(true, monitor));
				} catch (CoreException e) {
					status.merge(e.getStatus());
				}
			}
		};
		try {
			new ProgressMonitorDialog(null).run(false, false, runnable);
		} catch (InvocationTargetException e) {
			status.merge(new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 1, WorkbenchMessages.getString("InternalError"), e.getTargetException())); //$NON-NLS-1$
		} catch (InterruptedException e) {
			status.merge(new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 1, WorkbenchMessages.getString("InternalError"), e)); //$NON-NLS-1$
		}
		ErrorDialog.openError(null, WorkbenchMessages.getString("ProblemsSavingWorkspace"), //$NON-NLS-1$
		null, status, IStatus.ERROR | IStatus.WARNING);
		if (!status.isOK()) {
			WorkbenchPlugin.log(WorkbenchMessages.getString("ProblemsSavingWorkspace"), status); //$NON-NLS-1$
		}
	}
	/**
	 * @see IWorkbench
	 */
	public IWorkbenchWindow getActiveWorkbenchWindow() {

		Display display = Display.getCurrent();
		// Display will be null if SWT has not been initialized or
		// this method was called from wrong thread.
		if (display == null)
			return null;
		Control shell = display.getActiveShell();
		while (shell != null) {
			Object data = shell.getData();
			if (data instanceof IWorkbenchWindow)
				return (IWorkbenchWindow) data;
			shell = shell.getParent();
		}
		Shell shells[] = display.getShells();
		for (int i = 0; i < shells.length; i++) {
			Object data = shells[i].getData();
			if (data instanceof IWorkbenchWindow)
				return (IWorkbenchWindow) data;
		}
		return null;
	}
	/**
	 * Returns the command line arguments, excluding any which were filtered out by the launcher.
	 */
	public String[] getCommandLineArgs() {
		return commandLineArgs;
	}
	/**
	 * Returns the editor history.
	 */
	public EditorHistory getEditorHistory() {
		if (editorHistory == null) {
			IPreferenceStore store = getPreferenceStore();
			editorHistory = new EditorHistory(store.getInt(IPreferenceConstants.RECENT_FILES));
		}
		return editorHistory;
	}
	/**
	 * Returns the perspective history.
	 */
	public PerspectiveHistory getPerspectiveHistory() {
		if (perspHistory == null) {
			perspHistory = new PerspectiveHistory(getPerspectiveRegistry());
		}
		return perspHistory;
	}
	/**
	 * Returns the editor registry for the workbench.
	 *
	 * @return the workbench editor registry
	 */
	public IEditorRegistry getEditorRegistry() {
		return WorkbenchPlugin.getDefault().getEditorRegistry();
	}
	/*
	 * Returns the number for a new window.  This will be the first
	 * number > 0 which is not used to identify another window in
	 * the workbench.
	 */
	private int getNewWindowNumber() {
		// Get window list.
		Window[] windows = windowManager.getWindows();
		int count = windows.length;

		// Create an array of booleans (size = window count).  
		// Cross off every number found in the window list.  
		boolean checkArray[] = new boolean[count];
		for (int nX = 0; nX < count; nX++) {
			if (windows[nX] instanceof WorkbenchWindow) {
				WorkbenchWindow ww = (WorkbenchWindow) windows[nX];
				int index = ww.getNumber() - 1;
				if (index >= 0 && index < count)
					checkArray[index] = true;
			}
		}

		// Return first index which is not used.
		// If no empty index was found then every slot is full.
		// Return next index.
		for (int index = 0; index < count; index++) {
			if (!checkArray[index])
				return index + 1;
		}
		return count + 1;
	}
	/**
	 * Returns the perspective registry for the workbench.
	 *
	 * @return the workbench perspective registry
	 */
	public IPerspectiveRegistry getPerspectiveRegistry() {
		return WorkbenchPlugin.getDefault().getPerspectiveRegistry();
	}
	/**
	 * Returns the preference manager for the workbench.
	 *
	 * @return the workbench preference manager
	 */
	public PreferenceManager getPreferenceManager() {
		return WorkbenchPlugin.getDefault().getPreferenceManager();
	}
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IPreferenceStore getPreferenceStore() {
		return WorkbenchPlugin.getDefault().getPreferenceStore();
	}
	/**
	 * Returns the shared images for the workbench.
	 *
	 * @return the shared image manager
	 */
	public ISharedImages getSharedImages() {
		return WorkbenchPlugin.getDefault().getSharedImages();
	}
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IMarkerHelpRegistry getMarkerHelpRegistry() {
		return WorkbenchPlugin.getDefault().getMarkerHelpRegistry();
	}
	/**
	 * Returns the current window manager being used by the workbench
	 */
	protected WindowManager getWindowManager() {
		return windowManager;
	}
	
	/**
	 * Returns the about info.
	 *
	 * @return the about info
	 */
	public AboutInfo getAboutInfo() {
		return aboutInfo;
	}

	/**
	 * Returns the about info for all configured features with a corresponding plugin.
	 *
	 * @return the about info
	 */
	public AboutInfo[] getFeaturesInfo() {
		if (featuresInfo == null)
			readFeaturesInfo();
		return featuresInfo;
	}

	/**
	 * Returns the about info for all new (since the last time the workbench was run)
	 * configured features with a corresponding plugin.
	 *
	 * @return the about info
	 */
	private AboutInfo[] getNewFeaturesInfo() {
		if (newFeaturesInfo == null)
			readFeaturesInfo();
		return newFeaturesInfo;
	}
	
	/**
	 * Returns the active AcceleratorConfiguration
	 */
	public AcceleratorConfiguration getActiveAcceleratorConfiguration() {
		return acceleratorConfiguration;
	}
	/**
	 * Answer the workbench state file.
	 */
	private File getWorkbenchStateFile() {
		IPath path = WorkbenchPlugin.getDefault().getStateLocation();
		path = path.append(DEFAULT_WORKBENCH_STATE_FILENAME);
		return path.toFile();
	}
	/**
	 * Returns the workbench window count.
	 * <p>
	 * @return the workbench window count
	 */
	public int getWorkbenchWindowCount() {
		return windowManager.getWindows().length;
	}
	/**
	 * @see IWorkbench
	 */
	public IWorkbenchWindow[] getWorkbenchWindows() {
		Window[] windows = windowManager.getWindows();
		IWorkbenchWindow[] dwindows = new IWorkbenchWindow[windows.length];
		System.arraycopy(windows, 0, dwindows, 0, windows.length);
		return dwindows;
	}
	/**
	 * Implements IWorkbench
	 * 
	 * @see org.eclipse.ui.IWorkbench#getWorkingSetManager()
	 * @since 2.0
	 */
	public IWorkingSetManager getWorkingSetManager() {
		return WorkbenchPlugin.getDefault().getWorkingSetManager();
	}
	/**
	 * Initializes the workbench.
	 *
	 * @return true if init succeeded.
	 */
	private boolean init(String[] commandLineArgs) {
		KeyBindingManager.getInstance();
		
		isStarting = true;

		this.commandLineArgs = commandLineArgs;
		if (WorkbenchPlugin.getDefault().isDebugging()) {
			WorkbenchPlugin.DEBUG = true;
			ModalContext.setDebugMode(true);
		}
		initializeProductImage();
		connectToWorkspace();
		addAdapters();
		windowManager = new WindowManager();
		WorkbenchColors.startup();
		initializeFonts();
		initializeAcceleratorConfiguration();
		initializeSingleClickOption();

		boolean avoidDeadlock = true;
		for (int i = 0; i < commandLineArgs.length; i++) {
			if (commandLineArgs[i].equalsIgnoreCase("-allowDeadlock")) //$NON-NLS-1$
				avoidDeadlock = false;
		}

		// deadlock code
		if (avoidDeadlock) {
			try {
				Display display = Display.getCurrent();
				UIWorkspaceLock uiLock = new UIWorkspaceLock(WorkbenchPlugin.getPluginWorkspace(), display);
				WorkbenchPlugin.getPluginWorkspace().setWorkspaceLock(uiLock);
				display.setSynchronizer(new UISynchronizer(display, uiLock));
			} catch (CoreException e) {
				e.printStackTrace(System.out);
			}
		}

		int restoreCode = openPreviousWorkbenchState();
		if (restoreCode == RESTORE_CODE_EXIT)
			return false;
		if (restoreCode == RESTORE_CODE_RESET)
			openFirstTimeWindow();
		
		forceOpenPerspective();
		openWelcomeEditors();
		refreshFromLocal();
		isStarting = false;
		return true;
	}
	private void refreshFromLocal() {
		IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
		boolean refresh = store.getBoolean(IPreferenceConstants.REFRESH_WORKSPACE_ON_STARTUP);
		if(!refresh)
			return;
		//Do not refresh if it was already done by core on startup.
		for (int i = 0; i < commandLineArgs.length; i++)
			if(commandLineArgs[i].equalsIgnoreCase("-refresh"))
				return;
		IWorkbenchWindow windows[] = getWorkbenchWindows();
		Shell shell = windows[windows.length - 1].getShell();
		ProgressMonitorDialog dlg = new ProgressMonitorDialog(shell);
		final CoreException ex[] = new CoreException[1];
		try {
			dlg.run(true,true,new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						IContainer root = ResourcesPlugin.getWorkspace().getRoot();
						root.refreshLocal(IResource.DEPTH_INFINITE,monitor);
					} catch (CoreException e) {
						ex[0] = e;
					}
				}
			});
			if(ex[0] != null) {
				String errorTitle = WorkbenchMessages.getString("Workspace.problemsTitle"); //$NON-NLS-1$
				String msg = WorkbenchMessages.getString("Workspace.problemMessage"); //$NON-NLS-1$
				ErrorDialog.openError(shell,errorTitle,msg,ex[0].getStatus());
			}
		} catch (InterruptedException e) {
			//Do nothing. Operation was canceled.
		} catch (InvocationTargetException e) {
			String msg = "InvocationTargetException refreshing from local on startup"; //$NON-NLS-1$
			WorkbenchPlugin.log(msg,new Status(Status.ERROR,PlatformUI.PLUGIN_ID,0,msg,e.getTargetException()));
		}
	}

	private void forceOpenPerspective() {
		if (getWorkbenchWindowCount() == 0) {
			// Something is wrong, there should be at least
			// one workbench window open by now.
			return;
		}
		
		String perspId = null;
		for (int i = 0; i < commandLineArgs.length - 1; i++) {
			if (commandLineArgs[i].equalsIgnoreCase("-perspective")) { //$NON-NLS-1$
				perspId = commandLineArgs[i+1];
				break;
			}
		}
		if (perspId == null)
			return;
		IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspId);
		if (desc == null)
			return;

		IWorkbenchWindow win = getActiveWorkbenchWindow();
		if (win == null)
			win = getWorkbenchWindows()[0];
		try {
			showPerspective(perspId, win);
		} catch (WorkbenchException e) {
			String msg = "Workbench exception showing specified command line perspective on startup."; //$NON-NLS-1$
			WorkbenchPlugin.log(msg, new Status(Status.ERROR, PlatformUI.PLUGIN_ID,0, msg, e));
		}
	}
	
	private void initializeSingleClickOption() {
		IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
		boolean openOnSingleClick = store.getBoolean(IPreferenceConstants.OPEN_ON_SINGLE_CLICK);
		boolean selectOnHover = store.getBoolean(IPreferenceConstants.SELECT_ON_HOVER); 
		boolean openAfterDelay = store.getBoolean(IPreferenceConstants.OPEN_AFTER_DELAY);
		int singleClickMethod = openOnSingleClick ? OpenStrategy.SINGLE_CLICK : OpenStrategy.DOUBLE_CLICK;
		if(openOnSingleClick) {
			if(selectOnHover)
				singleClickMethod |= OpenStrategy.SELECT_ON_HOVER;
			if(openAfterDelay)
				singleClickMethod |= OpenStrategy.ARROW_KEYS_OPEN;
		}
		OpenStrategy.setOpenMethod(singleClickMethod);
	}
	/**
	 * Initialize the workbench AcceleratorConfiguration with the stored values.
	 */
	private void initializeAcceleratorConfiguration() {
		IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
		String id = store.getString(IWorkbenchConstants.ACCELERATOR_CONFIGURATION_ID);
		
		if (id == null)
			id = IWorkbenchConstants.DEFAULT_ACCELERATOR_CONFIGURATION_ID;

		Path configuration = 
			KeyBindingManager.getInstance().getConfigurationForId(id);
		
		if (configuration == null)
			configuration = Path.create();
		
		KeyBindingManager.getInstance().setConfiguration(configuration);
	}
	/**
	 * Initialize the workbench fonts with the stored values.
	 */
	private void initializeFonts() {
		IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
		FontRegistry registry = JFaceResources.getFontRegistry();
		initializeFont(JFaceResources.DIALOG_FONT, registry, store);
		initializeFont(JFaceResources.BANNER_FONT, registry, store);
		initializeFont(JFaceResources.HEADER_FONT, registry, store);
		initializeFont(JFaceResources.TEXT_FONT, registry, store);
	}
	/**
	 * Initialize the specified font with the stored value.
	 */
	private void initializeFont(String fontKey, FontRegistry registry, IPreferenceStore store) {
		if (store.isDefault(fontKey))
			return;
		FontData[] font = PreferenceConverter.getFontDataArray(store, fontKey);
		registry.put(fontKey, font);
	}
	/**
	 * Initialize the product image obtained from the product info file
	 */
	private void initializeProductImage() {
		ImageDescriptor descriptor = getAboutInfo().getWindowImage();
		if (descriptor != null) {
			WorkbenchImages.getImageRegistry().put(IWorkbenchGraphicConstants.IMG_OBJS_DEFAULT_PROD, descriptor);
			Image image = WorkbenchImages.getImage(IWorkbenchGraphicConstants.IMG_OBJS_DEFAULT_PROD);
			if (image != null) {
				Window.setDefaultImage(image);
			}
		} else {
			// Avoid setting a missing image as the window default image
			WorkbenchImages.getImageRegistry().put(IWorkbenchGraphicConstants.IMG_OBJS_DEFAULT_PROD, ImageDescriptor.getMissingImageDescriptor());
		}			
	}
	/**
	 * Returns true if the workbench is in the process of closing
	 */
	public boolean isClosing() {
		return isClosing;
	}
	/**
	 * Returns true if the workbench is in the process of starting
	 */
	public boolean isStarting() {
		return isStarting;
	}
	/*
	 * Create the initial workbench window.
	 * @return true if the open succeeds
	 */
	private void openFirstTimeWindow() {
		// Create the window.
		WorkbenchWindow newWindow = new WorkbenchWindow(this, getNewWindowNumber());
		newWindow.create();
		windowManager.add(newWindow);

		// Create the initial page.
		try {
			IContainer root = WorkbenchPlugin.getPluginWorkspace().getRoot();
			newWindow.openPage(getPerspectiveRegistry().getDefaultPerspective(), root);
		} catch (WorkbenchException e) {
			ErrorDialog.openError(newWindow.getShell(), WorkbenchMessages.getString("Problems_Opening_Page"), //$NON-NLS-1$
			e.getMessage(),e.getStatus());
		}
		newWindow.open();
	}
	/*
	 * Create the workbench UI from a persistence file.
	 */
	private int openPreviousWorkbenchState() {
		// Read the workbench state file.
		final File stateFile = getWorkbenchStateFile();
		// If there is no state file cause one to open.
		if (!stateFile.exists())
			return RESTORE_CODE_RESET;

		final int result[] = { RESTORE_CODE_OK };
		Platform.run(new SafeRunnable(WorkbenchMessages.getString("ErrorReadingState")) { //$NON-NLS-1$
			public void run() throws Exception {
				FileInputStream input = new FileInputStream(stateFile);
				InputStreamReader reader = new InputStreamReader(input, "utf-8"); //$NON-NLS-1$
				IMemento memento = XMLMemento.createReadRoot(reader);
				
				// Validate known version format
				String version = memento.getString(IWorkbenchConstants.TAG_VERSION);
				boolean valid = false;
				for (int i = 0; i < VERSION_STRING.length; i++) {
					if(VERSION_STRING[i].equals(version)) {
						valid = true;
						break;
					}
				}
				if(!valid) {
					reader.close();
					MessageDialog.openError(
						(Shell) null,
						WorkbenchMessages.getString("Restoring_Problems"), //$NON-NLS-1$
						WorkbenchMessages.getString("Invalid_workbench_state_ve")); //$NON-NLS-1$
					stateFile.delete();
					result[0] = RESTORE_CODE_RESET;
					return;
				}
				
				// Validate compatible version format
				// We no longer support the release 1.0 format
				if (VERSION_STRING[0].equals(version)) {
					reader.close();
					boolean ignoreSavedState = new MessageDialog(
						null, 
						WorkbenchMessages.getString("Workbench.incompatibleUIState"), //$NON-NLS-1$
						null,
						WorkbenchMessages.getString("Workbench.incompatibleSavedStateVersion"), //$NON-NLS-1$
						MessageDialog.WARNING,
						new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 
						0).open() == 0; 	// OK is the default
					if (ignoreSavedState) {
						stateFile.delete();
						result[0] = RESTORE_CODE_RESET;
					} else {
						result[0] = RESTORE_CODE_EXIT;
					}
					return;
				}
				
				// Restore the saved state
				IStatus restoreResult = restoreState(memento);
				reader.close();
				if(restoreResult.getSeverity() == IStatus.ERROR) {
					ErrorDialog.openError(
						null,
						WorkbenchMessages.getString("Workspace.problemsTitle"),
						WorkbenchMessages.getString("Workbench.problemsRestoringMsg"),
						restoreResult);
				}
			}
			public void handleException(Throwable e) {
				super.handleException(e);
				result[0] = RESTORE_CODE_RESET;
				stateFile.delete();
			}

		});
		// ensure at least one window was opened
		if (result[0] == RESTORE_CODE_OK && windowManager.getWindows().length == 0)
			result[0] = RESTORE_CODE_RESET;
		return result[0];
	}
	/**
	 * Open the system summary editor
	 */
	public void openSystemSummaryEditor() {
		openEditor(new SystemSummaryEditorInput(), "org.eclipse.ui.SystemSummaryEditor", null); //$NON-NLS-1$
	}
		
	/**
	 * Open the Welcome editor for the primary feature or for a new feature
	 */
	private void openWelcomeEditors() {
		AboutInfo info = getAboutInfo();
		AboutInfo[] newFeatures = getNewFeaturesInfo();
		
		if (WorkbenchPlugin.getDefault().getPreferenceStore().getBoolean(IPreferenceConstants.WELCOME_DIALOG)) {
			// Show the quick start wizard the first time the workbench opens.

			// See if a welcome page is specified
			URL url = info.getWelcomePageURL();
			if (url == null)
				return;

			// Don't show it again
			WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.WELCOME_DIALOG, false);
	
			openEditor(new WelcomeEditorInput(info), WELCOME_EDITOR_ID, null);
		} else {
			// Show the welcome page for any newly installed features

			// Get the infos with welcome pages
			ArrayList welcomeFeatures = new ArrayList();
			IPluginRegistry registry = Platform.getPluginRegistry();
			for (int i = 0; i < newFeatures.length; i++) {
				if (newFeatures[i].getWelcomePageURL() != null) {
					if(newFeatures[i].getFeatureId() != null && newFeatures[i].getWelcomePerspective() != null) {
						IPluginDescriptor desc = registry.getPluginDescriptor(newFeatures[i].getFeatureId());
						//activates the feature plugin so it can run some install code.
						try {
							desc.getPlugin();
						} catch (CoreException e) {
						}
					}
					welcomeFeatures.add(newFeatures[i]);
				}
			}
			
			int wCount = getWorkbenchWindowCount();
			for (int i = 0; i < welcomeFeatures.size(); i++) {
				AboutInfo newInfo = (AboutInfo)welcomeFeatures.get(i);
				String id = newInfo.getWelcomePerspective();
				if(id == null || i >= wCount) //Other editors were already opened in restoreState(..)
					openEditor(new WelcomeEditorInput(newInfo), WELCOME_EDITOR_ID, id);
			}
		}
	}

	
	/**
	 * Open an editor for the given input
	 */
	private void openEditor(IEditorInput input, String editorId, String perspectiveId) {
		if (getWorkbenchWindowCount() == 0) {
			// Something is wrong, there should be at least
			// one workbench window open by now.
			return;
		}
		
		IWorkbenchWindow win = null;
		if(perspectiveId == null) {
			win = getActiveWorkbenchWindow();
		} else {
			IContainer root = WorkbenchPlugin.getPluginWorkspace().getRoot();
			try {
				win = openWorkbenchWindow(perspectiveId,root);
			} catch (WorkbenchException e) {
				if (WorkbenchPlugin.DEBUG) // only report ini problems if the -debug command line argument is used
					WorkbenchPlugin.log("Error opening window in Workbench.openEditor(..)");
				return;
			}
		}
		
		if (win == null)
			win = getWorkbenchWindows()[0];
		
		WorkbenchPage page = (WorkbenchPage)win.getActivePage();
		String id = perspectiveId;
		if (id == null)
			id = WorkbenchPlugin.getDefault().getPerspectiveRegistry().getDefaultPerspective();
				
		if (page == null) {
			// Create the page. 
			try {
				IContainer root = WorkbenchPlugin.getPluginWorkspace().getRoot();
				page = (WorkbenchPage)getActiveWorkbenchWindow().openPage(id, root);
			} catch (WorkbenchException e) {
				ErrorDialog.openError(
					win.getShell(), 
					WorkbenchMessages.getString("Problems_Opening_Page"), //$NON-NLS-1$
					e.getMessage(),
					e.getStatus());
			}
		}
		
		if (page == null)
			return;
		
		if (page.getActivePerspective() == null) {
			try {
				page = (WorkbenchPage)showPerspective(id, win);
			} catch (WorkbenchException e) {
				IStatus status = new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 1, WorkbenchMessages.getString("QuickStartAction.openEditorException"), e); //$NON-NLS-1$
				ErrorDialog.openError(
					win.getShell(),
					WorkbenchMessages.getString("Workbench.openEditorErrorDialogTitle"),  //$NON-NLS-1$
					WorkbenchMessages.getString("Workbench.openEditorErrorDialogMessage"),  //$NON-NLS-1$
					status);
				return;
			}
		}
		
		page.setEditorAreaVisible(true);
		
		// see if we already have an editor
		IEditorPart editor = page.findEditor(input);
		if (editor != null) {	
			page.activate(editor);
				return;
		}

		try {
			page.openEditor(input, editorId);
		} catch (PartInitException e) {
			IStatus status = new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 1, WorkbenchMessages.getString("QuickStartAction.openEditorException"), e); //$NON-NLS-1$
			ErrorDialog.openError(
				win.getShell(),
				WorkbenchMessages.getString("Workbench.openEditorErrorDialogTitle"),  //$NON-NLS-1$
				WorkbenchMessages.getString("Workbench.openEditorErrorDialogMessage"),  //$NON-NLS-1$
				status);
		}
		return;
	}
	/**
	 * Opens a new window and page with the default perspective.
	 */
	public IWorkbenchWindow openWorkbenchWindow(IAdaptable input) throws WorkbenchException {
		return openWorkbenchWindow(getPerspectiveRegistry().getDefaultPerspective(), input);
	}
	/**
	 * Opens a new workbench window and page with a specific perspective.
	 */
	public IWorkbenchWindow openWorkbenchWindow(final String perspID, final IAdaptable input) throws WorkbenchException {
		// Run op in busy cursor.
		final Object[] result = new Object[1];
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				try {
					result[0] = busyOpenWorkbenchWindow(perspID, input);
				} catch (WorkbenchException e) {
					result[0] = e;
				}
			}
		});
		if (result[0] instanceof IWorkbenchWindow)
			return (IWorkbenchWindow) result[0];
		else if (result[0] instanceof WorkbenchException)
			throw (WorkbenchException) result[0];
		else
			throw new WorkbenchException(WorkbenchMessages.getString("Abnormal_Workbench_Conditi")); //$NON-NLS-1$
	}
	
	/**
	 * Reads the about, platform and product info.
	 * This info contains the info to show in the about dialog,
	 * the platform and product name, product images, copyright etc.
	 *
	 * @return true if the method succeeds 
	 */
	private boolean readInfo() {
		// determine the identifier of the "dominant" application 
		IPlatformConfiguration conf = BootLoader.getCurrentPlatformConfiguration();
		String versionedFeatureId = conf.getPrimaryFeatureIdentifier();
		
		if (versionedFeatureId == null) {
			aboutInfo = new AboutInfo(null, null); // Ok to pass null
		} else {	
			int index = versionedFeatureId.lastIndexOf("_"); //$NON-NLS-1$
			if (index == -1)
				aboutInfo = new AboutInfo(versionedFeatureId, null); 
			else {
				String mainPluginName = versionedFeatureId.substring(0, index);
				PluginVersionIdentifier mainPluginVersion = null;
				try {
					mainPluginVersion =
						new PluginVersionIdentifier(versionedFeatureId.substring(index + 1));
				} catch (Exception e) {
					IStatus iniStatus = new Status(IStatus.ERROR, WorkbenchPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
						0, "Unknown plugin version " + versionedFeatureId, e); //$NON-NLS-1$
					WorkbenchPlugin.log("Problem obtaining configuration info ", iniStatus);//$NON-NLS-1$
				}
				aboutInfo = new AboutInfo(mainPluginName, mainPluginVersion);
			} 
		}
		
		boolean success = true;

		try {
			aboutInfo.readINIFile();
		} catch (CoreException e) {
			WorkbenchPlugin.log("Error reading about info file", e.getStatus()); //$NON-NLS-1$
			success = false;
		}

		return success;
	}

	private VersionedIdentifier[] collectFeatures() {	
		try {
		   ArrayList result = new ArrayList();
		   ILocalSite localSite = SiteManager.getLocalSite();
		   IInstallConfiguration config = localSite.getCurrentConfiguration();
		   IConfiguredSite [] csites = config.getConfiguredSites();
		   for (int i=0; i<csites.length; i++) {
		      IConfiguredSite csite = csites[i];
		      // get handles to the configured features in the site
		      IFeatureReference [] crefs = csite.getConfiguredFeatures();
		      for (int j=0; j<crefs.length; j++) {
		         IFeatureReference cref = crefs[j];
		         try {
		         	VersionedIdentifier vi = cref.getVersionedIdentifier();
		         	result.add(vi);
		         } catch (CoreException ex) {
		         }
		      }
		   }
		   return (VersionedIdentifier[])result.toArray(new VersionedIdentifier[result.size()]);
		} catch (CoreException e) {
			return new VersionedIdentifier[0];
		}
	}

	/**
	 * Reads the about info for all the configured features.
	 */
	private void readFeaturesInfo() {
		// get the previous features
		VersionedIdentifier featureEntries[] = collectFeatures();
		IDialogSettings settings = WorkbenchPlugin.getDefault().getDialogSettings();
		String[] oldFeaturesArray = settings.getArray(INSTALLED_FEATURES);
		List oldFeatures = null;
		if (oldFeaturesArray != null)
			 oldFeatures = Arrays.asList(oldFeaturesArray);

		ArrayList aboutInfos = new ArrayList();
		ArrayList newAboutInfos = new ArrayList();
		
		String[] idArray = new String[featureEntries.length];	
		for (int i = 0; i < featureEntries.length; i++) {
			VersionedIdentifier entry = featureEntries[i];
			String id = entry.getIdentifier();
			PluginVersionIdentifier vid = entry.getVersion();
			String versionedId = id + ":" + vid;
			idArray[i] = versionedId;

			try {
				AboutInfo info = new AboutInfo(id, vid);
				aboutInfos.add(info);
				if (oldFeatures != null && !oldFeatures.contains(versionedId))
					// only report a feature as new if we have a previous record of old features
					newAboutInfos.add(info);
			} catch (RuntimeException e) {
				if (WorkbenchPlugin.DEBUG) // only report ini problems if the -debug command line argument is used
					WorkbenchPlugin.log("Error parsing version \"" + vid + "\" for plugin: " + id + " in Workbench.readFeaturesInfo()");
				// continue
			}
		}
		settings.put(INSTALLED_FEATURES, idArray);

		// ensure a consistent ordering
		Collections.sort(aboutInfos, new Comparator() {
			Collator coll = Collator.getInstance();
			public int compare(Object a, Object b) {
				AboutInfo infoA = (AboutInfo) a;
				AboutInfo infoB = (AboutInfo) b;
				int c = coll.compare(infoA.getFeatureId(), infoB.getFeatureId());
				if (c == 0) {
					c = infoA.getVersionId().isGreaterThan(infoB.getVersionId()) ? 1 : -1;
				}
				return c;
			}
		});
		
		featuresInfo = new AboutInfo[aboutInfos.size()];
		aboutInfos.toArray(featuresInfo);
		
		
		for (int i = 0; i < featuresInfo.length; i++) {
			try {
				featuresInfo[i].readINIFile();
				// Exclude any feature for which there is no corresponding plug-in
				if (featuresInfo[i].getDescriptor() == null) {
					aboutInfos.remove(featuresInfo[i]);
					newAboutInfos.remove(featuresInfo[i]);
				}
			} catch (CoreException e) {
				if (WorkbenchPlugin.DEBUG) // only report ini problems if the -debug command line argument is used
					WorkbenchPlugin.log("Error reading about info file for feature: " + featuresInfo[i].getFeatureId(), e.getStatus()); //$NON-NLS-1$
			}
		}
		
		newFeaturesInfo = new AboutInfo[newAboutInfos.size()];
		newAboutInfos.toArray(newFeaturesInfo);
		if (aboutInfos.size() < featuresInfo.length) {
			featuresInfo = new AboutInfo[aboutInfos.size()];
			aboutInfos.toArray(featuresInfo);
		}
	}

	/**
	 * Record the workbench UI in a document
	 */
	private XMLMemento recordWorkbenchState() {
		XMLMemento memento = XMLMemento.createWriteRoot(IWorkbenchConstants.TAG_WORKBENCH);
		IStatus status = saveState(memento);
		if(status.getSeverity() != IStatus.OK) {
			ErrorDialog.openError((Shell)null,
				WorkbenchMessages.getString("Workbench.problemsSaving"),  //$NON-NLS-1$
				WorkbenchMessages.getString("Workbench.problemsSavingMsg"), //$NON-NLS-1$
				status);
		}
		return memento;
	}
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public boolean restart() {
		return close(EXIT_RESTART); // this is the return code from run() to trigger a restart 
	}
	/**
	 * Restores the state of the previously saved workbench
	 */
	private IStatus restoreState(IMemento memento) {

		MultiStatus result = new MultiStatus(
			PlatformUI.PLUGIN_ID,IStatus.OK,
			WorkbenchMessages.getString("Workbench.problemsRestoring"),null);
		// Read perspective history.
		// This must be done before we recreate the windows, because it is
		// consulted during the recreation.
		IMemento childMem = memento.getChild(IWorkbenchConstants.TAG_PERSPECTIVE_HISTORY);
		if (childMem != null)
			result.add(getPerspectiveHistory().restoreState(childMem));
				
		IMemento mruMemento = memento.getChild(IWorkbenchConstants.TAG_MRU_LIST); //$NON-NLS-1$
		if (mruMemento != null) {
			result.add(getEditorHistory().restoreState(mruMemento));
		}

		// Get the child windows.
		IMemento[] children = memento.getChildren(IWorkbenchConstants.TAG_WINDOW);
		IPerspectiveRegistry reg = WorkbenchPlugin.getDefault().getPerspectiveRegistry();

		AboutInfo newFeaturesWithPerspectives[] = collectNewFeaturesWithPerspectives();
		// Read the workbench windows.
		for (int x = 0; x < children.length; x++) {
			childMem = children[x];
			WorkbenchWindow newWindow = new WorkbenchWindow(this, getNewWindowNumber());
			newWindow.create();
			IPerspectiveDescriptor desc = null;
			if(x < newFeaturesWithPerspectives.length)
				desc = reg.findPerspectiveWithId(newFeaturesWithPerspectives[x].getWelcomePerspective());

			result.merge(newWindow.restoreState(childMem,desc));
			if(desc != null) {
				IWorkbenchPage page = newWindow.getActivePage();
				if(page == null) {
					IWorkbenchPage pages[] = newWindow.getPages();
					if(pages != null && pages.length > 0)
						page = pages[0];
				}
				if(page == null) {
					IContainer root = WorkbenchPlugin.getPluginWorkspace().getRoot();
					try {
						page = (WorkbenchPage)getActiveWorkbenchWindow().openPage(newFeaturesWithPerspectives[x].getWelcomePerspective(), root);				
					} catch (WorkbenchException e) {
						result.add(e.getStatus());
					}
				} else {
					page.setPerspective(desc);
				}
				newWindow.setActivePage(page);
				try {				
					page.openEditor(new WelcomeEditorInput(newFeaturesWithPerspectives[x]),WELCOME_EDITOR_ID,true);
				} catch (PartInitException e) {
					result.add(e.getStatus());
				}
			}
			windowManager.add(newWindow);
			newWindow.open();
		}
		return result;
	}
	/**
	 * Return an array with all new welcome perspectives declared in the
	 * new installed features.
	 */
	private AboutInfo[] collectNewFeaturesWithPerspectives() {
		ArrayList result = new ArrayList();
		AboutInfo newFeatures[] = getNewFeaturesInfo();
		for (int i = 0; i < newFeatures.length; i++) {
			AboutInfo info = newFeatures[i];
			if(info.getWelcomePerspective() != null && info.getWelcomePageURL() != null)
				result.add(info);
		}
		return (AboutInfo[])result.toArray(new AboutInfo[result.size()]);
	}
	/**
	 * Returns an array of all plugins that extend org.eclipse.ui.startup.
	 */
	public IPluginDescriptor[] getEarlyActivatedPlugins() {
		IPluginRegistry registry = Platform.getPluginRegistry();
		String pluginId = "org.eclipse.ui"; 
		String extensionPoint = "startup";
		
		IExtensionPoint point = registry.getExtensionPoint(pluginId, extensionPoint);
		IExtension[] extensions = point.getExtensions();
		IPluginDescriptor result[] = new IPluginDescriptor[extensions.length];
		for (int i = 0; i < extensions.length; i++) {
			result[i] = extensions[i].getDeclaringPluginDescriptor();
		}
		return result;
	}
	/**
	 * Starts plugins on startup.
	 */
	protected void startPlugins() {
		Runnable work = new Runnable() {
			IPreferenceStore store = getPreferenceStore();
			final String pref = store.getString(IPreferenceConstants.PLUGINS_NOT_ACTIVATED_ON_STARTUP);
			public void run() {
				IPluginDescriptor descriptors[] = getEarlyActivatedPlugins();
				for (int i = 0; i < descriptors.length; i++) {
					final IPluginDescriptor pluginDescriptor = descriptors[i]; 
					SafeRunnable code = new SafeRunnable() {
						public void run() throws Exception {
							String id = pluginDescriptor.getUniqueIdentifier() + IPreferenceConstants.SEPARATOR;
							if(pref.indexOf(id) < 0) {
								Plugin plugin = pluginDescriptor.getPlugin();
								IStartup startup = (IStartup)plugin;
								startup.earlyStartup();
							}
						}
						public void handleException(Throwable exception) {
							WorkbenchPlugin.log("Unhandled Exception", new Status(IStatus.ERROR, "org.eclipse.ui", 0, "Unhandled Exception", exception));
						}
					};
					Platform.run(code);
				}
			}
		};
		
		Thread thread = new Thread(work);
		thread.start();
	}
	/**
	 * Runs the workbench.
	 */
	public Object run(Object arg) {
		String[] commandLineArgs = new String[0];
		if (arg != null && arg instanceof String[])
			commandLineArgs = (String[]) arg;
		if (!readInfo())
			return null;
		String appName = getAboutInfo().getAppName();
		if (appName != null)
			Display.setAppName(appName);
		Display display = new Display();
		//Workaround for 1GEZ9UR and 1GF07HN
		display.setWarnings(false);
		display.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = close();
			}
		});
		try {
			handler = new ExceptionHandler(this);
			Window.setExceptionHandler(handler);
			boolean initOK = init(commandLineArgs);
			Platform.endSplash();
			runEventLoop = true;
			if (initOK) 
				checkUpdates(); // may trigger a close/restart
			if (initOK && runEventLoop) {
				startPlugins();
				runEventLoop();
			}
			shutdown();
		} finally {
			if (!display.isDisposed())
			  display.dispose();
		}
		return returnCode;
	}
	/**
	 * run an event loop for the workbench.
	 */
	protected void runEventLoop() {
		Display display = Display.getCurrent();
		runEventLoop = true;
		while (runEventLoop) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			} catch (Throwable t) {
				handler.handleException(t);
			}
		}
	}
	/**
	 * Saves the current state of the workbench so it can be restored later on
	 */
	private IStatus saveState(IMemento memento) {
		MultiStatus result = new MultiStatus(
			PlatformUI.PLUGIN_ID,IStatus.OK,
			WorkbenchMessages.getString("Workbench.problemsSaving"),null);

		// Save the version number.
		memento.putString(IWorkbenchConstants.TAG_VERSION, VERSION_STRING[1]);

		// Save the workbench windows.
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int nX = 0; nX < windows.length; nX++) {
			WorkbenchWindow window = (WorkbenchWindow) windows[nX];
			IMemento childMem = memento.createChild(IWorkbenchConstants.TAG_WINDOW);
			result.merge(window.saveState(childMem));
		}
		result.add(getEditorHistory().saveState(memento.createChild(IWorkbenchConstants.TAG_MRU_LIST))); //$NON-NLS-1$
		// Save perspective history.
		result.add(getPerspectiveHistory().saveState(memento.createChild(IWorkbenchConstants.TAG_PERSPECTIVE_HISTORY))); //$NON-NLS-1$
		return result;
	}
	/**
	 * Save the workbench UI in a persistence file.
	 */
	private boolean saveWorkbenchState(XMLMemento memento) {
		// Save it to a file.
		File stateFile = getWorkbenchStateFile();
		try {
			FileOutputStream stream = new FileOutputStream(stateFile);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "utf-8"); //$NON-NLS-1$
			memento.save(writer);
			writer.close();
		} catch (IOException e) {
			stateFile.delete();
			MessageDialog.openError((Shell) null, WorkbenchMessages.getString("SavingProblem"), //$NON-NLS-1$
			WorkbenchMessages.getString("ProblemSavingState")); //$NON-NLS-1$
			return false;
		}

		// Success !
		return true;
	}
	/*
	 * Sets the active accelerator configuration to be the configuration
	 * with the given id.
	 */
	public void setActiveAcceleratorConfiguration(AcceleratorConfiguration config) {
		if (config != null) {
			acceleratorConfiguration = config; 
 			String id = config.getId();			
			Path configuration = KeyBindingManager.getInstance().getConfigurationForId(id);
		
			if (configuration == null)
				configuration = Path.create();
	
			KeyBindingManager.getInstance().setConfiguration(configuration);
		}	
	}

	/**
	 * @see IExecutableExtension
	 */
	public void setInitializationData(IConfigurationElement configElement, String propertyName, Object data) {
		startingPlugin = configElement.getDeclaringExtension().getDeclaringPluginDescriptor();
		productInfoFilename = (String) ((Map) data).get(P_PRODUCT_INFO);
	}
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchPage showPerspective(String perspectiveId, IWorkbenchWindow window)
		throws WorkbenchException
	{
		Assert.isNotNull(perspectiveId);
		
		// If the specified window has the requested perspective open, then the window
		// is given focus and the perspective is shown. The page's input is ignored.
		WorkbenchWindow win = (WorkbenchWindow) window;
		if (win != null) {
			WorkbenchPage page = win.getActiveWorkbenchPage();
			if (page != null) {
				IPerspectiveDescriptor perspectives[] = page.getOpenedPerspectives();
				for (int i = 0; i < perspectives.length; i++) {
					IPerspectiveDescriptor persp = perspectives[i];
					if (perspectiveId.equals(persp.getId())) {
						win.getShell().open();
						page.setPerspective(persp);
						return page;
					}
				}
			}
		}
		
		// If another window that has the workspace root as input and the requested
		// perpective open and active, then the window is given focus.
		IAdaptable input = WorkbenchPlugin.getPluginWorkspace().getRoot();
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			win = (WorkbenchWindow) windows[i];
			if (window != win) {
				WorkbenchPage page = win.getActiveWorkbenchPage();
				if (page != null) {
					boolean inputSame = false;
					if (input == null)
						inputSame = (page.getInput() == null);
					else
						inputSame = input.equals(page.getInput());
					if (inputSame) {
						Perspective persp = page.getActivePerspective();
						if (perspectiveId.equals(persp.getDesc().getId())) {
							win.getShell().open();
							return page;
						}
					}
				}
			}
		}
			
		// Otherwise the requested perspective is opened and shown in the specified
		// window or in a new window depending on the current user preference for opening
		// perspectives, and that window is given focus.
		win = (WorkbenchWindow) window;
		if (win != null) {
			IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
			int mode = store.getInt(IPreferenceConstants.OPEN_PERSP_MODE);
			IWorkbenchPage page = win.getActiveWorkbenchPage();
			IPerspectiveDescriptor persp = null;
			if (page != null)
				persp = page.getPerspective();
			
			// Only open a new window if user preference is set and the window
			// has an active perspective.
			if (IPreferenceConstants.OPM_NEW_WINDOW == mode && persp != null) {
				IWorkbenchWindow newWindow = openWorkbenchWindow(perspectiveId, input);
				return newWindow.getActivePage();
			} else {
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
				if (desc == null)
					throw new WorkbenchException(WorkbenchMessages.getString("WorkbenchPage.ErrorRecreatingPerspective")); //$NON-NLS-1$
				win.getShell().open();
				if (page == null)
					page = win.openPage(perspectiveId, input);
				else
					page.setPerspective(desc);
				return page;
			}
		}

		// Just throw an exception....
		throw new WorkbenchException(WorkbenchMessages.format("Workbench.showPerspectiveError", new Object[] { perspectiveId })); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchPage showPerspective(String perspectiveId, IWorkbenchWindow window, IAdaptable input) 
		throws WorkbenchException
	{
		Assert.isNotNull(perspectiveId);

		// If the specified window has the requested perspective open and the same requested
		// input, then the window is given focus and the perspective is shown.
		boolean inputSameAsWindow = false;
		WorkbenchWindow win = (WorkbenchWindow) window;
		if (win != null) {
			WorkbenchPage page = win.getActiveWorkbenchPage();
			if (page != null) {
				boolean inputSame = false;
				if (input == null)
					inputSame = (page.getInput() == null);
				else
					inputSame = input.equals(page.getInput());
				if (inputSame) {
					inputSameAsWindow = true;
					IPerspectiveDescriptor perspectives[] = page.getOpenedPerspectives();
					for (int i = 0; i < perspectives.length; i++) {
						IPerspectiveDescriptor persp = perspectives[i];
						if (perspectiveId.equals(persp.getId())) {
							win.getShell().open();
							page.setPerspective(persp);
							return page;
						}
					}
				}
			}
		}
		
		// If another window has the requested input and the requested
		// perpective open and active, then that window is given focus.
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			win = (WorkbenchWindow) windows[i];
			if (window != win) {
				WorkbenchPage page = win.getActiveWorkbenchPage();
				if (page != null) {
					boolean inputSame = false;
					if (input == null)
						inputSame = (page.getInput() == null);
					else
						inputSame = input.equals(page.getInput());
					if (inputSame) {
						Perspective persp = page.getActivePerspective();
						if (perspectiveId.equals(persp.getDesc().getId())) {
							win.getShell().open();
							return page;
						}
					}
				}
			}
		}

		// If the specified window has the same requested input but not the requested
		// perspective, then the window is given focus and the perspective is opened and shown
		// on condition that the user preference is not to open perspectives in a new window.
		win = (WorkbenchWindow) window;
		if (inputSameAsWindow && win != null) {
			IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
			int mode = store.getInt(IPreferenceConstants.OPEN_PERSP_MODE);
			
			if (IPreferenceConstants.OPM_NEW_WINDOW != mode) {
				IWorkbenchPage page = win.getActiveWorkbenchPage();
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
				if (desc == null)
					throw new WorkbenchException(WorkbenchMessages.getString("WorkbenchPage.ErrorRecreatingPerspective")); //$NON-NLS-1$
				win.getShell().open();
				if (page == null)
					page = win.openPage(perspectiveId, input);
				else
					page.setPerspective(desc);
				return page;
			}
		}

		// If the specified window has no active perspective, then open the
		// requested perspective and show the specified window.
		if (win != null) {
			IWorkbenchPage page = win.getActiveWorkbenchPage();
			IPerspectiveDescriptor persp = null;
			if (page != null)
				persp = page.getPerspective();
			if (persp == null) {
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
				if (desc == null)
					throw new WorkbenchException(WorkbenchMessages.getString("WorkbenchPage.ErrorRecreatingPerspective")); //$NON-NLS-1$
				win.getShell().open();
				if (page == null)
					page = win.openPage(perspectiveId, input);
				else
					page.setPerspective(desc);
				return page;
			}
		}
		
		// Otherwise the requested perspective is opened and shown in a new window, and the
		// window is given focus.
		IWorkbenchWindow newWindow = openWorkbenchWindow(perspectiveId, input);
		return newWindow.getActivePage();
	}
	
	/**
	 * shutdown the application.
	 */
	private void shutdown() {
		WorkbenchColors.shutdown();		
		JFaceColors.disposeColors();
	}

	/**
	 * Creates the action delegate for each action extension contributed by
	 * a particular plugin.  The delegates are only created if the
	 * plugin itself has been activated.
	 * 
	 * @param pluginId the plugin id.
	 */
	public void refreshPluginActions(String pluginId) {
		WWinPluginAction.refreshActionList();
	}
	/*
	 * @see IWorkbench#getDecoratorManager()
	 */
	public IDecoratorManager getDecoratorManager() {
		return WorkbenchPlugin.getDefault().getDecoratorManager();
	}

}