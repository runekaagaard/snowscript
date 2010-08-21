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

import java.util.ArrayList;

/**
 * Container for lists of branch instructions pending resolution.
 *
 */
public class LoopNest {
	
	private ArrayList<Op> breaks = new ArrayList<Op>(); // list of Op's where offset needs resolving
	private ArrayList<Op> continues = new ArrayList<Op>(); // list of Op's where offset needs resolving
	
	/**
	 * @return the breaks
	 */
	public ArrayList<Op> getBreaks() {
		return breaks;
	}
	/**
	 * @return the continues
	 */
	public ArrayList<Op> getContinues() {
		return continues;
	}
}
