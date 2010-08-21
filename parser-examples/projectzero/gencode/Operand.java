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
import com.ibm.p8.engine.core.object.PHPClass;
import com.ibm.p8.engine.core.util.NameString;

/**
 * Extended operands to opcodes.
 * Some of the less frequent operands are indirected through this class so the Opcode class does not require additional fields.
 */
public class Operand {
	public final Invocable invocable;
	public final NameString name;
	public final int[] branchTable;
	public final PHPClass phpClass;
	
	/**
	 * 
	 * @param inInvocable .
	 */
	public Operand(Invocable inInvocable) {
		this.invocable = inInvocable;
		this.name = null;
		this.branchTable = null;
		this.phpClass = null;
	}
	
	/**
	 * 
	 */
	public Operand() {
		this.invocable = null;
		this.name = null;
		this.branchTable = null;
		this.phpClass = null;
	}
	
	/**
	 * 
	 * @param inName .
	 */
	public Operand(NameString inName) {
		this.invocable = null;
		this.name = inName;
		this.branchTable = null;
		this.phpClass = null;
	}
	
	/**
	 * 
	 * @param inBranchTable .
	 */
	public Operand(int[] inBranchTable) {
		this.invocable = null;
		this.name = null;
		this.branchTable = inBranchTable;
		this.phpClass = null;
	}
	
	/**
	 * 
	 * @param inPHPClass .
	 */
	public Operand(PHPClass inPHPClass) {
		this.invocable = null;
		this.name = null;
		this.branchTable = null;
		this.phpClass = inPHPClass;
	}
}