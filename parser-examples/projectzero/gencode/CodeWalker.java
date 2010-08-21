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

import java.util.Iterator;

/**
 * 
 */
public class CodeWalker implements Iterable<Op> {
	private final CodeType code;
	private final int size;

	/**
	 * 
	 * @param code .
	 */
	public CodeWalker(CodeType code) {
		this.code = code;
		this.size = code.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<Op> iterator() {
		return new Iterator<Op>() {
			// next PC
			private int pc = 0;

			public boolean hasNext() {
				return pc < size;
			}

			public Op next() {
				return code.get(pc++);
			}

			public void remove() {
				assert false;
			}
		};
	}
}
