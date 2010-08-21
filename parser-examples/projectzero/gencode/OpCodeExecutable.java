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

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.logging.Logger;

import com.ibm.p8.engine.ast.utils.ExecutionContext;
import com.ibm.p8.engine.ast.utils.PrintVisitor;
import com.ibm.p8.engine.core.Classes;
import com.ibm.p8.engine.core.ExecutableCode;
import com.ibm.p8.engine.core.ExceptionWrapper;
import com.ibm.p8.engine.core.PHPErrorHandler;
import com.ibm.p8.engine.core.PHPStack;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.RuntimeInterpreter;
import com.ibm.p8.engine.core.StackFrameImpl;
import com.ibm.p8.engine.core.StackFrame;
import com.ibm.p8.engine.core.Ticker;
import com.ibm.p8.engine.core.UserSpaceInvocable;
import com.ibm.p8.engine.core.VarMapHash;
import com.ibm.p8.engine.core.object.PHPClass;
import com.ibm.p8.engine.debug.impl.P8DebugProvider;
import com.ibm.p8.utilities.log.P8LogManager;
import com.ibm.phpj.logging.SAPIComponent;
import com.ibm.phpj.logging.SAPILevel;
import com.ibm.phpj.xapi.ConfigurationService;

/**
 * Code container. Code interpreter is here.
 * Makes opcode's callable by implementing the <code>ExecutableCode</code> interface.
 */
public class OpCodeExecutable extends ExecutableCode {

	protected static final Logger LOGGER = P8LogManager._instance.getLogger(SAPIComponent.Runtime);
	
	static final int TIMEOUT_GRANULARITY = 200;

	private final CodeType code;
	private final UserSpaceInvocable invocable;
	private final String fileName;

	/**
	 * program counter - state-full - this is only used in runtime specific
	 * instance not the cached (shared) instance.
	 */
	private int pc = -1; // -1 indicates code never began execution

	/**
	 * 
	 * @param code
	 *            the code to be executed in this node.
	 * @param invocable .
	 */
	public OpCodeExecutable(CodeType code, UserSpaceInvocable invocable) {
		this.invocable = invocable;
		this.code = code;
		this.fileName = code.get(0).getFilename();
	}

	/**
	 * @return code
	 */
	public CodeType getCode() {
		return code;
	}

