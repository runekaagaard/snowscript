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
/**
 * 
 * class for the catchstack.
 *
 */
public class CatchStackEntry {
	/**
	 * Container for program counter and stack height to be stacked for each catch block.
	 * @param inCatchPC - program counter.
	 * @param inStackSize - the stack size.
	 */
		public CatchStackEntry(int inCatchPC, int inStackSize) {
			this.catchPC = inCatchPC;
			this.stackSize = inStackSize;
		}
		private int catchPC;
		private int stackSize;
		
		public int getStackSize() {
			return this.stackSize;
		}

		public int getCatchPC() {
			return this.catchPC;
		}
}
