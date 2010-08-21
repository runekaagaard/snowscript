/*
 * ============================================================================
 * Licensed Materials - Property of IBM
 * Project  Zero
 *
 * (C) Copyright IBM Corp. 2007  All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 * ============================================================================
 * Copyright (c) 1999 - 2006 The PHP Group. All rights reserved.
 * ============================================================================
 */
package com.ibm.p8.engine.parser.gencode;

import com.ibm.p8.engine.core.Invocable;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.object.PHPMethod;
import com.ibm.p8.engine.core.util.NameString;

/**
 * Container for the parameters to be held while preparing a function or method call.
 */
public class InvocableStackEntry {
	private PHPValue baseObject;
	private PHPMethod method;
	private Invocable invocable;
	private NameString methodName;
	private ImplicitCallType implicitCallType;
	
	/**
	 * What kind of implicit call type.
	 */
	public enum ImplicitCallType {
		NotImplicit,
		__Call,
		__CallStatic,
	}
	
	/**
	 * @param invocable .
	 */
	public InvocableStackEntry(Invocable invocable) {
		this.invocable = invocable;
	}
	
	/**
	 * @param baseObject .
	 * @param method .
	 * @param methodName .
	 * @param implicitCallType .
	 */
	public InvocableStackEntry(PHPValue baseObject, PHPMethod method, NameString methodName, ImplicitCallType implicitCallType) {
		this.baseObject = baseObject;
		this.method = method;
		this.invocable = method.getMethodBody();
		this.methodName = methodName;
		this.implicitCallType = implicitCallType;
	}

	public PHPValue getBaseObject() {
		return baseObject;
	}

	public PHPMethod getMethod() {
		return method;
	}

	public Invocable getInvocable() {
		return invocable;
	}

	public NameString getMethodName() {
		return methodName;
	}

	public ImplicitCallType getImplicitCallType() {
		return implicitCallType;
	}
}