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
import java.util.EmptyStackException;
import com.ibm.p8.engine.core.Invocable;


/**
 * Stack of items needed during preparation of a method call.
 *
 */
public class InvocableStack {
	
	private final ArrayList<InvocableStackEntry> stack = new ArrayList<InvocableStackEntry>();
	private int tos = -1;

	/**
	 * 
	 * @return InvocableStackEntry at top of stack
	 */
	public InvocableStackEntry pop() {
		if (tos < 0) {
			throw new EmptyStackException();
		}
		return stack.get(tos--);
	}

    /**
     * 
     * @param inInvocable .
     */
    public void push(Invocable inInvocable) {
		push(new InvocableStackEntry(inInvocable));
	}
    
	/**
     * @param inEntry .
     */
	public void push(InvocableStackEntry inEntry) {
		tos++;
		if (stack.size() > tos) {
			stack.set(tos, inEntry);
		} else {
			stack.add(inEntry);
		}
		
	}
	
	/**
	 * 
	 * @return invocable 
	 */
	public Invocable peekInvocable() {
		if (tos < 0) {
			throw new EmptyStackException();
		}
		return stack.get(tos).getInvocable();
	}
	
	/**
	 * 
	 * @return invocable 
	 */
	public InvocableStackEntry peek() {
		if (tos < 0) {
			throw new EmptyStackException();
		}
		return stack.get(tos);
	}
}
