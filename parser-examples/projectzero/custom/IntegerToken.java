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
package com.ibm.p8.engine.parser.custom;

import com.ibm.p8.engine.parser.core.Token;

/**
 * This token has a special slot to contain a integer number.
 * 
 */
public class IntegerToken extends Token {
	private int value;

	public int getValue() {
		return value;
	}
 
	/**
	 * Constructor.
	 * @param value - the value
	 */
	public IntegerToken(int value) {
		this.value = value;
	}
	/**
	 * Convert to string.
	 * @see com.ibm.p8.engine.parser.core.Token#toString()
	 * @return - the string.
	 */
	public String toString() {
		return Integer.toString(value);
	}
	

}
