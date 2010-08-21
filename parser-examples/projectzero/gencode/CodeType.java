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

import com.ibm.p8.engine.parser.model.Ast;


/**
 * Contains linear list of OpCodes and associated attributes during 
 * generation.
 *
 */
public class CodeType extends ArrayList<Op> {

	private static final long serialVersionUID = 1329953624153234583L;

	private int currentPushCount = 0;
	/**
	 * Resolve (fix branches) for breaks in added code.
	 * @param source AST
	 * @param generator GeneratorContext
	 * @param branches <code>ArrayList</code> defining branches to be resolved
	 */
	public void resolveBranches(Ast source, GeneratorContext generator, ArrayList<Op> branches) {
		if (branches.size() != 0) {
			// resolve branches to here
			for (Op op : branches) {
				assert op.operation == Op.BRANCH;
				int branchOffset = size() - (indexOf(op) + 1);
				op.resolveBranch(branchOffset);
			}
		}
	}
	
	/**
	 * Add the supplied code to this. 
	 * @param code code to add
	 * @return no of pushes in the supplied block
	 */
	public int add(CodeType code) {
		
		this.currentPushCount += code.currentPushCount;
		super.addAll(code);
		return code.currentPushCount;
	}
	
	/**
	 * Add the supplied code to this block - it is expected that the code will push 1 value
	 * it is an error if it does not.
	 * @param code code to add
	 */
	public void addPush1(CodeType code) {
		
		assert code.currentPushCount == 1 : "pushCount: " + code.currentPushCount + " code: " + code;
		this.add(code);
	}
	
	/**
	 * add the code to this block - it is expected that the code will pop 1 value
	 * it is an error if it does not.
	 * @param code code to add
	 */
	public void addPop1(CodeType code) {
		
		assert code.currentPushCount == -1 : "pushCount: " + code.currentPushCount + " code: " + code;
		this.add(code);
	}
	
	/**
	 * add the code to this block - it is expected that the code will push 1 or more values
	 * it is an error if it does not.
	 * @param code code to add
	 */
	public void addPush(CodeType code) {
		
		assert code.currentPushCount > 0 : "pushCount: " + code.currentPushCount + " code: " + code;
		this.add(code);
	}
	
	/**
	 * add the code to this block - the code must not push a value
	 * it is an an error if it does.
	 * @param code code to add
	 */
	public void addFlat(CodeType code) {

		assert code.currentPushCount == 0 : "pushCount: " + code.currentPushCount + " code: " + code;
		this.add(code);
	}
	
	/**
	 * add an OpCode to this block.
	 * @param opCode operation to add
	 * @return true always
	 */
	public boolean add(Op opCode) {

		this.currentPushCount += opCode.getPushCount();
		return super.add(opCode);
	}
	
	/**
	 * 
	 * @return no of values pushed by this basic block
	 */
	public int getPushCount() {
		return currentPushCount;
	}
	
	/**
	 * explicitly set the stack push count - used rarely e.g. function calls
	 * @param pushCount number of items stacked
	 */
	public void setPushCount(int pushCount) {
		currentPushCount = pushCount;
	}
	
	/**
	 * Set the last instructions tick property.
	 * @param tick .
	 */
	public void setTick(boolean tick) {
		if (tick && size() > 0) { // assertion: if there are no instructions in this code
							// its reasonable not to tick
			get(size() - 1).setTick();
		}
	}
	
	/**
	 * Remove a <code>Op</code> from list.
	 * 
	 * @param index
	 * 			index of <code>Op</code> to be removed
	 * @return <code>Op</code> removed
	 */
	public Op remove(int index) {
		Op op = super.remove(index);
		currentPushCount -= op.getPushCount();
		return op;
	}
}
