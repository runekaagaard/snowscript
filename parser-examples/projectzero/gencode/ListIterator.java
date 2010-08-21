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

import com.ibm.p8.engine.core.PHPErrorHandler;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.RuntimeInterpreter;
import com.ibm.p8.engine.core.PHPValue.Types;
import com.ibm.p8.engine.core.object.ObjectFacade;
import com.ibm.p8.engine.core.types.PHPArray;

/**
 * Helper class implementing and keeping state for LIST_INIT and LIST_NEXT operations.
 */
public final class ListIterator {

	private final RuntimeInterpreter runtime;
	private final PHPValue list;
	private final Types listType;
	private PHPArray array;
	private String string;
	private int remaining;
	
	/**
	 * @param inRuntime .
	 * @param inList .
	 * @param inRemaining .
	 */
	public ListIterator(RuntimeInterpreter inRuntime, PHPValue inList, int inRemaining) {
		this.runtime = inRuntime;
		this.list = inList;
		this.listType = inList.getType();
		this.remaining = inRemaining;
	}
	
	/**
	 * 
	 * @param silent .
	 * @return initial value
	 */
	public PHPValue init(boolean silent) {
		switch(listType) {
		case PHPTYPE_ARRAY:
			array = list.getArray();
			break;
		case PHPTYPE_STRING:
			string = list.getPHPString().getJavaString();
			break;
		case PHPTYPE_OBJECT:
			Object[] inserts = { ObjectFacade.getPHPClass(list).getName() };
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null, "List.NotObject", inserts);
			break;
		default: // all other types
			break;
		}
		return next(silent);
	}
	
	/**
	 * 
	 * @param silent .
	 * @return the next value
	 */
	public PHPValue next(boolean silent) {
		PHPValue nextValue;
		switch (listType) {
		case PHPTYPE_ARRAY:
			nextValue = array.get(Integer.valueOf(remaining - 1), false, false);
			if (nextValue == null) {
				if (!silent) {
				    // PHP Notice:  Undefined offset:  3 in C:\arena\test\list2.php on line 6
				    Object[] inserts = { remaining - 1 };
				    runtime.raiseExecError(PHPErrorHandler.E_NOTICE, null, "ListOffset.Undefined", inserts);
				}
				nextValue = PHPValue.createNull();
			}
			break;
		case PHPTYPE_STRING:
			if (remaining <= string.length()) {
				nextValue = PHPValue.createString(string.substring(remaining - 1, remaining));
			} else {
				if (!silent) {
					// PHP Notice:  Undefined offset:  3 in C:\arena\test\list2.php on line 6
				    Object[] inserts = { remaining - 1 };
				    runtime.raiseExecError(PHPErrorHandler.E_NOTICE, null, "ListOffset.UndefinedString", inserts);
				}
				nextValue = PHPValue.createString("");
			}
			break;
		default: // all other types, object never gets this far
			nextValue = PHPValue.createNull();
			break;
		}
		remaining--;
		return nextValue;
	}
	
	/**
	 * @return the list
	 */
	public PHPValue getList() {
		return this.list;
	}
}
