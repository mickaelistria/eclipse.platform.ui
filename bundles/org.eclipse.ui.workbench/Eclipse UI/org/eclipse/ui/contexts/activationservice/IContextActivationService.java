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

package org.eclipse.ui.contexts.activationservice;

import java.util.Set;

/**
 * <p>
 * TODO javadoc
 * </p>
 * <p>
 * This interface is not intended to be extended or implemented by clients.
 * </p>
 * <p>
 * <em>EXPERIMENTAL</em>
 * </p>
 * 
 * @since 3.0
 * @see IContextActivationServiceListener
 */
public interface IContextActivationService {

	/**
	 * Registers an instance of <code>IContextActivationServiceListener</code> to
	 * listen for changes to properties of this instance.
	 * 
	 * @param contextActivationServiceListener
	 *            the instance of <code>IContextActivationServiceListener</code> to
	 *            register. Must not be <code>null</code>. If an attempt is
	 *            made to register an instance of <code>IContextActivationServiceListener</code>
	 *            which is already registered with this instance, or if this
	 *            instance is disposed, no operation is performed.
	 */
	void addContextActivationServiceListener(IContextActivationServiceListener contextActivationServiceListener);

	/**
	 * <p>
	 * Returns the set of identifiers to active contexts.
	 * </p>
	 * <p>
	 * Notification is sent to all registered listeners if this property
	 * changes.
	 * </p>
	 * 
	 * @return the set of identifiers to active contexts. This set may be
	 *         empty, but is guaranteed not to be <code>null</code>. If this
	 *         set is not empty, it is guaranteed to only contain instances of
	 *         <code>String</code>.
	 */
	Set getActiveContextIds();

	/**
	 * Unregisters an instance of <code>IContextActivationServiceListener</code>
	 * listening for changes to properties of this instance.
	 * 
	 * @param contextActivationServiceListener
	 *            the instance of <code>IContextActivationServiceListener</code> to
	 *            unregister. Must not be <code>null</code>. If an attempt
	 *            is made to unregister an instance of <code>IContextActivationServiceListener</code>
	 *            which is not already registered with this instance, or if
	 *            this instance is disposed, no operation is performed.
	 */
	void removeContextActivationServiceListener(IContextActivationServiceListener contextActivationServiceListener);
}