	/** {@inheritDoc} */
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("CodeNode[");
		for (int i = 0; i < code.size(); i++) {
			sb.append("\n");
			sb.append(i);
			sb.append(":\t");
			sb.append(code.get(i).toString());
		}
		sb.append(" ]");
		return sb.toString();
	}

	/** 
	 * @param pv .
	 */
	public void printVisit(PrintVisitor pv) {

		PrintStream ps = pv.getPrintStream();

		ps.print("CodeNode[");
		for (int i = 0; i < code.size(); i++) {
			ps.print("\n");
			ps.print(i);
			ps.print(":\t");
			ps.print(code.get(i).toString());
		}
		ps.print(" ]\n");
	}

	/** {@inheritDoc} */
	public int getLineNumber(int depth) {
		// TODO line number table
		if (pc > 0) {
			// instruction currently being executed is generally addressed as pc
			// - 1
			// maybe -1 if code execution not started
			// no window for it to be 0
			return code.get(pc - 1).getLineNumber();
		} else {
			return code.get(0).getLineNumber();
		}

	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PHPValue executeFunction(RuntimeInterpreter runtime,
			PHPValue... args) {
		
		
		UserSpaceInvocable invocable = (UserSpaceInvocable)getInvocable();
		OpCodeExecutable runtimeNode = new OpCodeExecutable(code, invocable);
		
		// code fragments e.g. default font have Invocable 
		if (invocable == null) {
			ExecutableCode prevCode = runtime.getStackFrame().getActiveCode();
			runtime.getStackFrame().setActiveCode(runtimeNode);
			PHPValue returnVal = runtimeNode.run(runtime, ExecutionContext.READING);
			runtime.getStackFrame().setActiveCode(prevCode);
			return returnVal;
		}
		
		// Calling function {0} (node {1})
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { invocable.getFunctionName(), this};
			LOGGER.log(SAPILevel.DEBUG, "3052", inserts);
		}

		ExecutableCode previousCode = null;
		PHPValue returnVal = null;
		boolean entry_pushed = false;
		try {
			if (invocable.isMain()) {
				previousCode = runtime.getStackFrame().getActiveCode();
				runtime.getStackFrame().setActiveCode(runtimeNode);
			} else {
				// Prepare the new stack frame
				StackFrame sf = new StackFrameImpl(
						runtime, invocable, null, args, null);
				runtime.setNewStackFrame(sf);
				sf.setVariables(new VarMapHash());
				
				// Check argument type hints
				if (invocable.hasHints()) {
					invocable.checkHints(runtime, args);
				}

				// Setup arguments by transferring them from the stack to the local map
				invocable.extractArguments(runtime, args);
				
				// after extract args since extract can raise errors with backtrace
				sf.setActiveCode(runtimeNode);
			}

			// execute the function
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceCall(invocable, runtime, args); }

			runtime.pushProgramCacheEntry(invocable.getProgramCacheEntry());
			entry_pushed = true;
			returnVal = runtimeNode.run(runtime, ExecutionContext.READING);

			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceReturn(runtime, returnVal); }

			if (invocable.isMain()) {
				runtime.getStackFrame().setActiveCode(previousCode);
			}
			
			if (returnVal != null) {
				if (!invocable.isReturnByReference()) {
					// this is a return by value
					// clone ensures the return value is a temporary - it has no references
					returnVal = returnVal.cloneIfReferenced();
				} else {
					assert returnVal.isWritable() : "return by reference did not return a writable value.";
				}
			} else {
				switch (runtime.getStackFrame().getStackFrameType()) {
				case INCLUDE:
				case INCLUDE_ONCE:
				case REQUIRE:
				case REQUIRE_ONCE:
					/* allow includes to return a real null */
					break;
				default:
					returnVal = PHPValue.createNull();
				}
			}
		} finally {
			if (entry_pushed) {
				runtime.popProgramCacheEntry();
			}
			if (!invocable.isMain()) {
				// ensure StackFrame collapsed and argument references adjusted regardless
				// of normal return or exception
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						args[i].decReferences();
					}
				}
				runtime.getLocals().decReferencesAllValues();
				runtime.collapseStackFrame();
			}
		}
		return returnVal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PHPValue executeMethod(RuntimeInterpreter runtime,
			PHPValue receiver, PHPValue... args) {
		UserSpaceInvocable invocable = (UserSpaceInvocable)getInvocable();
		OpCodeExecutable runtimeNode = new OpCodeExecutable(code, invocable);

		assert invocable != null;
		assert !invocable.isMain();
		
		// Calling function {0} (node {1})
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { invocable.getFunctionName(), this};
			LOGGER.log(SAPILevel.DEBUG, "3052", inserts);
		}

		PHPValue returnVal = null;
		boolean entry_pushed = false;
		try {
			// Prepare the new stack frame
			StackFrame sf = new StackFrameImpl(
					runtime, invocable, receiver, args, null);
			runtime.setNewStackFrame(sf);
			sf.setVariables(new VarMapHash());

			// Check argument type hints
			if (invocable.hasHints()) {
				invocable.checkHints(runtime, args);
			}

			// Setup arguments by transferring them from the stack to the local map
			invocable.extractArguments(runtime, args);

			// Setup $this
			runtime.getLocals().assignValue(PHPClass.THIS_VARNAME, receiver);
			
			// after extract args since extract can raise errors with backtrace
			sf.setActiveCode(runtimeNode);
			
			// execute the function
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceCall(invocable, runtime, args); }

			runtime.pushProgramCacheEntry(invocable.getProgramCacheEntry());
			entry_pushed = true;
			returnVal = runtimeNode.run(runtime, ExecutionContext.READING);
			
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceReturn(runtime, returnVal); }

			if (returnVal != null) {
				if (!invocable.isReturnByReference()) {
					// this is a return by value
					// clone ensures the return value is a temporary - it has no references
					returnVal = returnVal.cloneIfReferenced();
				} else {
					assert returnVal.isWritable() : "return by reference did not return a writable value.";
				}
			}
		} finally {
			if (entry_pushed) {
				runtime.popProgramCacheEntry();
			}
			// ensure StackFrame collapsed and argument references adjusted regardless
			// of normal return or exception
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					args[i].decReferences();
				}
			}
			runtime.getLocals().decReferencesAllValues();
			runtime.collapseStackFrame();
		}
		if (returnVal == null) {
			returnVal = PHPValue.createNull();
		}
		return returnVal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeVoidFunction(RuntimeInterpreter runtime,
			PHPValue... args) {
		UserSpaceInvocable invocable = (UserSpaceInvocable)getInvocable();
		OpCodeExecutable runtimeNode = new OpCodeExecutable(code, invocable);

		assert invocable != null;
		
		// Calling function {0} (node {1})
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { invocable.getFunctionName(), this};
			LOGGER.log(SAPILevel.DEBUG, "3052", inserts);
		}

		ExecutableCode previousCode = null;
		boolean entry_pushed = false;
		try {
			if (invocable.isMain()) {
				previousCode = runtime.getStackFrame().getActiveCode();
				runtime.getStackFrame().setActiveCode(runtimeNode);
			} else {
				// Prepare the new stack frame
				StackFrame sf = new StackFrameImpl(
						runtime, invocable, null, args, null);
				runtime.setNewStackFrame(sf);
				sf.setVariables(new VarMapHash());

				// Check argument type hints
				if (invocable.hasHints()) {
					invocable.checkHints(runtime, args);
				}

				// Setup arguments by transferring them from the stack to the local map
				invocable.extractArguments(runtime, args);
				
				// after extract args since extract can raise errors with backtrace
				sf.setActiveCode(runtimeNode);
			}
			
			// execute the function
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceCall(invocable, runtime, args); }

			runtime.pushProgramCacheEntry(invocable.getProgramCacheEntry());
			entry_pushed = true;
			runtimeNode.run(runtime, ExecutionContext.READING);
            
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceReturn(runtime, null); }

			if (invocable.isMain()) {
				runtime.getStackFrame().setActiveCode(previousCode);
			}
			
		} finally {
			if (entry_pushed) {
				runtime.popProgramCacheEntry();
			}
			if (!invocable.isMain()) {
				// ensure StackFrame collapsed and argument references adjusted regardless
				// of normal return or exception
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						args[i].decReferences();
					}
				}
				runtime.getLocals().decReferencesAllValues();
				runtime.collapseStackFrame();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeVoidMethod(RuntimeInterpreter runtime,
			PHPValue receiver, PHPValue... args) {
		UserSpaceInvocable invocable = (UserSpaceInvocable)getInvocable();
		OpCodeExecutable runtimeNode = new OpCodeExecutable(code, invocable);

		assert invocable != null;
		assert !invocable.isMain();
		
		// Calling function {0} (node {1})
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { invocable.getFunctionName(), this};
			LOGGER.log(SAPILevel.DEBUG, "3052", inserts);
		}

		boolean entry_pushed = false;
		try {
			// Prepare the new stack frame
			StackFrame sf = new StackFrameImpl(
					runtime, invocable, receiver, args, null);
			runtime.setNewStackFrame(sf);
			sf.setVariables(new VarMapHash());

			// Check argument type hints
			if (invocable.hasHints()) {
				invocable.checkHints(runtime, args);
			}

			// Setup arguments by transferring them from the stack to the local map
			invocable.extractArguments(runtime, args);

			// Setup $this
			runtime.getLocals().assignValue(PHPClass.THIS_VARNAME, receiver);
			
			// after extract args since extract can raise errors with backtrace
			sf.setActiveCode(runtimeNode);
			
			// execute the function
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceCall(invocable, runtime, args); }

			runtime.pushProgramCacheEntry(invocable.getProgramCacheEntry());
			entry_pushed = true;
			runtimeNode.run(runtime, ExecutionContext.READING);
            
			if (invocable.isFunctionTraceOn()) { runtime.getFunctionTrace().traceReturn(runtime, null); }

		} finally {
			if (entry_pushed) {
				runtime.popProgramCacheEntry();
			}
			// ensure StackFrame collapsed and argument references adjusted regardless
			// of normal return or exception
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					args[i].decReferences();
				}
			}
			runtime.getLocals().decReferencesAllValues();
			runtime.collapseStackFrame();
		}
	}

	/**
	 * Implementation of execute. Called on call specific copy of this class
	 * such that program counter is not shared.
	 * 
	 * @param runtime
	 *            runtime context
	 * @param ectx
	 *            dynamically determined execution context
	 * @return 	return value or null
	 * 				
	 */
	private PHPValue run(RuntimeInterpreter runtime, ExecutionContext ectx) {
		boolean trace = runtime.getOptions().getTraceCode();
		final P8DebugProvider debugProvider = runtime.getDebugProvider();
		final ConfigurationService configurationService = runtime.getConfigurationService();
		final PHPStack stack = runtime.getStack();
		final int entryStackSize = stack.size();
		final Ticker ticker = runtime.getTicker();
		final Classes classes = runtime.getClasses();
		int timeoutCounter = 0;

		final Stack<CatchStackEntry> catchStack = new Stack<CatchStackEntry>(); // stack
																				// of
																				// addresses
																				// of
																				// catch
																				// blocks
		final Stack<ForEachIterator> feStack = new Stack<ForEachIterator>(); // stack
																				// of
																				// foreach
																				// helpers
		final Stack<ListIterator> liStack = new Stack<ListIterator>(); // stack
																		// of
																		// list
																		// helpers
		final InvocableStack invocableStack = new InvocableStack(); // stack of
																	// invocable/methods
																	// in
																	// preparaion

		if (trace) {
			System.out.println("Enter " + this + "\n");
		}

		pc = 0; // address first instruction
		int lastCheckForSuspendLine = -1; // last line where we checked for suspend

		for (;;) { // loop over try catch
			try {
				for (;;) { // execution loop

					Op op = code.get(pc);
					if (trace) {
						System.out.println(stack.toString());
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:S");
						System.out.println(sdf.format(new Date()) + " @" + pc + ": \t" + op);
					}
					pc++; // now address next instruction to be executed

					if (debugProvider.debugMode) { // this does not currently change at runtime
						int currentLine = op.getLineNumber();
						// If debug is on, check for suspend if this op can be stepped into, or
						// if we're on a new line and this op can be stepped on to. 
						if (op.canBeSteppedInto() 
								|| (currentLine != lastCheckForSuspendLine && op.checkLineStep())) { 
							debugProvider.checkForSuspend(op.getLineNumber()); // check to see if  we should stop
							lastCheckForSuspendLine = currentLine;					
						}							
					}

					// copied from PHPStack.setActiveNode() TODO Delete from
					// PHPStack when AST execute unsupported
					// can we do better than counting and polling?
					if (timeoutCounter++ >= TIMEOUT_GRANULARITY) {
						timeoutCounter = 0;
						if (configurationService.isTimedOut()) {
							configurationService.notifyTimeoutHandled();
							// throw a timeout exception and unwind
							Object[] inserts = new Object[1];
							inserts[0] = Integer.valueOf(configurationService.getTimeLimit());
							if ((Integer) inserts[0] == 1) {
								runtime.raiseExecError(PHPErrorHandler.E_USER_ERROR, null, "Configuration.TimeoutOne",
										inserts);
							} else {
								runtime.raiseExecError(PHPErrorHandler.E_USER_ERROR, null, "Configuration.Timeout",
										inserts);
							}
						}
					}

					switch (op.operation) {
					case Op.REVERSE:
						Op.opREVERSE(op.integer, stack);
						break;

					case Op.LOCAL_R:
						Op.opLOCAL_R(runtime, op.string, stack);
						break;
						
					case Op.LOCAL_FE:
						Op.opLOCAL_FE(runtime, op.string, stack);
						break;
						
					case Op.LOCAL_U:
						Op.opLOCAL_U(runtime, op.string, stack);
						break;

					case Op.LOCAL_W:
						Op.opLOCAL_W(runtime, op.string, stack);
						break;

					case Op.LOCAL_RW:
						Op.opLOCAL_RW(runtime, op.string, stack);
						break;

					case Op.LOCAL_I:
						Op.opLOCAL_I(runtime, op.string, stack);
						break;

					case Op.INDIRECT:
						Op.opINDIRECT(runtime, stack);
						break;

					case Op.INDIRECT_W:
						Op.opINDIRECT_W(runtime, stack);
						break;

					case Op.ISSET_LOCAL:
						Op.opISSET_LOCAL(runtime, op.string, op.bool, stack);
						break;

					case Op.UNSET_LOCAL:
						Op.opUNSET_LOCAL(runtime, op.string, stack);
						break;

					case Op.GLOBAL_R:
						Op.opGLOBAL_R(runtime, op.string, stack);
						break;

					case Op.GLOBAL_FE:
						Op.opGLOBAL_FE(runtime, op.string, stack);
						break;						
						
					case Op.GLOBAL_U:
						Op.opGLOBAL_U(runtime, op.string, stack);
						break;

					case Op.GLOBAL_W:
						Op.opGLOBAL_W(runtime, op.string, stack);
						break;

					case Op.GLOBAL_RW:
						Op.opGLOBAL_RW(runtime, op.string, stack);
						break;

					case Op.GLOBAL_I:
						Op.opGLOBAL_I(runtime, op.string, stack);
						break;

					case Op.ISSET_GLOBAL:
						Op.opISSET_GLOBAL(runtime, op.string, op.bool, stack);
						break;

					case Op.UNSET_GLOBAL:
						Op.opUNSET_GLOBAL(runtime, op.string);
						break;

					case Op.PUSH:
						Op.opPUSH(op.phpValue, stack);
						break;
					
					case Op.PUSHTEMP:
						Op.opPUSHTEMP(op.phpValue, stack);
						break;

					case Op.RETURN_BY_REF_CHECK:
						Op.opRETURN_BY_REF_CHECK(runtime, stack);
						break;

					case Op.RETURN:
						// this test works and is not the same as "op.bool" TODO: make it the same
						if (stack.size() > entryStackSize) {
							assert stack.size() == entryStackSize + 1 : "Stack should 1 at RETURN " + (stack.size() - entryStackSize);
							// if there is a value on the stack return it
							return stack.pop();
						} else {
							assert stack.size() == entryStackSize : "Stack should be 0 at RETURN " + (stack.size() - entryStackSize);
							// otherwise return null
							return null;
						}

					case Op.BRANCH:
						pc = Op.opBRANCH(op.integer, pc);
						break;
						
					case Op.CATCH_ENTER:
						pc = Op.opCATCH_ENTER(runtime, op.integer, stack, op.operand, pc);
						break;

					case Op.BRTRUE:
						pc = Op.opBRTRUE(runtime, op.integer, stack, pc);
						break;

					case Op.BRFALSE:
						pc = Op.opBRFALSE(runtime, op.integer, stack, pc);
						break;

					case Op.ASSIGN_REF_LOCAL:
						Op.opASSIGN_REF_LOCAL(runtime, op.string, op.bool, stack);
						break;

					case Op.ASSIGN_VAL_LOCAL:
						Op.opASSIGN_VAL_LOCAL(runtime, op.string, op.bool, stack);
						break;

					case Op.ASSIGN_REF_GLOBAL:
						Op.opASSIGN_REF_GLOBAL(runtime, op.string, stack);
						break;

					case Op.ASSIGN_VAL_GLOBAL:
						Op.opASSIGN_VAL_GLOBAL(runtime, op.string, op.bool, stack);
						break;

					case Op.SWAP:
						Op.opSWAP(stack);
						break;

					/* OPERATORS */
					case Op.CMPLT:
						Op.opCMPLT(runtime, stack);
						break;
					case Op.CMPLE:
						Op.opCMPLE(runtime, stack);
						break;
					case Op.CMPGT:
						Op.opCMPGT(runtime, stack);
						break;
					case Op.CMPGE:
						Op.opCMPGE(runtime, stack);
						break;
					case Op.CMPID:
						Op.opCMPID(runtime, stack);
						break;
					case Op.CMPEQ:
						Op.opCMPEQ(runtime, stack);
						break;
					case Op.CMPNE:
						Op.opCMPNE(runtime, stack);
						break;
					case Op.CMPNI:
						Op.opCMPNI(runtime, stack);
						break;
					case Op.ADD:
						Op.opADD(runtime, op.bool, stack);
						break;
					case Op.SUB:
						Op.opSUB(runtime, op.bool, stack);
						break;
					case Op.REM:
						Op.opREM(runtime, op.bool, stack);
						break;
					case Op.MUL:
						Op.opMUL(runtime, op.bool, stack);
						break;
					case Op.DIV:
						Op.opDIV(runtime, op.bool, stack);
						break;
					case Op.CASTARRY:
						Op.opCASTARRY(runtime, stack);
						break;
					case Op.CASTBOOL:
						Op.opCASTBOOL(runtime, stack);
						break;
					case Op.CASTDOUB:
						Op.opCASTDOUB(runtime, stack);
						break;
					case Op.CASTINT:
						Op.opCASTINT(runtime, stack);
						break;
					case Op.CASTOBJ:
						Op.opCASTOBJ(runtime, stack);
						break;
					case Op.CASTSTR:
						Op.opCASTSTR(runtime, stack);
						break;
					case Op.BITAND:
						Op.opBITAND(runtime, op.bool, stack);
						break;
					case Op.BITNOT:
						Op.opBITNOT(runtime, stack);
						break;
					case Op.BITOR:
						Op.opBITOR(runtime, op.bool, stack);
						break;
					case Op.BITSLEFT:
						Op.opBITSLEFT(runtime, op.bool, stack);
						break;
					case Op.BITSRIGHT:
						Op.opBITSRIGHT(runtime, op.bool, stack);
						break;
					case Op.BITXOR:
						Op.opBITXOR(runtime, op.bool, stack);
						break;
					case Op.CONCAT:
						Op.opCONCAT(runtime, op.bool, stack);
						break;
					case Op.PREDEC:
						Op.opPREDEC(runtime, stack, op.bool);
						break;
					case Op.PREINC:
						Op.opPREINC(runtime, stack, op.bool);
						break;
					case Op.POSTDEC:
						Op.opPOSTDEC(runtime, stack, op.bool);
						break;
					case Op.POSTINC:
						Op.opPOSTINC(runtime, stack, op.bool);
						break;
					case Op.LOGAND:
						Op.opLOGAND(runtime, stack);
						break;
					case Op.LOGOR:
						Op.opLOGOR(runtime, stack);
						break;
					case Op.LOGXOR:
						Op.opLOGXOR(runtime, stack);
						break;
					case Op.LOGNOT:
						Op.opLOGNOT(runtime, stack);
						break;
					case Op.NEG:
						Op.opNEG(runtime, stack);
						break;
					case Op.PLUS:
						Op.opPLUS(runtime, stack);
						break;

					case Op.CALL:
						Op.opCALL(runtime, op.integer, op.operand.invocable, op.bool);
						break;

					case Op.ECHO:
						Op.opECHO(runtime, stack);
						break;

					case Op.DROP:
						Op.opDROP(stack);
						break;

					case Op.DUP:
						Op.opDUP(stack);
						break;

					case Op.BREAK:
						pc = Op.opBREAK(runtime, op.integer, stack, pc);
						break;

					case Op.FE_INIT:
						pc = Op.opFE_INIT(runtime, op.integer, op.bool, op.operand, stack, pc, feStack);
						break;

					case Op.FE_NEXT:
						pc = Op.opFE_NEXT(runtime, op.integer, stack, pc, feStack);
						break;

					case Op.FE_FREE:
						Op.opFE_FREE(feStack);
						break;

					case Op.ERROR:
						Op.opERROR(runtime, op.integer, op.string);
						break;

					case Op.SILENCE:
						Op.opSILENCE(runtime, op.bool);
						break;

					case Op.ADDFUNC:
						Op.opADDFUNC(runtime, op.integer);
						break;

					case Op.ADDCLASS:
						Op.opADDCLASS(runtime, op.integer);
						break;

					case Op.CHKCLASS:
						Op.opCHKCLASS(runtime, op.operand.name);
						break;

					case Op.INSTANCEOF:
						Op.opINSTANCEOF(runtime, op.operand, stack);
						break;

					case Op.THROW:
						pc = Op.opTHROW(runtime, stack, classes, catchStack, pc);
						break;

					case Op.TRY_ENTER:
						Op.opTRY_ENTER(op.integer, stack, catchStack, pc);
						break;

					case Op.TRY_EXIT:
						Op.opTRY_EXIT(catchStack);
						break;

					case Op.PREPARE_ARG_BY_VALUE:
						Op.opPREPARE_ARG_BY_VALUE(stack);
						break;
						
					case Op.PREPARE_ARG_BY_REFERENCE:
						Op.opPREPARE_ARG_BY_REFERENCE(stack);
						break;
						
					case Op.PREPARE_ARG_PREFER_REFERENCE:
						Op.opPREPARE_ARG_PREFER_REFERENCE(stack);
						break;

					case Op.PREPARE_ARG_DYNAMIC_TARGET:
						Op.opPREPARE_ARG_DYNAMIC_TARGET(stack, op.integer, invocableStack);
						break;

					case Op.FIND_FUNCTION:
						Op.opFIND_FUNCTION(runtime, op.operand, invocableStack);
						break;

					case Op.FIND_METHOD:
						Op.opFIND_METHOD(runtime, stack, invocableStack, op.operand.name);
						break;

					case Op.INVOKE_METHOD:
						Op.opINVOKE_METHOD(runtime, op.integer, stack, invocableStack, op.bool);
						break;

					case Op.INVOKE_FUNCTION:
						Op.opINVOKE_FUNCTION(runtime, op.integer, stack, invocableStack, op.bool);
						break;

					case Op.ARG_CONTEXT:
						pc = Op.opARG_CONTEXT(op.integer, op.operand.branchTable, invocableStack, pc);
						break;
						
					case Op.PREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE:
						Op.opPREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE(runtime, op.integer, 
								 invocableStack);
	                    break;								

					case Op.NEWARRAY:
						Op.opNEWARRAY(stack);
						break;

					case Op.ASSIGN_REF_ARRAY:
						Op.opASSIGN_REF_ARRAY(runtime, op.bool, stack);
						break;

					case Op.ASSIGN_VAL_ARRAY:
						Op.opASSIGN_VAL_ARRAY(runtime, op.bool, stack);
						break;

					case Op.ASSIGN_REF_INDEX:
						Op.opASSIGN_REF_INDEX(runtime, op.bool, op.phpValue, stack);
						break;

					case Op.ASSIGN_VAL_INDEX:
						Op.opASSIGN_VAL_INDEX(runtime, op.bool, op.phpValue, stack);
						break;

					case Op.ARRAY_APPEND:
						Op.opARRAY_APPEND(runtime, op.bool, stack);
						break;

					case Op.ARRAY_INSERT:
						Op.opARRAY_INSERT(runtime, op.bool, stack);
						break;

					case Op.INDEX_R:
						Op.opINDEX_R(runtime, op.phpValue);
						break;
						
					case Op.INDEX_FE:
						Op.opINDEX_FE(runtime, op.phpValue);
						break;
						
					case Op.INDEX_W:
						Op.opINDEX_W(runtime, op.phpValue, op.bool);
						break;

					case Op.INDEX_RW:
						Op.opINDEX_RW(runtime, op.phpValue, op.bool);
						break;

					case Op.INDEX_U:
						Op.opINDEX_U(runtime, op.phpValue);
						break;

					case Op.INDEX_I:
						Op.opINDEX_I(runtime, op.phpValue);
						break;

					case Op.ISSET_INDEX:
						Op.opISSET_INDEX(runtime, op.bool, op.phpValue, stack);
						break;

					case Op.UNSET_INDEX:
						Op.opUNSET_INDEX(runtime, op.phpValue, stack);
						break;

					case Op.STATIC_PROPERTY:
						Op.opSTATIC_PROPERTY(runtime, op.bool, op.operand.name, stack);
						break;

					case Op.STATIC_PROPERTY_RW_INCDEC:
						Op.opSTATIC_PROPERTY_RW_INCDEC(runtime, op.bool, op.operand.name, stack);
						break;
						
					case Op.ARRAY_INIT_CHECK:
						Op.opARRAY_INIT_CHECK(stack);
						break;

					case Op.ASSIGN_REF_PROPERTY:
						Op.opASSIGN_REF_PROPERTY(runtime, op.bool, op.string, stack);
						break;

					case Op.ASSIGN_VAL_PROPERTY:
						Op.opASSIGN_VAL_PROPERTY(runtime, op.bool, op.string, stack);
						break;

					case Op.ASSIGN_VAL_PROPERTY2:
						Op.opASSIGN_VAL_PROPERTY2(runtime, op.bool, stack);
						break;

					case Op.ISSET_PROPERTY:
						Op.opISSET_PROPERTY(runtime, op.bool, op.string, stack);
						break;

					case Op.UNSET_PROPERTY:
						Op.opUNSET_PROPERTY(runtime, op.string, stack);
						break;

					case Op.PROPERTY_R:
						Op.opPROPERTY_R(runtime, op.string, stack);
						break;
						
					case Op.PROPERTY_FE:
						Op.opPROPERTY_FE(runtime, op.string, stack);
						break;						

					case Op.PROPERTY_I:
						Op.opPROPERTY_I(runtime, op.string, stack);
						break;

					case Op.PROPERTY_U:
						Op.opPROPERTY_U(runtime, op.string, stack);
						break;

					case Op.PROPERTY_RW:
						Op.opPROPERTY_RW(runtime, op.string, op.bool, stack);
						break;

					case Op.PROPERTY_RW_INCDEC:
						Op.opPROPERTY_RW_INCDEC(runtime, op.string, op.bool, op.integer, stack);
						break;

					case Op.PROPERTY_RW_OPASSIGN:
						Op.opPROPERTY_RW_OPASSIGN(runtime, op.bool, op.string, stack);
						break;

					case Op.LOAD_STATIC:
						Op.opLOAD_STATIC(runtime, op.string);
						break;

					case Op.ASSIGN_REF_STATIC_PROPERTY:
						Op.opASSIGN_REF_STATIC_PROPERTY(runtime, op.operand.name, op.bool, stack);
						break;

					case Op.ASSIGN_VAL_STATIC_PROPERTY:
						Op.opASSIGN_VAL_STATIC_PROPERTY(runtime, op.operand.name, op.bool, stack);
						break;

					case Op.OBJECT_INIT_CHECK:
						Op.opOBJECT_INIT_CHECK(runtime, stack, classes);
						break;

					case Op.UNSET_STATIC_PROPERTY_ERROR:
						Op.opUNSET_STATIC_PROPERTY_ERROR(runtime, op.operand.name, stack);
						break;

					case Op.ISSET_STATIC_PROPERTY:
						Op.opISSET_STATIC_PROPERTY(runtime, op.operand.name, op.bool, stack);
						break;

					case Op.CLASS_CONSTANT:
						Op.opCLASS_CONSTANT(runtime, op.operand.name, stack);
						break;

					case Op.MAKE_GLOBAL:
						Op.opMAKE_GLOBAL(runtime, op.string, stack);
						break;

					case Op.INCLUDE:
						Op.opINCLUDE(runtime, op.integer, stack);
						break;

					case Op.EVAL:
						Op.opEVAL(runtime, op.string, stack);
						break;

					case Op.INDEX_ENCAPS:
						Op.opINDEX_ENCAPS(runtime, op.phpValue, op.bool, stack);
						break;

					case Op.CLASS_CLONE:
						Op.opCLASS_CLONE(runtime, stack);
						break;

					case Op.CLASS_NEW:
						pc = Op.opCLASS_NEW(runtime, op.integer, stack, classes, invocableStack, pc);
						break;
						
					case Op.PREP_NEW_BY_REF:
						Op.opPREP_NEW_BY_REF(runtime, stack);
						break;
						
					case Op.FIND_STATIC_METHOD:
						Op.opFIND_STATIC_METHOD(runtime, op.operand.name, stack, classes, invocableStack);
						break;

					case Op.INVOKE_STATIC_METHOD:
						Op.opINVOKE_STATIC_METHOD(runtime, op.integer, invocableStack, op.bool);
						break;

					case Op.CONSTANT:
						Op.opCONSTANT(runtime, op.string, stack);
						break;

					case Op.EXIT:
						Op.opEXIT(runtime, op.bool, stack);
						break;

					case Op.LIST_INIT:
						Op.opLIST_INIT(runtime, op.integer, op.bool, stack, liStack);
						break;

					case Op.LIST_NEXT:
						Op.opLIST_NEXT(runtime, op.bool, stack, liStack);
						break;

					case Op.LIST_FREE:
						Op.opLIST_FREE(runtime, op.bool, stack, liStack);
						break;

					case Op.TICKER:
						Op.opTICKER(runtime, op.bool, stack);
						break;

					case Op.MULTI_CONCAT:
						Op.opMULTI_CONCAT(runtime, op.integer, stack);
						break;
					
					case Op.ASSIGN_VAL_PROP_INDEXED:
						Op.opASSIGN_VAL_PROP_INDEXED(runtime, op.bool, stack);
						break;
						
					case Op.INDEX_RW_OPASSIGN:
						Op.opINDEX_RW_OPASSIGN(runtime, op.phpValue, op.bool);
						break;
						
					case Op.PREPARE_CALL:
						break; // no op
						
					default:
						assert false : "Op " + op + " unimplemented";
						break;

					} // switch

					// check tick property
					if (op.tick) {
						ticker.tick();
					}
				} // inner for
			} catch (ExceptionWrapper ew) {
				if (catchStack.size() > 0) {
					// goto catch block with exception on stack
					stack.push(ew.getPHPValue());
					CatchStackEntry catchEntry = catchStack.pop();
					pc = catchEntry.getCatchPC();

					if (stack.size() > catchEntry.getStackSize() + 1) {
						PHPValue exception = stack.pop();
						stack.restoreStack(catchEntry.getStackSize());
						stack.push(exception);
					}
				} else {
					throw ew;
				}
			} // catch
		} // outer for
	} // method

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileName() {
		return fileName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserSpaceInvocable getInvocable() {
		return invocable;
	}
}
