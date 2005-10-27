/******************************************************************************* * Copyright (c) 2005 IBM Corporation and others. * All rights reserved. This program and the accompanying materials * are made available under the terms of the Eclipse Public License v1.0 * which accompanies this distribution, and is available at * http://www.eclipse.org/legal/epl-v10.html * * Contributors: *     IBM Corporation - initial API and implementation ******************************************************************************/package org.eclipse.jface.menus;/** * <p> * An event describing changes to an <code>SWidget</code>. * </p> * <p> * Clients may instantiate this class, but must not extend. * </p> * <p> * <strong>EXPERIMENTAL</strong>. This class or interface has been added as * part of a work in progress. There is a guarantee neither that this API will * work nor that it will remain the same. Please do not use this API without * consulting with the Platform/UI team. * </p> *  * @since 3.2 * @see IWidgetListener#widgetChanged(WidgetEvent) */public final class WidgetEvent extends MenuElementEvent {	/**	 * The bit used to represent whether the item has changed its	 * fully-qualified command.	 */	private static final int CHANGED_WIDGET = LAST_USED_BIT_MENU_ELEMENT << 1;	/**	 * The item that has changed; this value is never <code>null</code>.	 */	private final SWidget widget;	/**	 * Creates a new instance of <code>WidgetEvent</code>.	 * 	 * @param widget	 *            The instance that has changed; must not be <code>null</code>.	 * @param widgetChanged	 *            <code>true</code>, iff the class providing the widgets	 *            changed.	 * @param locationsChanged	 *            <code>true</code> if the locations have changed;	 *            <code>false</code> otherwise.	 * @param definedChanged	 *            <code>true</code>, iff the defined property changed.	 * @param visibilityChanged	 *            <code>true</code>, iff the visibility property change.	 */	public WidgetEvent(final SWidget widget, final boolean widgetChanged,			final boolean locationsChanged, final boolean definedChanged,			final boolean visibilityChanged) {		super(locationsChanged, definedChanged, visibilityChanged);		if (widget == null)			throw new NullPointerException("A widget event needs a widget"); //$NON-NLS-1$		this.widget = widget;		if (widgetChanged) {			changedValues |= CHANGED_WIDGET;		}	}	/**	 * Returns the instance that changed.	 * 	 * @return the instance that changed. Guaranteed not to be <code>null</code>.	 */	public final SWidget getWidget() {		return widget;	}	/**	 * Returns whether or not the class providing the widgets changed.	 * 	 * @return <code>true</code>, iff the widget property changed.	 */	public final boolean isWidgetChanged() {		return ((changedValues & CHANGED_WIDGET) != 0);	}}