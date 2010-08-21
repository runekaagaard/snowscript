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
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;

import com.ibm.p8.engine.ast.utils.ExecutionContext;
import com.ibm.p8.engine.core.ExecutableCode;
import com.ibm.p8.engine.core.Invocable;
import com.ibm.p8.engine.core.RuntimeInterpreter;
import com.ibm.p8.engine.core.UserSpaceInvocable;
import com.ibm.p8.engine.core.util.NameString;
import com.ibm.p8.engine.parser.model.Ast;


/**
 * Context which is required during code generation and can be discarded
 * when code is complete.
 * Looking to keep code generation context separate from runtime context. (RuntimeInterpreter)
 */
public class GeneratorContext {
	
	/**
	 * The runtime is currently needed to raise errors.
	 */
	private final RuntimeInterpreter runtime;
	
	/**
	 * Lists of unresolved branches.
	 */
	private Stack<LoopNest> loopStack = new Stack<LoopNest>();
	
	/**
	 * Function table for the current script.
	 */
	private final Map<String, Invocable> functions;

	/**
	 * True if ticks checks are required for this files compile.
	 */
	private final boolean fileTicks;
	
	/**
	 * Counts the levels of nested declare ticks statements.
	 */
	private int tickNest = 0;
	
	/**
	 * True if this file is being compiled for debug.
	 */
	private final boolean fileDebug;

	/**
	 * Create a new LoopNest record.
	 */
	public void pushLoops() {
		loopStack.push(new LoopNest());
	}
	
	/**
	 * Retrieve a LoopNest record.
	 * @return the outermost branch lists
	 */
	public LoopNest popLoops() {
		// but is all resolved yet at the top level
		return loopStack.pop();
	}
	
	/**
	 * Retrieve the current LoopNest record.
	 * @return loop nesting level
	 */
	public LoopNest peekLoops() {
		return loopStack.peek();
	}
	
	/**
	 * @return the current nesting level.
	 */
	public int getLoopNest() {
		return loopStack.size();
	}
	
	/**
	 * Start a new stack of loops when generating a function or method.
	 * @return the old loop stack.
	 */
	public Stack<LoopNest> saveLoopStack() {
		Stack<LoopNest> returnStack = loopStack;
		loopStack = new Stack<LoopNest>();
		return returnStack;
	}
	
	/**
	 * Restore loop stack on return from function generation.
	 * @param inLoopStack the save loop stack.
	 */
	public void restoreLoopStack(Stack<LoopNest> inLoopStack) {
		loopStack = inLoopStack;
	}
	
	/**
	 * Add a break to be resolved later.
	 * @param op the BRANCH Op to resolve
	 * @param levels no of loops to break out
	 * @return the Op parameter.
	 */
	public Op saveUnresolvedBreak(Op op, int levels) {
		assert op.operation == Op.BRANCH;
		assert loopStack.size() >= levels;
		assert levels > 0;
		
		// add to the list of branches at the right level 
		loopStack.get(loopStack.size() - levels).getBreaks().add(op);
		return op;
	}
	
	/**
	 * Add a continue to be resolved later.
	 * @param op the BRANCH Op to resolve
	 * @param levels no of loops to break out
	 * @return the Op parameter.
	 */
	public Op saveUnresolvedContinue(Op op, int levels) {
		assert op.operation == Op.BRANCH;
		assert loopStack.size() >= levels;
		assert levels > 0;
		
		// add to the list of branches at the right level 
		loopStack.get(loopStack.size() - levels).getContinues().add(op);
		return op;
	}
	
	/**
	 * Retrieve a copy of he current unresolved breaks/continues.
	 * @return snapshot
	 */
	public ArrayList<LoopNest> breakSnapshot() {
		ArrayList<LoopNest> snapshot = new ArrayList<LoopNest>(loopStack.size());
		for (LoopNest existingLoopNest : loopStack) { // FIFO
			LoopNest newLoopNest = new LoopNest();
			newLoopNest.getBreaks().addAll(existingLoopNest.getBreaks());
			newLoopNest.getContinues().addAll(existingLoopNest.getContinues());
			snapshot.add(newLoopNest);
		}
		return snapshot;
	}
	
	/**
	 * Get a copy of the unresolved breaks.
	 * @return copy of breaks
	 */
	public ArrayList<LoopNest> breakCopy() {
		ArrayList<LoopNest> copy = new ArrayList<LoopNest>(loopStack.size());
		copy.addAll(loopStack);
		return copy;
	}
	
	/**
	 * 
	 * @param functionName name of the function
	 * @return true if the function is defined
	 */
	public boolean isFunctionDefined(NameString functionName) {
		return functions.containsKey(functionName.lowerCase());
	}
	
	/**
	 * 
	 * @param functionName  name of the function
	 * @return Invocable node
	 */
	public Invocable lookupScriptFunction(NameString functionName) {
		return functions.get(functionName.lowerCase());
	}
	
	/**
	 * Provide the current runtime.
	 * @return runtime
	 */
	public RuntimeInterpreter getRuntime() {
		return runtime;
	}
	
	/**
	 * Generate code from the root program node.
	 * @param invocable .
	 * @param program the AST program
	 * @return code - implementation of program in code
	 */
	public ExecutableCode generate(UserSpaceInvocable invocable, Ast program) {
		
		// Generate the program's code
		CodeType code = null;
	    code = program.generate(this, true, ExecutionContext.READING);
	    
	    // Generate an implicit return at the end of the script.
	    // Use the program's last child as the op's node. This ensures line
		// numbers make for sensible stepping behaviour during debug.
	    Ast lastNode;	    
	    int programChildren = program.getNumChildren();
	    if (programChildren > 0) {
	    	lastNode = program.getChild(program.getNumChildren() - 1);
	    } else {
	    	lastNode = program;
	    }
	    if (code.getPushCount() > 0) {
			code.add(new Op(lastNode, Op.RETURN, true));
	    } else {
	    	code.add(new Op(lastNode, Op.RETURN, false));
	    }
		
		return new OpCodeExecutable(code, invocable);
	}
	
	/**
	 * Is tick checking is required currently.
	 * either for the whole file or because we a re currently inside a tick block
	 * @return true for ticks
	 */
	public boolean isTick() { 
		return this.fileTicks || this.tickNest > 0;
	}
	
	/**
	 * entering declares tick section.
	 */
	public void pushTick() {
		this.tickNest++;
	}
	
	/**
	 * exiting declares tick section.
	 */
	public void popTick() {
		this.tickNest--;
	}

	/**
	 * is debug checking required.
	 * @return true for debug checking
	 */
	public boolean isDebug() {
		return this.fileDebug;
	}
	
	/**
	 * 
	 * @param runtime the current runtime.
	 * @param inFunctions - function table.
	 * @param inTick tick requirement
	 * @param inDebug debug requirement
	 */
	public GeneratorContext(RuntimeInterpreter runtime, Collection<Invocable> inFunctions, 
			boolean inTick, boolean inDebug) {
		this.runtime = runtime;
		this.functions = new IdentityHashMap<String, Invocable>(inFunctions.size());
		for (Invocable func : inFunctions) {
			this.functions.put(func.getFunctionName().lowerCase(), func);
		}
		this.fileTicks = inTick;
		this.fileDebug = inDebug;
	}
}
