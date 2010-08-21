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
 * This token has a special slot to contain a floating point number.
 * 
 */
public class DoubleToken extends Token {
	private double value;

	public double getValue() {
		return value;
	}

	/**
	 * Constuctor.
	 * @param value - the value
	 */
	public DoubleToken(double value) {
		this.value = value;
	}
	
	/**
	 * Convert to string.
	 * @see com.ibm.p8.engine.parser.core.Token#toString()
	 * @return - the string.
	 */
	public String toString() {
		return Double.toString(value);
	}
}
