/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.vvt.exceptions;

public class FxConcurrentRequestNotAllowedException extends Throwable {
	private static final long serialVersionUID = 1L;
	
	public FxConcurrentRequestNotAllowedException() { super(); }
	public FxConcurrentRequestNotAllowedException(String s) { super(s); }
}
