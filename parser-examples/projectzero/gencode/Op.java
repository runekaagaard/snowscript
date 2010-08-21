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

import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;

import com.ibm.p8.engine.core.ArgumentSemantics;
import com.ibm.p8.engine.core.Classes;
import com.ibm.p8.engine.core.ErrorType;
import com.ibm.p8.engine.core.ExceptionWrapper;
import com.ibm.p8.engine.core.ExecutableCode;
import com.ibm.p8.engine.core.FatalError;
import com.ibm.p8.engine.core.IncludeEval;
import com.ibm.p8.engine.core.Indexable;
import com.ibm.p8.engine.core.Invocable;
import com.ibm.p8.engine.core.Operators;
import com.ibm.p8.engine.core.PHPErrorHandler;
import com.ibm.p8.engine.core.PHPStack;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.ProgramCacheEntry;
import com.ibm.p8.engine.core.RuntimeInterpreter;
import com.ibm.p8.engine.core.StackFrame;
import com.ibm.p8.engine.core.StackFrameIncludeImpl;
import com.ibm.p8.engine.core.StaticVariableScope;
import com.ibm.p8.engine.core.TerminateScript;
import com.ibm.p8.engine.core.VarMapConstants;
import com.ibm.p8.engine.core.PHPValue.Types;
import com.ibm.p8.engine.core.StackFrame.StackFrameType;
import com.ibm.p8.engine.core.TerminateScript.Reasons;
import com.ibm.p8.engine.core.array.ArrayFacade;
import com.ibm.p8.engine.core.object.ClassFacade;
import com.ibm.p8.engine.core.object.MagicMethodInfo;
import com.ibm.p8.engine.core.object.ObjectFacade;
import com.ibm.p8.engine.core.object.PHPClass;
import com.ibm.p8.engine.core.object.PHPMethod;
import com.ibm.p8.engine.core.object.PHPMethodAndCallType;
import com.ibm.p8.engine.core.object.PHPPropertyDescriptor;
import com.ibm.p8.engine.core.object.ObjectHandlers.CheckType;
import com.ibm.p8.engine.core.string.StringFacade;
import com.ibm.p8.engine.core.types.PHPArray;
import com.ibm.p8.engine.core.util.NameString;
import com.ibm.p8.engine.library.BuiltinLibrary;
import com.ibm.p8.engine.library.StandardClasses.PHPException;
import com.ibm.p8.engine.library.StandardClasses.PHPStdClass;
import com.ibm.p8.engine.parser.gencode.InvocableStackEntry.ImplicitCallType;
import com.ibm.p8.engine.parser.model.Ast;
import com.ibm.p8.utilities.log.P8LogManager;
import com.ibm.phpj.logging.SAPIComponent;
import com.ibm.phpj.logging.SAPILevel;
import com.ibm.phpj.reflection.XAPIPassSemantics;

/**
 * Op contains a P8 Op-code and its associated operands.
 */

public class Op {
	
	// properties are final where possible
	protected final byte operation;

	private final int linenumber;
	private final String filename;
	
	protected boolean tick;
	
	// operands - there would be a union here if this were C
	protected final boolean bool;
	protected int integer; // not final since branches can be resolved after the opcode is created
	
	protected final String string;
	protected final PHPValue phpValue;
	protected final Operand operand;

	/**
	 * Logger for the <code>Evaluator</code>.
	 */
	private static final Logger LOGGER = P8LogManager._instance
	.getLogger(SAPIComponent.Runtime);

	/** mnemonic names of each opcode. */
	private static final String[] OPNAME = {
			"REVERSE", // 0
			"ISSET_LOCAL", // 1
			"UNSET_LOCAL", // 2
			"ISSET_LOBAL", // 3
			"UNSET_GLOBAL", // 4
			"PUSH", // 5
			"RETURN", // 6
			"BRANCH", // 7
			"BRTRUE", // 8
			"BRFALSE", // 9
			"ASSIGN_REF_LOCAL", // 10
			"ASSIGN_VAL_LOCAL", // 11
			"CMPLT", // 12
			"CMPLE", // 13
			"CMPID", // 14
			"CMPEQ", // 15
			"CMPNE", // 16
			"CMPNI", // 17
			"SWAP", // 18
			"ADD", // 19
			"SUB", // 20
			"REM", // 21
			"MUL", // 22
			"DIV", // 23
			"CASTARRY", // 24
			"CASTBOOL", // 25
			"CASTDOUB", // 26
			"CASTINT", // 27
			"CASTOBJ", // 28
			"CASTSTR", // 29
			"BITAND", // 30
			"BITNOT", // 31
			"BITOR", // 32
			"BITSLEFT", // 33
			"BITSRIGHT", // 34
			"BITXOR", // 35
			"CONCAT", // 36
			"PREDEC", // 37
			"PREINC", // 38
			"LOGAND", // 39
			"LOGOR", // 40
			"LOGXOR", // 41
			"NEG", // 42
			"POSTDEC", // 43
			"POSTINC", // 44
			"CALL", // 45
			"ECHO", // 46
			"DROP", // 47
			"DUP", // 48
			"BREAK", // 49
			"TRY_ENTER", // 50
			"UNSET_STATIC_PROPERTY_ERROR", // 51
			"ISSET_STATIC_PROPERTY", // 52
			"LOGNOT", // 53
			"LOAD_STATIC", // 54
			"ASSIGN_REF_STATIC_PROPERTY", // 55
			"ASSIGN_VAL_STATIC_PROPERTY", // 56
			"ASSIGN_REF_GLOBAL", // 57
			"ASSIGN_VAL_GLOBAL", // 58
			"PLUS", // 59
			"FE_INIT", // 60
			"FE_NEXT", // 61
			"FE_FREE", // 62
			"ERROR", // 63
			"SILENCE", // 64
			"ADDFUNC", // 65
			"ADDCLASS", // 66
			"CHKCLASS", // 67
			"INSTANCEOF", // 68
			"CLASS_NEW", // 69
			"THROW", // 70
			"TRY_EXIT", // 71
			"PREPARE_ARG_BY_VALUE", // 72
			"FIND_FUNCTION", // 73
			"INVOKE_FUNCTION", // 74
			"FIND_METHOD", // 75
			"INVOKE_METHOD", // 76
			"ARG_CONTEXT", // 77
			"PREPARE_ARG_DYNAMIC_TARGET", // 78
			"INDIRECT", // 79
			"INDIRECT_W", // 80
			"NEWARRAY", // 81
			"ASSIGN_REF_INDEX", // 82
			"ASSIGN_VAL_INDEX", // 83
			"ASSIGN_REF_ARRAY", // 84
			"ASSIGN_VAL_ARRAY", // 85
			"INDEX_R", // 86
			"INDEX_W", // 87
			"INDEX_RW", // 88
			"INDEX_U", // 89
			"INDEX_I", // 90
			"UNSET_INDEX", // 91
			"ISSET_INDEX", // 92
			"STATIC_PROPERTY", // 93
			"ARRAY_INIT_CHECK", // 94
			"ASSIGN_REF_PROPERTY", // 95
			"ASSIGN_VAL_PROPERTY", // 96
			"ISSET_PROPERTY", // 97
			"UNSET_PROPERTY", // 98
			"PROPERTY_R", // 99
			"LOCAL_R", // 100
			"LOCAL_W", // 101
			"LOCAL_RW", // 102
			"LOCAL_I", // 103
			"GLOBAL_R", // 104
			"GLOBAL_W", // 105
			"GLOBAL_RW", // 106
			"GLOBAL_I", // 107
			"PROPERTY_U", // 108
			"OBJECT_INIT_CHECK", // 109
			"CLASS_CONSTANT", // 110
			"FIND_STATIC_METHOD", // 111
			"EVAL", // 112
			"INCLUDE", // 113
			"MAKE_GLOBAL", // 114
			"INDEX_ENCAPS", // 115
			"CLASS_CLONE", // 116
			"INVOKE_STATIC_METHOD", // 117
			"LIST_INIT", // 118
			"LIST_NEXT", // 119
			"LIST_FREE", // 120
			"CONSTANT", // 121
			"EXIT", // 122
			"TICKER", // 123
			"PROPERTY_I", // 124
			"PROPERTY_RW", // 125
			"LOCAL_U", // 126
			"GLOBAL_U", // 127
			"ARRAY_APPEND", // 128
			"ARRAY_INSERT", // 129
			"PROPERTY_RW_INCDEC", // 130
			"PROPERTY_RW_OPASSIGN", // 131
			"ASSIGN_VAL_PROPERTY2", // 132
			"RETURN_BY_REF_CHECK", // 133
			"CATCH_ENTER", //134
			"GLOBAL_FE", //135
			"LOCAL_FE", //136
			"INDEX_FE", //137
			"PROPERTY_FE", //138
			"MULTI_CONCAT", // 139
			"PREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE", // 140
			"ASSIGN_VAL_PROP_INDEXED", //141
			"INDEX_RW_OPASSIGN", //142
			"PREPARE_CALL", // 143
			"PREPARE_ARG_BY_REFERENCE", // 144
			"PREPARE_ARG_PREFER_REFERENCE", // 145
			"PREP_NEW_BY_REF", // 146
			"PUSHTEMP", // 147
			"CMPGE", // 148
			"CMPGT", // 149
			"STATIC_PROPERTY_RW_INCDEC", // 150
	};

	public static final byte REVERSE = 0;
	public static final byte ISSET_LOCAL = 1;
	public static final byte UNSET_LOCAL = 2;
	public static final byte ISSET_GLOBAL = 3;
	public static final byte UNSET_GLOBAL = 4;
	public static final byte PUSH = 5; // push constant PHPValue
	public static final byte RETURN = 6; // from function, return statement
	public static final byte BRANCH = 7;
	public static final byte BRTRUE = 8;
	public static final byte BRFALSE = 9;
	public static final byte ASSIGN_REF_LOCAL = 10;
	public static final byte ASSIGN_VAL_LOCAL = 11;
	public static final byte CMPLT = 12;
	public static final byte CMPLE = 13;
	public static final byte CMPID = 14;
	public static final byte CMPEQ = 15;
	public static final byte CMPNE = 16;
	public static final byte CMPNI = 17;
	public static final byte SWAP = 18;

	/**
	 * Arithmetic Addition.
	 * Binary operator operands [boolean inplace(false)]
	 * inplace - if true then first PHPValue argument is updated in place with
	 * the result (implementing operator assignments e.g. +=) otherwise a new
	 * PHPValue is created Stack pushes: -1 (pops 2, pushes 1)
	 */
	public static final byte ADD = 19;

	/**
	 * Arithmetic Subtraction.
	 * 
	 * @see Op#ADD
	 */
	public static final byte SUB = 20;

	/**
	 * Arithmetic Remainder (modulus) (%).
	 * 
	 * @see Op#ADD
	 */
	public static final byte REM = 21;

	/**
	 * Arithmetic Multiply.
	 * 
	 * @see Op#ADD
	 */
	public static final byte MUL = 22;

	/**
	 * Arithmetic Divide.
	 * 
	 * @see Op#ADD
	 */
	public static final byte DIV = 23;

	public static final byte CASTARRY = 24;
	public static final byte CASTBOOL = 25;
	public static final byte CASTDOUB = 26;
	public static final byte CASTINT = 27;
	public static final byte CASTOBJ = 28;
	public static final byte CASTSTR = 29;
	public static final byte BITAND = 30;
	public static final byte BITNOT = 31;
	public static final byte BITOR = 32;
	public static final byte BITSLEFT = 33;
	public static final byte BITSRIGHT = 34;
	public static final byte BITXOR = 35;
	public static final byte CONCAT = 36;
	public static final byte PREDEC = 37;
	public static final byte PREINC = 38;
	public static final byte LOGAND = 39;
	public static final byte LOGOR = 40;
	public static final byte LOGXOR = 41;
	public static final byte NEG = 42;
	public static final byte POSTDEC = 43;
	public static final byte POSTINC = 44;
	public static final byte CALL = 45;
	public static final byte ECHO = 46;
	public static final byte DROP = 47;
	public static final byte DUP = 48;
	public static final byte BREAK = 49;
	public static final byte TRY_ENTER = 50;
	public static final byte UNSET_STATIC_PROPERTY_ERROR = 51;
	public static final byte ISSET_STATIC_PROPERTY = 52;
	public static final byte LOGNOT = 53;
	public static final byte LOAD_STATIC = 54;
	public static final byte ASSIGN_REF_STATIC_PROPERTY = 55;
	public static final byte ASSIGN_VAL_STATIC_PROPERTY = 56;
	public static final byte ASSIGN_REF_GLOBAL = 57;
	public static final byte ASSIGN_VAL_GLOBAL = 58;
	public static final byte PLUS = 59; // unary
	
	/**
	 * Foreach loop initialisation.
	 * 
	 * <pre>
	 * operands:
	 * 		bool: isArrowSyntax (key and value)
	 * 		bool: isReference assignment
	 * 		int: offset of next instruction if iterable entity is empty
	 * 
	 * on entry:           		on successful exit:
	 * TOS: Iterable Entity         First Value
	 * 								First Key (if isArrowSyntax)
	 * 
	 * </pre>
	 */
	public static final byte FE_INIT = 60;
	
	/**
	 * Foreach loop incrementer.
	 * 
	 * <pre>
	 * operands:
	 * 		int: pc offset of next instruction backward loop branch, if there is another value
	 * 
	 * on entry:           		on successful exit:
	 * TOS: 				        Next Value
	 * 								Next Key (if isArrowSyntax)
	 * 
	 * </pre>
	 */
	public static final byte FE_NEXT = 61;
	
	/**
	 * Foreach loop resource deallocation.
	 * 
	 */
	public static final byte FE_FREE = 62;
	public static final byte ERROR = 63;
	public static final byte SILENCE = 64;
	public static final byte ADDFUNC = 65;
	public static final byte ADDCLASS = 66;
	public static final byte CHKCLASS = 67;
	public static final byte INSTANCEOF = 68;
	public static final byte CLASS_NEW = 69;
	public static final byte THROW = 70;
	public static final byte TRY_EXIT = 71;
	public static final byte PREPARE_ARG_BY_VALUE = 72;
	public static final byte FIND_FUNCTION = 73;
	public static final byte INVOKE_FUNCTION = 74;
	public static final byte FIND_METHOD = 75;
	public static final byte INVOKE_METHOD = 76;
	public static final byte ARG_CONTEXT = 77;
	public static final byte PREPARE_ARG_DYNAMIC_TARGET = 78;
	public static final byte INDIRECT = 79;
	public static final byte INDIRECT_W = 80;
	public static final byte NEWARRAY = 81;
	public static final byte ASSIGN_REF_INDEX = 82; // assign TOS-2 to
	// TOS[TOS-1] by reference
	// pop2
	public static final byte ASSIGN_VAL_INDEX = 83; // assign TOS-2 to
	// TOS[TOS-1] by value pop2
	public static final byte ASSIGN_REF_ARRAY = 84; // assign TOS-1 to TOS[] by
	// reference pop1
	public static final byte ASSIGN_VAL_ARRAY = 85; // assign TOS-1 to TOS[] by
	// value pop1
	public static final byte INDEX_R = 86;
	public static final byte INDEX_W = 87;
	public static final byte INDEX_RW = 88;
	public static final byte INDEX_U = 89;
	public static final byte INDEX_I = 90;
	public static final byte UNSET_INDEX = 91;
	public static final byte ISSET_INDEX = 92;
	public static final byte STATIC_PROPERTY = 93;
	public static final byte ARRAY_INIT_CHECK = 94;

	/**
	 * Assign to a property by reference.
	 * 
	 * @see Op#ASSIGN_VAL_PROPERTY
	 */
	public static final byte ASSIGN_REF_PROPERTY = 95;

	/**
	 * Assign to a property by value.
	 * 
	 * <pre>
	 * on entry:           on exit:
	 * TOS: Object         TOS: Value (if true)
	 *      FieldName
	 *      Value
	 * </pre>
	 */
	public static final byte ASSIGN_VAL_PROPERTY = 96;

	public static final byte ISSET_PROPERTY = 97;
	public static final byte UNSET_PROPERTY = 98;
	public static final byte PROPERTY_R = 99;

	/** Stack the named local value observing READING context. */
	public static final byte LOCAL_R = 100;
	/** Stack the named local value observing PREPARING_WRITE context. */
	public static final byte LOCAL_W = 101;
	/**
	 * Stack the named local value observing READING_AND_PREPARING_WRITE
	 * context.
	 */
	public static final byte LOCAL_RW = 102;
	/** Stack the named local value observing PREPARING_ISSET context. */
	public static final byte LOCAL_I = 103;
	public static final byte GLOBAL_R = 104;
	public static final byte GLOBAL_W = 105;
	public static final byte GLOBAL_RW = 106;
	public static final byte GLOBAL_I = 107;
	public static final byte PROPERTY_U = 108;
	public static final byte OBJECT_INIT_CHECK = 109;
	public static final byte CLASS_CONSTANT = 110;
	public static final byte FIND_STATIC_METHOD = 111;
	public static final byte EVAL = 112;
	public static final byte INCLUDE = 113;
	public static final byte MAKE_GLOBAL = 114;
	public static final byte INDEX_ENCAPS = 115;
	public static final byte CLASS_CLONE = 116;
	public static final byte INVOKE_STATIC_METHOD = 117;
	public static final byte LIST_INIT = 118;
	public static final byte LIST_NEXT = 119;
	public static final byte LIST_FREE = 120;
	public static final byte CONSTANT = 121;
	public static final byte EXIT = 122;
	public static final byte TICKER = 123;
	public static final byte PROPERTY_I = 124;
	public static final byte PROPERTY_RW = 125;
	/** Stack the named local value observing PREPARING_ISSET context. */
	public static final byte LOCAL_U = 126;
	/** Stack the named superglobal value observing PREPARING_ISSET context. */
	public static final byte GLOBAL_U = 127;
	public static final byte ARRAY_APPEND = (byte) ((int) 128);
	public static final byte ARRAY_INSERT = (byte) ((int) 129);
	/**
	 * Special case instruction for Pre and Post Increment and Decrement.
	 * Enables ASSIGN_VAL_PROPERTY to be used to trigger invocation of __set
	 * magic methods.
	 * 
	 * <pre>
	 * on entry:          on exit:
	 * TOS: Object        TOS: Property Value
	 *      FieldName          Object
	 *                         FieldName
	 *                         Property Value
	 * </pre>
	 */
	public static final byte PROPERTY_RW_INCDEC = (byte) ((int) 130);

	/**
	 * Special case instruction for Operator Assign e.g. +=. Enables
	 * ASSIGN_VAL_PROPERTY to be used to trigger invocation of __set magic
	 * methods.
	 * 
	 * <pre>
	 * on entry:           on exit:
	 * TOS: Object         TOS: Property (LHS) Value
	 *      FieldName           RHS Value
	 *      RHS Value           Object
	 *                          FieldName
	 * </pre>
	 */
	public static final byte PROPERTY_RW_OPASSIGN = (byte) ((int) 131);
	public static final byte ASSIGN_VAL_PROPERTY2 = (byte) ((int) 132);

	/** Check whether the return value of a return-by-reference function is really writable. */
	public static final byte RETURN_BY_REF_CHECK = (byte) ((int) 133);
	
	/** Enter a catch block. */
	public static final byte CATCH_ENTER = (byte) ((int) 134);

	/**
	 * Special case value reader opcodes for foreach variable: behave just like
	 * *_R opcodes, but split the variable from its cow references.
	 */
	public static final byte GLOBAL_FE = (byte) ((int) 135);
	public static final byte LOCAL_FE = (byte) ((int) 136);
	public static final byte INDEX_FE = (byte) ((int) 137);
	public static final byte PROPERTY_FE = (byte) ((int) 138);
	
	/**
	 * Create a string of the concatenation of multiple values on the stack.
	 * operand - integer number of values on the stack to concatenate
	 * 
	 * specifically 0 results in an empty string
	 */
	public static final byte MULTI_CONCAT = (byte) ((int)139);
	
	public static final byte PREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE = (byte) ((int)140);
	
	/**
	 * Assigns to an indexable only if the indexable is an object. Used
	 * in combination with opINDEX_RW_INCDEC or opINDEX_RW_OPASSIGN.
	 */
	public static final byte ASSIGN_VAL_PROP_INDEXED = (byte) ((int) 141);

	/**
	 * Like INDEX_RW but will leave the indexable and key on the stack so that
	 * if the indexable is an object, the handler can be called on the object to
	 * perform the write. Used in combination with ASSIGN_VAL_PROP_INDEXED,
	 * which will perform the write.
	 */
	public static final byte INDEX_RW_OPASSIGN = (byte) ((int) 142);
	
	/**
	 * Operand is number of actual arguments which are to be passed to the upcoming CALL.
	 * No-op in < level1 bytecode.
	 */
	public static final byte PREPARE_CALL = (byte) ((int) 143);
	
	/**
	 * Prepare argument for pass by reference.
	 */
	public static final byte PREPARE_ARG_BY_REFERENCE = (byte) ((int) 144);

	/**
	 * Prepare argumnet for pass by reference if possible.
	 */
	public static final byte PREPARE_ARG_PREFER_REFERENCE = (byte) ((int) 145);

	/**
	 * Prepare a freshly new'ed up and constructed object for reference
	 * assignment to support deprecated syntax: $r =& new C.
	 */
	public static final byte PREP_NEW_BY_REF = (byte) ((int) 146);
	
	/**
	 * PUSH without clone, for use where the value is just being wrappered
	 * in a PHPValue an cannot end up in a variable.
	 * Better solution would be to have consuming opcode handle additional 
	 * constant operand. (Thats a TODO).
	 */
	public static final byte PUSHTEMP = (byte) ((int) 147);
	
	/**
	 * CMPGE & CMPGT to make the compare opcodes symmetric.
	 */
	public static final byte CMPGE = (byte) ((int) 148);
	public static final byte CMPGT = (byte) ((int) 149);

	/**
	 * Provides increment/decrement for static class properties.
	 */
	public static final byte STATIC_PROPERTY_RW_INCDEC = (byte) ((int) 150);

	/**
	 * private constructor with initialisation common to all OpCodes.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param str .
	 * @param value .
	 * @param inInt .
	 * @param inBool .
	 * @param inOperand .
	 */
	private Op(Ast sourceAst, byte opCode, String str, PHPValue value, int inInt, boolean inBool, Operand inOperand) {
		this.operation = opCode;
		this.linenumber = sourceAst.getLineNumber();
		this.filename = sourceAst.getFileName();
		this.string = str;
		this.phpValue = value;
		this.integer = inInt;
		this.bool = inBool;
		this.operand = inOperand;
		this.tick = false;
	}

	/**
	 * Op with no operands.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 */
	public Op(Ast sourceAst, byte opCode) {
		this(sourceAst, opCode, null, null, 0, false, null);
		switch (opCode) {
		case RETURN_BY_REF_CHECK:
		case CMPLT:
		case CMPLE:
		case CMPGT:
		case CMPGE:
		case CMPID:
		case CMPEQ:
		case CMPNE:
		case CMPNI:
		case SWAP:
		case ADD:
		case SUB:
		case REM:
		case MUL:
		case DIV:
		case CASTARRY:
		case CASTBOOL:
		case CASTDOUB:
		case CASTINT:
		case CASTOBJ:
		case CASTSTR:
		case BITAND:
		case BITNOT:
		case BITOR:
		case BITSLEFT:
		case BITSRIGHT:
		case BITXOR:
		case CONCAT:
		case LOGAND:
		case LOGOR:
		case LOGXOR:
		case NEG:
		case PLUS:
		case CALL:
		case ECHO:
		case DROP:
		case DUP:
		case LOGNOT:
		case FE_FREE:
		case TRY_EXIT:
		case THROW:
		case INDIRECT:
		case INDIRECT_W:
		case NEWARRAY:
		case INDEX_R:
		case INDEX_FE:
		case INDEX_U:
		case INDEX_I:
		case UNSET_INDEX:
		case UNSET_PROPERTY:
		case UNSET_LOCAL:
		case ARRAY_INIT_CHECK:
		case ASSIGN_VAL_LOCAL:
		case ASSIGN_REF_LOCAL:
		case OBJECT_INIT_CHECK:
		case CLASS_CLONE:
		case INSTANCEOF:
		case PREP_NEW_BY_REF:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}

	}

	/**
	 * Op which take a string parameter e.g. LOCAL (varName).
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param str .
	 */
	public Op(Ast sourceAst, byte opCode, String str) {
		this(sourceAst, opCode, str, null, 0, false, null);
		switch (opCode) {
		case LOCAL_R:
		case LOCAL_FE:			
		case LOCAL_U:
		case LOCAL_W:
		case LOCAL_RW:
		case LOCAL_I:
		case UNSET_LOCAL:
		case ASSIGN_REF_LOCAL:
		case ASSIGN_VAL_LOCAL:
		case GLOBAL_R:
		case GLOBAL_FE:
		case GLOBAL_U:
		case GLOBAL_W:
		case GLOBAL_RW:
		case GLOBAL_I:
		case ISSET_GLOBAL:
		case UNSET_GLOBAL:
		case ASSIGN_REF_GLOBAL:
		case LOAD_STATIC:
		case MAKE_GLOBAL:
		case CONSTANT:
		case PROPERTY_R:
		case PROPERTY_FE:
		case PROPERTY_I:
		case PROPERTY_U:
		case UNSET_PROPERTY:
			assert str == null || str == str.intern() : "intern this operand: " + str;
			break;
		case EVAL:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op which take a <code>NameString</code> parameter.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param name .
	 * @param i .
	 */
	public Op(Ast sourceAst, byte opCode, NameString name, int i) {
		this(sourceAst, opCode, null, null, i, false, new Operand(name));

		switch (opCode) {
		case FIND_METHOD:
		case FIND_STATIC_METHOD:
		case FIND_FUNCTION:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}
	
	/**
	 * Op which take a <code>NameString</code> parameter.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param name .
	 */
	public Op(Ast sourceAst, byte opCode, NameString name) {
		this(sourceAst, opCode, null, null, 0, false, new Operand(name));

		switch (opCode) {
		case FIND_FUNCTION:
		case INSTANCEOF:
		case CLASS_CONSTANT:
		case CHKCLASS:
		case UNSET_STATIC_PROPERTY_ERROR:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}
	
	/**
	 * Op integer parameter. e.g. BRANCH +2, ECHO 2.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param i .
	 */
	public Op(Ast sourceAst, byte opCode, int i) {
		this(sourceAst, opCode, null, null, i, false, null);
		switch (opCode) {
		case BRANCH:
		case BRFALSE:
		case BRTRUE:
		case BREAK:

		case FE_NEXT:
		case TRY_ENTER:
		case PREPARE_CALL:
		case PREPARE_ARG_BY_VALUE:
		case PREPARE_ARG_BY_REFERENCE:
		case PREPARE_ARG_PREFER_REFERENCE:
		case PREPARE_ARG_DYNAMIC_TARGET:
		case INCLUDE:
		case REVERSE:
		case MULTI_CONCAT:
		case PREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE:
		case FIND_FUNCTION:
		case FIND_METHOD:
		case ADDFUNC:
		case ADDCLASS:	
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}
	
	/**
	 * Op integer parameter. e.g. BRANCH +2, ECHO 2.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param i .
	 * @param i2 .
	 */
	public Op(Ast sourceAst, byte opCode, int i, int i2) {
		this(sourceAst, opCode, null, null, i, false, new Operand(new int[] { i2 }));
		switch (opCode) {
		case CLASS_NEW:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op with integer and boolean parameter. example INVOKE_FUNCTION
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param i .
	 * @param inBool .
	 */
	public Op(Ast sourceAst, byte opCode, int i, boolean inBool) {
		this(sourceAst, opCode, null, null, i, inBool, null);
		switch (opCode) {
		case LIST_INIT:
		case INVOKE_FUNCTION:
		case INVOKE_METHOD:
		case INVOKE_STATIC_METHOD:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}
	
	/**
	 * Op with boolean parameter. example SILENCE
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inBool .
	 */
	public Op(Ast sourceAst, byte opCode, boolean inBool) {
		this(sourceAst, opCode, null, null, 0, inBool, null);
		switch (opCode) {
		case RETURN:
		case ASSIGN_REF_INDEX:
		case ASSIGN_VAL_INDEX:
		case ASSIGN_REF_ARRAY:
		case ASSIGN_VAL_ARRAY:
		case ASSIGN_VAL_PROPERTY2:
		case ASSIGN_VAL_PROP_INDEXED:
		case ASSIGN_REF_LOCAL:
		case ASSIGN_VAL_LOCAL:
		case ISSET_PROPERTY:
		case SILENCE:
		case ISSET_INDEX:
		case EXIT:
		case LIST_FREE:
		case TICKER:
		case ADD:
		case SUB:
		case MUL:
		case REM:
		case DIV:
		case BITSRIGHT:
		case BITSLEFT:
		case CONCAT:
		case BITOR:
		case BITAND:
		case BITXOR:
		case ARRAY_INSERT:
		case ARRAY_APPEND:
		case INDEX_W:
		case INDEX_RW:
		case INDEX_RW_OPASSIGN:
		case LIST_NEXT:
		case PREDEC:
		case PREINC:
		case POSTDEC:
		case POSTINC:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op with 2 boolean operands, for example PROPERTY_RW_INCDEC.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inStr .
	 * @param inBool .
	 * @param inBool2 .
	 */
	public Op(Ast sourceAst, byte opCode, String inStr, boolean inBool, boolean inBool2) {
		// overload integer for second boolean 
		this(sourceAst, opCode, inStr, null, inBool2 ? 1 : 0, inBool, null);
		switch (opCode) {
		case PROPERTY_RW_INCDEC:
			assert inStr == null || inStr == inStr.intern() : "intern this operand: " + inStr;
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}


	/**
	 * Op with PHPValue (constant).
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param value .
	 */
	public Op(Ast sourceAst, byte opCode, PHPValue value) {
		this(sourceAst, opCode, null, value, 0, false, null);
		switch (opCode) {
		case PUSH:
		case PUSHTEMP:
		case UNSET_INDEX:
		case INDEX_R:
		case INDEX_FE:
		case INDEX_I:
		case INDEX_U:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op with PHPValue (constant) and a boolean.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param value .
	 * @param inBool .
	 */
	public Op(Ast sourceAst, byte opCode, PHPValue value, boolean inBool) {
		this(sourceAst, opCode, null, value, 0, inBool, null);
		switch (opCode) {
		case INDEX_W:
		case INDEX_RW:
		case INDEX_ENCAPS:
		case ISSET_INDEX:
		case INDEX_RW_OPASSIGN:
		case ASSIGN_VAL_INDEX:
		case ASSIGN_REF_INDEX:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op ARG_CONTEXT .
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param paramPosition .
	 * @param byValueBranch .
	 * @param byRefPreferenceBranch .
	 */
	public Op(Ast sourceAst, byte opCode, int paramPosition, int byValueBranch,
			int byRefPreferenceBranch) {
		// overload 
		this(sourceAst, opCode, null, null, paramPosition, false, new Operand(new int[] { byValueBranch,
				byRefPreferenceBranch }));
		switch (opCode) {
		case ARG_CONTEXT:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op CALL.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param invocable .
	 * @param actualParameterCount .
	 * @param isRetValRedundant .
	 */
	public Op(Ast sourceAst, byte opCode, Invocable invocable,
			int actualParameterCount, boolean isRetValRedundant) {
		this(sourceAst, opCode, null, null, actualParameterCount, isRetValRedundant, new Operand(invocable));
		switch (opCode) {
		case CALL:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op FE_INIT.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inBool1 .
	 * @param inBool2 .
	 * @param inInt .
	 */
	public Op(Ast sourceAst, byte opCode, boolean inBool1, boolean inBool2, int inInt) {
		// overload object field for second boolean (Boolean(true) if true / null if false)
		this(sourceAst, opCode, null, null, inInt, inBool1, inBool2 ? new Operand() : null);
		switch (opCode) {
		case FE_INIT:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	
	
	/**
	 * Op with integer and string operands.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inInt .
	 * @param inString .
	 */
	public Op(Ast sourceAst, byte opCode, int inInt, String inString) {
		this(sourceAst, opCode, inString, null, inInt, false, null);
		switch (opCode) {
		case ERROR:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op with integer and string operands.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inInt .
	 * @param inString .
	 * @param inbool .
	 */
	public Op(Ast sourceAst, byte opCode, int inInt, NameString inString, boolean inbool) {
		this(sourceAst, opCode, null, null, inInt, inbool, new Operand(inString));
		switch (opCode) {
		case CATCH_ENTER:
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op with boolean and string operands.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inBool .
	 * @param inString .
	 */
	public Op(Ast sourceAst, byte opCode, String inString, boolean inBool) {
		this(sourceAst, opCode, inString, null, 0, inBool, null);
		switch (opCode) {
		case ASSIGN_REF_LOCAL:
		case ASSIGN_VAL_LOCAL:
		case ASSIGN_VAL_GLOBAL:
		case ASSIGN_REF_PROPERTY:
		case ASSIGN_VAL_PROPERTY:
		case ISSET_GLOBAL:
		case ISSET_LOCAL:
		case PROPERTY_RW:
		case PROPERTY_RW_OPASSIGN:
		case ISSET_PROPERTY:
			assert inString == null || inString == inString.intern() : "intern this operand: " + inString;
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * Op with boolean and string operands.
	 * 
	 * @param sourceAst .
	 * @param opCode .
	 * @param inBool .
	 * @param inNameString .
	 */
	public Op(Ast sourceAst, byte opCode, NameString inNameString, boolean inBool) {
		this(sourceAst, opCode, null, null, 0, inBool, new Operand(inNameString));
		switch (opCode) {
		case ASSIGN_REF_STATIC_PROPERTY:
		case ASSIGN_VAL_STATIC_PROPERTY:
		case STATIC_PROPERTY:
		case ISSET_STATIC_PROPERTY:
		case UNSET_STATIC_PROPERTY_ERROR:
		case STATIC_PROPERTY_RW_INCDEC:
		
			break;

		default:
			assert false : "wrong parameters for op " + OPNAME[opCode < 0 ? (int)opCode + 256 : opCode];
		}
	}

	/**
	 * 
	 * @see java.lang.Object#toString()
	 * @return String
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(OPNAME[operation & 0xff]);
		if (bool) {
			sb.append(" ");
			sb.append(bool);
		}
		if (string != null) {
			sb.append(" ");
			sb.append(string);
		}
		if (phpValue != null) {
			sb.append(" ");
			sb.append(phpValue.toString());
		}

		if (operand != null) {
			sb.append(" ");
			if (operand.invocable != null) {
				sb.append(operand.invocable.getFunctionName());
				sb.append(" ");
				sb.append(integer);
				sb.append(" args");
			}
			if (operand.name != null) {
				sb.append(operand.name.mixedCase());
			}
		}
		if (operation == Op.BRANCH || operation == Op.BRFALSE
				|| operation == Op.BRTRUE || operation == Op.CLASS_NEW
				|| operation == FE_INIT || operation == FE_NEXT) {
			sb.append(" ");
			if (integer > 0) {
				sb.append('+');
			}
			sb.append(integer);
		} else if (integer > 0) {
			sb.append(" ");
			sb.append(integer);
		}
		if (operation == Op.INVOKE_METHOD || operation == Op.INVOKE_FUNCTION
				|| operation == Op.INVOKE_STATIC_METHOD) {
			sb.append(" ");
			sb.append(integer);
			sb.append("args");
		}
		sb.append(" line: ");
		sb.append(getLineNumber());
		// if (ast != null) {
		// sb.append("\t");
		// sb.append(ast.getClass());
		// }
		return sb.toString();
	}

	/**
	 * resolveBranch.
	 * 
	 * @param branch .
	 */
	public void resolveBranch(int branch) {
		this.integer = branch;
	}

	/**
	 * sets the last opcode's tick flag.
	 */
	public void setTick() {
		this.tick = true;
	}

	/**
	 * Returns the number of items pushed onto the stack by the Op at runtime.
	 * 
	 * @return push count for this operation
	 */
	public int getPushCount() {

		switch (this.operation) {

		default:
		case PREP_NEW_BY_REF:
		case FE_NEXT: // this is 0 if the branch is taken otherwise it will be 1 or 2 
					  // we need the count when the branch is not taken
			return 0;

		case STATIC_PROPERTY_RW_INCDEC:
			return (this.bool ? 2 : 1);
			
		case LOCAL_R:
		case LOCAL_FE:
		case LOCAL_U:
		case LOCAL_W:
		case LOCAL_RW:
		case LOCAL_I:
		case PUSH:
		case PUSHTEMP:
		case DUP:
		case GLOBAL_R:
		case GLOBAL_FE:
		case GLOBAL_U:
		case GLOBAL_W:
		case GLOBAL_RW:
		case GLOBAL_I:
		case NEWARRAY:
		case ISSET_GLOBAL:
		case CONSTANT:
		case LIST_NEXT:
			return 1;
			
		case PROPERTY_RW_OPASSIGN:
			return (this.string == null) ? 1 : 2;

		case ISSET_LOCAL:
			return (this.string == null) ? 0 : 1;
			
		case CMPLT:
		case CMPLE:
		case CMPGT:
		case CMPGE:
		case CMPID:
		case CMPEQ:
		case CMPNE:
		case CMPNI:
		case BRTRUE:
		case BRFALSE:
		case ADD:
		case SUB:
		case REM:
		case MUL:
		case DIV:
		case BITAND:
		case BITOR:
		case BITSLEFT:
		case BITSRIGHT:
		case BITXOR:
		case LOGAND:
		case LOGOR:
		case LOGXOR:
		case CONCAT:
		case DROP:
		case ASSIGN_REF_GLOBAL:
		case RETURN: // value left on the stack will be picked up by invoke()
		case THROW: // like return the stack value is "consumed" at far as the
					// call site is concerned
		case BREAK:
		
		case UNSET_STATIC_PROPERTY_ERROR:
		case FIND_STATIC_METHOD:
		case ARRAY_APPEND:
		case ECHO:
			return -1;
			
		case CALL:
			return -(this.integer);

		
		case ASSIGN_VAL_PROPERTY2:
		case ASSIGN_VAL_PROP_INDEXED:
			return this.bool ? -2 : -3;

		case ASSIGN_REF_PROPERTY:
		case ASSIGN_VAL_PROPERTY:
			return this.string == null ?
					(this.bool ? -2 : -3) :
					(this.bool ? -1 : -2);
			
		case ASSIGN_REF_INDEX:
		case ASSIGN_VAL_INDEX:
			return this.phpValue == null ?
					(this.bool ? -2 : -3) :
					(this.bool ? -1 : -2);
					
		case ASSIGN_REF_LOCAL:
		case UNSET_PROPERTY:
			return this.string == null ? -2 : -1;

		case FIND_METHOD:
			return this.operand.name == null ? -2 : -1;
			
		case ASSIGN_VAL_LOCAL:
			return this.string == null ?
					(this.bool ? -1 : -2) :
					(this.bool ?  0 : -1);
			
		case INVOKE_METHOD:
		case INVOKE_FUNCTION:
		case INVOKE_STATIC_METHOD:
		case MULTI_CONCAT:
			return 1 - this.integer; // return value - args

		case ASSIGN_REF_ARRAY:
		case ASSIGN_VAL_ARRAY:
		case ASSIGN_REF_STATIC_PROPERTY:
		case ASSIGN_VAL_STATIC_PROPERTY:
			return this.bool ? -1 : -2;

		case ARRAY_INSERT:
			return -2;
			
		case UNSET_INDEX:
			return this.phpValue == null ? -2 : -1;

		case UNSET_LOCAL:
		case MAKE_GLOBAL:
		case INSTANCEOF:
		case FIND_FUNCTION:
		case PROPERTY_R:
		case PROPERTY_FE:
		case PROPERTY_I:
		case PROPERTY_U:
		case PROPERTY_RW:
		case ISSET_PROPERTY:
			return this.string == null ? -1 : 0;

		case ISSET_INDEX:	
		case INDEX_ENCAPS:
		case INDEX_R:
		case INDEX_FE:
		case INDEX_U:
		case INDEX_I:
			return this.phpValue == null ? -1 : 0;

		case LIST_FREE:
		case FE_INIT:
			return this.bool ? 1 : 0;
		
		case INDEX_W:
		case INDEX_RW:
			return this.phpValue == null ? 
					(this.bool ? -1 : 0) : 0;
					
		case EXIT:
		case TICKER:
			return this.bool ? -1 : 0;
			
		case INDEX_RW_OPASSIGN:
			return this.phpValue == null ? 
					(this.bool ? 1 : 2) : 2;

		case ASSIGN_VAL_GLOBAL:
		case POSTINC:
		case POSTDEC:
		case PREINC:
		case PREDEC:
			return this.bool ? 0 : -1;
			
		case PROPERTY_RW_INCDEC:
			return this.string == null ? 
					(this.integer == 0 ? 1 : 2) :
					(this.integer == 0 ? 2 : 3);
			
		case CATCH_ENTER:
			return 1; 
		}
	}

	public byte getOperation() {
		return this.operation;
	}

	public boolean getBool() {
		return this.bool;
	}

	/**
	 * @return lineNumber
	 */
	public int getLineNumber() {
		return linenumber;
	}

	/**
	 * 
	 * @return fileName
	 */
	public String getFilename() {
		return this.filename;
	}
	
	// IMPLEMENTATION OF OPCODES AS STATIC METHODS
	// opXXX uses PHPStack
	// jhXXX (Java ByteCode Helper) args: ( [stacked values] [RuntimeInterpreter] [constants] ) uses Java Stack
	
	/**
	 * @param runtime .
	 */
	public static void jhTick(RuntimeInterpreter runtime) {
		runtime.getTicker().tick();
	}
	
	/**
	 * @param opint .
	 * @param stack .
	 * no jhREVERSE must be implemented directly in bytecode
	 */
	public static void opREVERSE(int opint, PHPStack stack) {
		stack.reverse(opint);
		return;
	}

	/**
	 * @param runtime .
	 * @param name .
	 * @param stack .
	 */
	public static void opLOCAL_R(RuntimeInterpreter runtime,
			String name, PHPStack stack) {
		stack.push(jhLOCAL_R(runtime, name));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param name .
	 * @return readable local named (name)
	 */
	public static PHPValue jhLOCAL_R(RuntimeInterpreter runtime, String name) {
		PHPValue val = runtime.getLocals().getUnchecked(name);
		if (val == null) {
			raiseUndefinedVariableError(runtime, name);
			val = PHPValue.createNull();
		}
		return val;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opLOCAL_FE(RuntimeInterpreter runtime, 
			String opstring, PHPStack stack) {
		stack.push(jhLOCAL_FE(runtime, opstring));
		return;
	}
	
	/** 
	 * @param runtime .
	 * @param name .
	 * @return local named (name) for use in ForEach 
	 */
	public static PHPValue jhLOCAL_FE(RuntimeInterpreter runtime, String name) {
		PHPValue val = runtime.getLocals().getWritableUnchecked(name);
		if (val == null) {
			raiseUndefinedVariableError(runtime, name);
			val = PHPValue.createNull();
		}
		return val;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opLOCAL_U(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhLOCAL_U(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return unsettable local named (name)
	 */
	public static PHPValue jhLOCAL_U(RuntimeInterpreter runtime, String opstring) {
		PHPValue val = runtime.getLocals().getWritableUnchecked(opstring);
		if (val == null) {
			raiseUndefinedVariableError(runtime, opstring);
			val = PHPValue.createNull();
		}
		return val;
	}

	/** 
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opLOCAL_W(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhLOCAL_W(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return writable local named (opstring)
	 */
	public static PHPValue jhLOCAL_W(RuntimeInterpreter runtime, String opstring) {
		return runtime.getLocals().getWritable(opstring);
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opLOCAL_RW(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhLOCAL_RW(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return readable pre writable local named (opstring)
	 */
	public static PHPValue jhLOCAL_RW(RuntimeInterpreter runtime, String opstring) {
		PHPValue val = runtime.getLocals().getWritableUnchecked(opstring);
		if (val == null) {
			raiseUndefinedVariableError(runtime, opstring);
			val = PHPValue.createNull();
			runtime.getLocals().assignValue(opstring, val);
		}
		return val;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opLOCAL_I(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhLOCAL_I(runtime, opstring));
		return;
	}
	
	/**
	 * @param opstring .
	 * @param runtime .
	 * @return issetable local named (opstring)
	 */
	public static PHPValue jhLOCAL_I(RuntimeInterpreter runtime, String opstring) {
		return runtime.getLocals().get(opstring);
	}

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opINDIRECT(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhINDIRECT(stack.pop(), runtime));
		return;
	}
	
	/**
	 * 
	 * @param name .
	 * @param runtime .
	 * @return readable local named in (name)
	 */
	public static PHPValue jhINDIRECT(PHPValue name, RuntimeInterpreter runtime) {
		return jhLOCAL_R(runtime, name.getJavaString());
	}

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opINDIRECT_W(RuntimeInterpreter runtime,
			PHPStack stack) {
		stack.push(jhINDIRECT_W(stack.pop(), runtime));
		return;
	}
	
	/**
	 * 
	 * @param name .
	 * @param runtime .
	 * @return writable local named in the PHPValue (name)
	 */
	public static PHPValue jhINDIRECT_W(PHPValue name, RuntimeInterpreter runtime) {
		return runtime.getLocals().getWritable(name.getJavaString());
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opISSET_LOCAL(RuntimeInterpreter runtime,
			String opstring, boolean opbool, PHPStack stack) {
		
		if (opstring == null) {
			if (opbool) { // ensureTrue i.e. EMPTY
				stack.push(jhEMPTY_LOCAL(stack.pop(), runtime));
			} else {
				stack.push(jhISSET_LOCAL(stack.pop(), runtime));
			}
		} else {
			if (opbool) { // ensureTrue i.e. EMPTY
				stack.push(jhEMPTY_LOCAL(runtime, opstring));
			} else {
				stack.push(jhISSET_LOCAL(runtime, opstring));
			}
		}
	}
	
	/**
	 * @param nameValue .
	 * @param runtime .
	 * @return boolean PHPValue . 
	 */
	public static PHPValue jhEMPTY_LOCAL(PHPValue nameValue, RuntimeInterpreter runtime) {
		return jhEMPTY_LOCAL(runtime, nameValue.getJavaString());
	}
	
	/**
	 * @param runtime .
	 * @param name .
	 * @return boolean PHPValue . 
	 */
	public static PHPValue jhEMPTY_LOCAL(RuntimeInterpreter runtime, String name) {
		PHPValue val = jhLOCAL_I(runtime, name);
		return PHPValue.createBool(!val.getBoolean());
	}
	
	/**
	 * @param nameValue .
	 * @param runtime .
	 * @return boolean PHPValue . 
	 */
	public static PHPValue jhISSET_LOCAL(PHPValue nameValue, RuntimeInterpreter runtime) {
		return jhISSET_LOCAL(runtime, nameValue.getJavaString());
	}
	
	/**
	 * @param runtime .
	 * @param name .
	 * @return boolean PHPValue . 
	 */
	public static PHPValue jhISSET_LOCAL(RuntimeInterpreter runtime, String name) {
		PHPValue val = jhLOCAL_I(runtime, name);
		return PHPValue.createBool(val.getType() != PHPValue.Types.PHPTYPE_NULL);
	}

	/**
	 * @param runtime .
	 * @param varName .
	 * @param stack .
	 */
	public static void opUNSET_LOCAL(RuntimeInterpreter runtime,
			String varName, PHPStack stack) {
		if (varName == null) {
			jhUNSET_LOCAL(stack.pop(), runtime);
		} else {
			jhUNSET_LOCAL(runtime, varName);
		}
	}
	
	/**
	 * @param varNameValue .
	 * @param runtime .
	 */
	public static void jhUNSET_LOCAL(PHPValue varNameValue, RuntimeInterpreter runtime) {
		runtime.getLocals().unset(varNameValue.getJavaString());
	}
	
	/**
	 * @param varName .
	 * @param runtime .
	 */
	public static void jhUNSET_LOCAL(RuntimeInterpreter runtime, String varName) {
		runtime.getLocals().unset(varName);
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opGLOBAL_R(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhGLOBAL_R(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return readable global named (opstring)
	 */
	public static PHPValue jhGLOBAL_R(RuntimeInterpreter runtime, String opstring) {
		PHPValue val = runtime.getSuperGlobals().getUnchecked(opstring);
		if (val == null) {
			raiseUndefinedVariableError(runtime, opstring);
			val = PHPValue.createNull();
		}
		return val;
	}
	
	/**
	 * 
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opGLOBAL_FE(RuntimeInterpreter runtime, String opstring, PHPStack stack) {
		stack.push(jhGLOBAL_FE(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return global named (opstring) for use in ForEach
	 */
	public static PHPValue jhGLOBAL_FE(RuntimeInterpreter runtime, String opstring) {
		PHPValue val = runtime.getSuperGlobals().getWritableUnchecked(opstring);
		if (val == null) {
			raiseUndefinedVariableError(runtime, opstring);
			val = PHPValue.createNull();
		}
		return val;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opGLOBAL_U(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhGLOBAL_U(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return unsettable global named (opstring)
	 */
	public static PHPValue jhGLOBAL_U(RuntimeInterpreter runtime, String opstring) {
		PHPValue val = runtime.getSuperGlobals().getWritableUnchecked(opstring);
		if (val == null) {
			raiseUndefinedVariableError(runtime, opstring);
			val = PHPValue.createNull();
		}
		return val;
	}

	/**
	 * 
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opGLOBAL_W(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhGLOBAL_W(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return writable global named (opstring)
	 */
	public static PHPValue jhGLOBAL_W(RuntimeInterpreter runtime, String opstring) {
		return runtime.getSuperGlobals().getWritable(opstring);
	}

	/**
	 * 
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opGLOBAL_RW(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhGLOBAL_RW(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return readable in preparation for writing global named (opstring)
	 */
	public static PHPValue jhGLOBAL_RW(RuntimeInterpreter runtime, String opstring) {
		PHPValue val = runtime.getSuperGlobals().getWritableUnchecked(opstring);
		if (val == null) {
			raiseUndefinedVariableError(runtime, opstring);
			val = PHPValue.createNull();
			runtime.getLocals().assignValue(opstring, val);
		}
		return val;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opGLOBAL_I(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		stack.push(jhGLOBAL_I(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @return issetable global named (opstring)
	 */
	public static PHPValue jhGLOBAL_I(RuntimeInterpreter runtime, String opstring) {
		return runtime.getSuperGlobals().get(opstring);
	}

	/** 
	 * @param runtime .
	 * @param opstring .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opISSET_GLOBAL(RuntimeInterpreter runtime,
			String opstring, boolean opbool, PHPStack stack) {	
		if (opbool) { // ensureTrue
			stack.push(jhEMPTY_GLOBAL(runtime, opstring));
		} else {
			stack.push(jhISSET_GLOBAL(runtime, opstring));
		}
		return;
	}

	/** 
	 * @param runtime .
	 * @param name .
	 * @return boolean PHPValue true if (val) is empty
	 */
	public static PHPValue jhEMPTY_GLOBAL(RuntimeInterpreter runtime, String name) {
		PHPValue val = jhGLOBAL_I(runtime, name);
		return PHPValue.createBool(!val.getBoolean());
	}
	
	/** 
	 * Future Use - return intrinsic boolean.
	 * @param runtime .
	 * @param name .
	 * @return boolean true if (val) is empty
	 */
	public static boolean jhBoolEMPTY_GLOBAL(RuntimeInterpreter runtime, String name) {
		PHPValue val = jhGLOBAL_I(runtime, name);
		return !val.getBoolean();
	}
	
	/** 
	 * @param runtime .
	 * @param name .
	 * @return boolean PHPValue true if (val) is set
	 */
	public static PHPValue jhISSET_GLOBAL(RuntimeInterpreter runtime, String name) {
		PHPValue val = jhGLOBAL_I(runtime, name);
		return PHPValue.createBool(val.getType() != PHPValue.Types.PHPTYPE_NULL);
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 */
	 public static void opUNSET_GLOBAL(RuntimeInterpreter runtime,
			String opstring) {
		jhUNSET_GLOBAL(runtime, opstring);
	 }
	 
	 /**
	  * @param runtime .
	  * @param opstring .
	  */
	 public static void jhUNSET_GLOBAL(RuntimeInterpreter runtime, String opstring) {
		runtime.getSuperGlobals().unset(opstring);
	 }

	/**
	 * @param opphpvalue .
	 * @param stack .
	 */
	public static void opPUSH(PHPValue opphpvalue, PHPStack stack) {
		stack.push(jhPUSH(opphpvalue)); 
		return;
	}
	
	/**
	 * @param opphpvalue .
	 * @return clone of value
	 */
	public static PHPValue jhPUSH(PHPValue opphpvalue) {
		opphpvalue = opphpvalue.clone(); // clone since PHPValue must not be shared between threads
		opphpvalue.incReferences();	 // reference count is not synchronised
		return opphpvalue;
	}
	
	/**
	 * @param opphpvalue .
	 * @param stack .
	 */
	public static void opPUSHTEMP(PHPValue opphpvalue, PHPStack stack) {
		stack.push(opphpvalue); 
		return;
	}
	
	/**
	 * @param runtime .
	 * @param hop .
	 * @param stack .
	 * @param operand .
	 * @param pc .
	 * @return new pc 
	 */
	public static int opCATCH_ENTER(RuntimeInterpreter runtime, 
			int hop, PHPStack stack, Operand operand, int pc) {
		
		opDUP(stack);
		opINSTANCEOF(runtime, operand, stack);
		pc = opBRFALSE(runtime, hop, stack, pc);
		return pc;
	}

	/**
	 * 
	 * @param runtime .
	 * @param hop .
	 * @param stack .
	 * @param className .
	 * @param pc .
	 * @return new pc
	 */
	public static int opCATCH_ENTER(RuntimeInterpreter runtime, 
			int hop, PHPStack stack, NameString className, int pc) {
		
		opDUP(stack);
		opINSTANCEOF(runtime, className, stack);
		pc = opBRFALSE(runtime, hop, stack, pc);
		return pc;
	}

	/**
	 * Used in catch processing to extract the PHP Exception object.
	 * @param wrapper .
	 * @param restoreSize PHPStack size to restore.
	 * @param runtime .
	 * @return PHP exception object
	 */
	public static PHPValue jhCATCH_ENTER(ExceptionWrapper wrapper, int restoreSize, RuntimeInterpreter runtime) {
		runtime.getStack().restoreStack(restoreSize);
		return wrapper.getPHPValue();
	}
	
	/** 
	 * @param opinteger .
	 * @param pc .
	 * @return new pc value
	 */
	public static int opBRANCH(int opinteger, int pc) {
		pc += opinteger;
		return pc;
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param stack .
	 * @param pc .
	 * @return new pc value
	 */
	public static int opBRTRUE(RuntimeInterpreter runtime, int opinteger,
			PHPStack stack, int pc) {
		PHPValue val = stack.pop();
		if (val.getBoolean() == true) {
			pc += opinteger;
		}
		return pc;
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param stack .
	 * @param pc .
	 * @return new pc value
	 */
	public static int opBRFALSE(RuntimeInterpreter runtime, int opinteger,
			PHPStack stack, int pc) {
		PHPValue val = stack.pop();
		if (val.getBoolean() == false) {
			pc += opinteger;
		}
		return pc;
	}

	/**
	 * 
	 * @param runtime .
	 * @param name .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_REF_LOCAL(RuntimeInterpreter runtime,
			String name, boolean opbool, PHPStack stack) {

			if (name == null) {
				name = jhINTERNED_STRING(stack.pop());
			}
			PHPValue val = stack.pop();
			jhASSIGN_REF_LOCAL(val, name, runtime);
			if (opbool) {
				stack.push(val);
			}
			return;
	}
	
	/**
	 * @param value .
	 * @return interned String from value
	 */
	public static String jhINTERNED_STRING(PHPValue value) {
		return value.getJavaString().intern();
	}
	
	/**
	 * @param value .
	 * @param name .
	 * @param runtime .
	 */
	public static void jhASSIGN_REF_LOCAL(PHPValue value, String name, RuntimeInterpreter runtime) {
		runtime.getLocals().assignRef(name, value);
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_LOCAL(RuntimeInterpreter runtime,
			String opstring, boolean opbool, PHPStack stack) {

		String name = opstring;
		if (name == null) {
			name = jhINTERNED_STRING(stack.pop());
		}
		PHPValue val = stack.pop();
		if (opbool) {
			stack.push(jhASSIGN_VAL_LOCAL_RETURN(val, name, runtime));
		} else {
			jhASSIGN_VAL_LOCAL(val, name, runtime);
		}
		return;
	}
	
	/**
	 * @param val .
	 * @param name .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_LOCAL(PHPValue val, String name, RuntimeInterpreter runtime) {
		runtime.getLocals().assignValue(name, val);
		return;
	}
	
	/**
	 * @param val .
	 * @param name .
	 * @param runtime .
	 * @return a clone of (val) if it is referenced
	 */
	public static PHPValue jhASSIGN_VAL_LOCAL_RETURN(PHPValue val, String name, RuntimeInterpreter runtime) {
		runtime.getLocals().assignValue(name, val);
		return val.cloneIfReferenced();
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opASSIGN_REF_GLOBAL(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {
		PHPValue val = stack.pop();
		jhASSIGN_REF_GLOBAL(val, opstring, runtime);
		return;
	}
	
	/**
	 * @param val .
	 * @param name .
	 * @param runtime .
	 */
	public static void jhASSIGN_REF_GLOBAL(PHPValue val, String name, RuntimeInterpreter runtime) {
		runtime.getSuperGlobals().assignRef(name, val);
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_GLOBAL(RuntimeInterpreter runtime,
			String opstring, boolean opbool, PHPStack stack) {
		PHPValue val = stack.pop();
		if (opbool) {
			stack.push(jhASSIGN_VAL_GLOBAL_RETURN(val, opstring, runtime));
		} else { 
			jhASSIGN_VAL_GLOBAL(val, opstring, runtime);
		}		
		return;
	}
	
	/**
	 * @param val .
	 * @param name .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_GLOBAL(PHPValue val, String name, RuntimeInterpreter runtime) {
		runtime.getSuperGlobals().assignValue(name, val);
		return;
	}
	
	/**
	 * @param val .
	 * @param name .
	 * @param runtime .
	 * @return a clone of (val) if it is referenced
	 */
	public static PHPValue jhASSIGN_VAL_GLOBAL_RETURN(PHPValue val, String name, RuntimeInterpreter runtime) {
		runtime.getSuperGlobals().assignValue(name, val);
		return val.cloneIfReferenced();
	}
	
	/**
	 * 
	 * @param stack
	 * java helper implemented in bytecode
	 */
	public static void opSWAP(PHPStack stack) {
		stack.swap();
		return;
	}

	/* OPERATORS */
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPLT(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPLT(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return v1 < v2
	 */
	public static PHPValue jhCMPLT(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareLessThan(runtime, v1, v2);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPGT(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPGT(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return v1 < v2
	 */
	public static PHPValue jhCMPGT(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareLessThan(runtime, v2, v1); // reverse operands for GREATER than
	}

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPLE(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPLE(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return .
	 */
	public static PHPValue jhCMPLE(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareLessThanOrEqual(runtime, v1, v2);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPGE(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPGE(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return .
	 */
	public static PHPValue jhCMPGE(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareLessThanOrEqual(runtime, v2, v1); // reverse operands for GREATER or equal
	}

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPID(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPID(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return .
	 */
	public static PHPValue jhCMPID(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareIdentical(runtime, v1, v2);
	}

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPEQ(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPEQ(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return .
	 */
	public static PHPValue jhCMPEQ(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareEqual(runtime, v1, v2);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPNE(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPNE(v2, v1, runtime));
		return;
	}
	
	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return .
	 */
	public static PHPValue jhCMPNE(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareNotEqual(runtime, v1, v2);
	}

	/** 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCMPNI(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue v1 = stack.pop();
		PHPValue v2 = stack.pop();
		stack.push(jhCMPNI(v2, v1, runtime));
		return;
	}

	/**
	 * @param v1 .
	 * @param v2 .
	 * @param runtime .
	 * @return .
	 */
	public static PHPValue jhCMPNI(PHPValue v1, PHPValue v2, RuntimeInterpreter runtime) {
		return Operators.compareNotIdentical(runtime, v1, v2);
	}
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opADD(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhADD_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhADD(val1, val2, runtime));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param val2 .
	 * @param val1 .
	 * @return sum v1 = v1 + v2
	 * notice argument order 
	 */
	public static PHPValue jhADD_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.add(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return sum val2 + val1
	 * notice operand order 
	 */
	public static PHPValue jhADD(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.add(runtime, val1, val2);
	}

	/** 
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opSUB(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhSUB_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhSUB(val1, val2, runtime));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param val1 .
	 * @param val2 .
	 * @return difference val1 = val1 - val2
 	 * notice operand order 
	 */
	public static PHPValue jhSUB_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.subtract(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return difference val1 - val2
	 * notice operand order 
	 */
	public static PHPValue jhSUB(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.subtract(runtime, val1, val2);
	}

	/**
	 * 
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opREM(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhREM_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhREM(val1, val2, runtime));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param val1 .
	 * @param val2 .
	 * @return remainder val1 = val1 % val2
	 * notice operand order 
	 */
	public static PHPValue jhREM_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.remainder(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return remainder val2 % val1
	 * notice operand order 
	 */
	public static PHPValue jhREM(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.remainder(runtime, val1, val2);
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opMUL(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhMUL_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhMUL(val1, val2, runtime));
		return;
	}

	/**
	 * @param runtime .
	 * @param val1 .
	 * @param val2 .
	 * @return remainder val1 = val1 * val2
	 * notice operand order 
	 */
	public static PHPValue jhMUL_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.multiply(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return remainder val2 - val1
	 * notice operand order 
	 */
	public static PHPValue jhMUL(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.multiply(runtime, val1, val2);
	}
	
	/**
	 * 
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opDIV(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhDIV_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhDIV(val1, val2, runtime));
		return;
	}

	/**
	 * @param runtime .
	 * @param val1 .
	 * @param val2 .
	 * @return dividend .
	 * notice operand order 
	 */
	public static PHPValue jhDIV_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result =  Operators.divide(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return dividend val2 - val1
	 * notice operand order 
	 */
	public static PHPValue jhDIV(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.divide(runtime, val1, val2);
	}
	
	/**
	 * 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCASTARRY(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhCASTARRY(stack.pop(), runtime));
		return;
	}
	
	/**
	 * @param value .
	 * @param runtime .
	 * @return array PHPValue
	 */
	public static PHPValue jhCASTARRY(PHPValue value, RuntimeInterpreter runtime) {
		return Operators.arrayCast(runtime, value);
	}

	/**
	 * 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCASTBOOL(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhCASTBOOL(stack.pop(), runtime));
		return;
	}

	/**
	 * @param value .
	 * @param runtime .
	 * @return boolean PHPValue
	 */
	public static PHPValue jhCASTBOOL(PHPValue value, RuntimeInterpreter runtime) {
		return Operators.boolCast(runtime, value);
	}
	
	/** 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCASTDOUB(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhCASTDOUB(stack.pop(), runtime));
		return;
	}
	/**
	 * @param value .
	 * @param runtime .
	 * @return array PHPValue
	 */
	public static PHPValue jhCASTDOUB(PHPValue value, RuntimeInterpreter runtime) {
		return Operators.doubleCast(runtime, value);
	}
	
	/** 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCASTINT(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhCASTINT(stack.pop(), runtime));
		return;
	}

	/**
	 * @param value .
	 * @param runtime .
	 * @return array PHPValue
	 */
	public static PHPValue jhCASTINT(PHPValue value, RuntimeInterpreter runtime) {
		return Operators.intCast(runtime, value);
	}
	
	/**
	 * 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCASTOBJ(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhCASTOBJ(stack.pop(), runtime));
		return;
	}

	/**
	 * @param value .
	 * @param runtime .
	 * @return object PHPValue
	 */
	public static PHPValue jhCASTOBJ(PHPValue value, RuntimeInterpreter runtime) {
		return Operators.objectCast(runtime, value);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCASTSTR(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhCASTSTR(stack.pop(), runtime));
		return;
	}

	/**
	 * @param value .
	 * @param runtime .
	 * @return string PHPValue
	 */
	public static PHPValue jhCASTSTR(PHPValue value, RuntimeInterpreter runtime) {
		return Operators.stringCast(runtime, value);
	}
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opBITAND(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhBITAND_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhBITAND(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise AND val1 = val1 & val2
	 */
	public static PHPValue jhBITAND_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.bitwiseAnd(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise AND val1 & val2
	 */
	public static PHPValue jhBITAND(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.bitwiseAnd(runtime, val1, val2);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opBITNOT(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(Operators.bitwiseNot(runtime, stack.pop()));
		return;
	}
	
	/**
	 * @param v .
	 * @param runtime .
	 * @return bitwise not of value
	 */
	public static PHPValue jhBITNOT(PHPValue v, RuntimeInterpreter runtime) {
		return Operators.bitwiseNot(runtime, v);
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opBITOR(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhBITOR_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhBITOR(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise OR val2 | val1
	 */
	public static PHPValue jhBITOR_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.bitwiseOr(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise OR val2 | val1
	 */
	public static PHPValue jhBITOR(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.bitwiseOr(runtime, val1, val2);
	}
	
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opBITSLEFT(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhBITSLEFT_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhBITSLEFT(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise shift left val2 << val1
	 */
	public static PHPValue jhBITSLEFT_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.bitwiseShiftLeft(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise shift left val2 << val1
	 */
	public static PHPValue jhBITSLEFT(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.bitwiseShiftLeft(runtime, val1, val2);
	}
	
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opBITSRIGHT(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhBITSRIGHT_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhBITSRIGHT(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise shift right val2 >> val1
	 */
	public static PHPValue jhBITSRIGHT_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.bitwiseShiftRight(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise shift right val2 >> val1
	 */
	public static PHPValue jhBITSRIGHT(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.bitwiseShiftRight(runtime, val1, val2);
	}
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opBITXOR(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhBITXOR_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhBITXOR(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise shift right val2 >> val1
	 */
	public static PHPValue jhBITXOR_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.bitwiseXOr(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return bitwise shift right val2 >> val1
	 */
	public static PHPValue jhBITXOR(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.bitwiseXOr(runtime, val1, val2);
	}
	
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opCONCAT(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			PHPValue val1 = stack.pop();
			PHPValue val2 = stack.pop();
			stack.push(jhCONCAT_INPLACE(val2, val1, runtime));
			return;
		}
		PHPValue val2 = stack.pop();
		PHPValue val1 = stack.pop();
		stack.push(jhCONCAT(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return concatenation val2.val1
	 */
	public static PHPValue jhCONCAT_INPLACE(PHPValue val2, PHPValue val1, RuntimeInterpreter runtime) {
		PHPValue result = Operators.concatenate(runtime, val1, val2);
		val1.copy(result);
		return result; // must return the temporary
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return concatenation val1.val2
	 */
	public static PHPValue jhCONCAT(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.concatenate(runtime, val1, val2);
	}
	
	/** 
	 * @param runtime .
	 * @param stack .
	 * @param push .
	 */
	public static void opPREDEC(RuntimeInterpreter runtime, PHPStack stack, boolean push) {
		PHPValue value = stack.pop();
		jhDEC(value, runtime);
		if (push) {
			stack.push(jhCLONE(value));       // clone since decrement updates PHPValue 'in place'
											  // consider $a[--$i][--$i]
		}
		return;
	}
	
	/**
	 * @param value .
	 * @return unconditionally cloned PHPValue
	 */
	public static PHPValue jhCLONE(PHPValue value) {
		return value.clone();
	}
	
	/**
	 * @param value .
	 * @param runtime .
	 */
	public static void jhDEC(PHPValue value, RuntimeInterpreter runtime) {
		Operators.decrementVar(runtime, value);
	}

	/** 
	 * @param runtime .
	 * @param stack .
	 * @param push .
	 */
	public static void opPREINC(RuntimeInterpreter runtime, PHPStack stack, boolean push) {
		PHPValue value = stack.pop();
		jhINC(value, runtime);
		if (push) {
			stack.push(jhCLONE(value));       // clone since increment updates PHPValue 'in place'
											  // consider $a[++$i][++$i]
		}
		return;
	}

	/**
	 * @param value .
	 * @param runtime .
	 */
	public static void jhINC(PHPValue value, RuntimeInterpreter runtime) {
		Operators.incrementVar(runtime, value);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 * @param push .
	 */
	public static void opPOSTDEC(RuntimeInterpreter runtime, PHPStack stack, boolean push) {
		PHPValue currentValue = stack.pop();
		if (push) {
			// postfix, so leave a copy of the CURRENT value on the stack
			stack.push(jhCLONE(currentValue));
		}
		jhDEC(currentValue, runtime);
		return;
	}

	/**
	 * @param runtime .
	 * @param stack .
	 * @param push .
	 */
	public static void opPOSTINC(RuntimeInterpreter runtime, PHPStack stack, boolean push) {
		PHPValue currentValue = stack.pop();
		if (push) {
			// postfix, so leave a copy of the CURRENT value on the stack
			stack.push(jhCLONE(currentValue));
		}
		jhINC(currentValue, runtime);
		return;
	}

	/** 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opLOGAND(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue val1 = stack.pop();
		PHPValue val2 = stack.pop();
		stack.push(jhLOGAND(val1, val2, runtime));
		return;
	}
	
	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return logical AND val2 & val1 
	 */
	public static PHPValue jhLOGAND(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.logicalAnd(runtime, val2, val1);
	}
	

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opLOGOR(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue val1 = stack.pop();
		PHPValue val2 = stack.pop();
		stack.push(jhLOGOR(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return logical OR val2 | val1 
	 */
	public static PHPValue jhLOGOR(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.logicalOr(runtime, val2, val1);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opLOGXOR(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue val1 = stack.pop();
		PHPValue val2 = stack.pop();
		stack.push(jhLOGXOR(val1, val2, runtime));
		return;
	}

	/**
	 * @param val1 .
	 * @param val2 .
	 * @param runtime .
	 * @return logical XOR val2 ^ val1 
	 */
	public static PHPValue jhLOGXOR(PHPValue val1, PHPValue val2, RuntimeInterpreter runtime) {
		return Operators.logicalXOr(runtime, val2, val1);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opLOGNOT(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhLOGNOT(stack.pop(), runtime));
		return;
	}
	
	/**
	 * @param val .
	 * @param runtime .
	 * @return logical NOT (~val) 
	 */
	public static PHPValue jhLOGNOT(PHPValue val, RuntimeInterpreter runtime) {
		return Operators.logicalNot(runtime, val);
	}

	/** 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opNEG(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhNEG(stack.pop(), runtime));
		return;
	}

	/**
	 * @param val .
	 * @param runtime .
	 * @return negate (-val) 
	 */
	public static PHPValue jhNEG(PHPValue val, RuntimeInterpreter runtime) {
		return Operators.negateValue(runtime, val);
	}
	
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opPLUS(RuntimeInterpreter runtime, PHPStack stack) {
		stack.push(jhPLUS(stack.pop(), runtime));
		return;
	}

	/**
	 * @param val .
	 * @param runtime .
	 * @return unary plus (+val) 
	 */
	public static PHPValue jhPLUS(PHPValue val, RuntimeInterpreter runtime) {
		return Operators.unaryPlus(runtime, val);
	}
	
	/**
	 * 
	 * @param runtime .
	 * @param opinteger .
	 * @param invocable .
	 * @param isRetValRedundant .
	 */
	public static void opCALL(RuntimeInterpreter runtime, int opinteger,
			Invocable invocable, boolean isRetValRedundant) {
		innerINVOKE_FUNCTION(runtime, opinteger, runtime.getStack(), invocable, isRetValRedundant);
		return;
	}
	
	/**
	 * @param runtime . 
	 * @param executable .
	 * @param args .
	 * @param isRetValRendundant .
	 * @return return value
	 */
	public static PHPValue jhCALL(PHPValue[] args, ExecutableCode executable, RuntimeInterpreter runtime,
			boolean isRetValRendundant) {
		if (isRetValRendundant) {
			executable.executeVoidFunction(runtime, args);
			return null;
		} else {
			return executable.executeFunction(runtime, args);
		}
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opECHO(RuntimeInterpreter runtime, PHPStack stack) {
		jhECHO(stack.pop(), runtime);
		return;
	}
	
	/**
	 * @param value .
	 * @param runtime .
	 */
	public static void jhECHO(PHPValue value, RuntimeInterpreter runtime) {
		BuiltinLibrary.echo(runtime, value);
	}
	

	/**
	 * @param stack .
	 */
	public static void opDROP(PHPStack stack) {
		stack.pop();
		return;
	}

	/**
	 * @param stack .
	 */
	public static void opDUP(PHPStack stack) {
		stack.push(stack.peek());
		return;
	}

	/**
	 * @param runtime .
	 * @param loopNest .
	 * @param stack .
	 * @param pc .
	 * @return new pc
	 */
	public static int opBREAK(RuntimeInterpreter runtime, int loopNest,
			PHPStack stack, int pc) {
		pc += jhBREAK(stack.pop(), runtime, loopNest);
		return pc;
	}
	
	/**
	 * @param breakValue .
	 * @param runtime .
	 * @param loopNest .
	 * @return branch table entry index
	 */
	public static int jhBREAK(PHPValue breakValue, RuntimeInterpreter runtime, int loopNest) {
		int breakLevels = breakValue.getInt();
		// a computed goto with an error message
		if (breakLevels > loopNest) {
			Object[] inserts = { breakLevels };
			if (breakLevels == 1) {
				// 1 level
				runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
						"Break.InvalidNumLevel", inserts);
			} else {
				// x levels
				runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
						"Break.InvalidNumLevels", inserts);
			}
		}
		if (--breakLevels < 0) {
			breakLevels = 0;
		}
		return breakLevels;
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param arrowSyntax .
	 * @param assignRefObj .
	 * @param stack .
	 * @param pc .
	 * @param feStack .
	 * @return new pc
	 */
	public static int opFE_INIT(RuntimeInterpreter runtime, int opinteger,
			boolean arrowSyntax, Object assignRefObj, PHPStack stack, int pc,
			Stack<ForEachIterator> feStack) {
	
		// assignRefObj is used as boolean
		ForEachIterator fe = jhFE_INIT(stack.pop(), runtime, arrowSyntax, assignRefObj != null);

		feStack.push(fe); // may be null
		if (fe != null) {
			stack.push(jhFE_VALUE(fe));
			if (arrowSyntax) {
				stack.push(jhFE_KEY(fe));
			}
		} else {
			// failed to initialise so jump out
			pc += opinteger;
		}
		return pc;
	}
	
	/**
	 * @param fe iterator.
	 * @return current value
	 */
	public static PHPValue jhFE_VALUE(ForEachIterator fe) {
		return fe.getValue();
	}
	
	/**
	 * @param fe iterator.
	 * @return current key
	 */
	public static PHPValue jhFE_KEY(ForEachIterator fe) {
		return fe.getKey();
	}
	
	/**
	 * @param val .
	 * @param arrowSyntax .
	 * @param assignRef .
	 * @param runtime .
	 * @return for each iterator
	 */
	public static ForEachIterator jhFE_INIT(PHPValue val, RuntimeInterpreter runtime, boolean arrowSyntax, boolean assignRef) {

		ForEachIterator iterator = new ForEachIterator(val, arrowSyntax, assignRef);

		if (iterator.init(runtime)) {
			return iterator;
		} else {
			return null;
		}
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param stack .
	 * @param pc . 
	 * @param feStack .
	 * @return new pc 
	 */
	public static int opFE_NEXT(RuntimeInterpreter runtime, int opinteger,
			PHPStack stack, int pc, Stack<ForEachIterator> feStack) {
		ForEachIterator fe = feStack.peek();
		if (jhFE_NEXT(fe, runtime)) {
			stack.push(jhFE_VALUE(fe));
			if (fe.isArrow()) {
				stack.push(jhFE_KEY(fe));
			}
			pc += opinteger;
		}
		return pc;
	}

	/**
	 * 
	 * @param iterator .
	 * @param runtime .
	 * @return true if next value available
	 */
	public static boolean jhFE_NEXT(ForEachIterator iterator, RuntimeInterpreter runtime) {
		return iterator.next(runtime);
	}
	
	/**
	 * @param feStack
	 * no jh required
	 */
	public static void opFE_FREE(Stack<ForEachIterator> feStack) {
		feStack.pop();
		return;
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param opstring .
	 */
	public static void opERROR(RuntimeInterpreter runtime, int opinteger,
			String opstring) {
		jhERROR(runtime, opinteger, opstring);
		return;
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param opstring .
	 */
	public static void jhERROR(RuntimeInterpreter runtime, int opinteger,
			String opstring) {
		runtime.raiseExecError(opinteger, null, opstring, null);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opbool .
	 */
	public static void opSILENCE(RuntimeInterpreter runtime, boolean opbool) {
		if (opbool) {
			jhSILENCE_BEGIN(runtime);
		} else {
			jhSILENCE_END(runtime);
		}
		return;
	}

	/**
	 * @param runtime .
	 */
	public static void jhSILENCE_BEGIN(RuntimeInterpreter runtime) {
		runtime.getErrorHandler().beginSilence();
	}
	
	/**
	 * @param runtime .
	 */
	public static void jhSILENCE_END(RuntimeInterpreter runtime) {
		runtime.getErrorHandler().endSilence();
	}
	
	/**
	 * @param runtime .
	 * @param opInvNum .
	 */
	public static void opADDFUNC(RuntimeInterpreter runtime, int opInvNum) {
		jhADDFUNC(runtime, opInvNum);
		return;
	}

	/**
	 * @param runtime .
	 * @param invNum .
	 */
	public static void jhADDFUNC(RuntimeInterpreter runtime, int invNum) {
		ProgramCacheEntry pc = runtime.getExecutingProgramCacheEntry();
		if (pc == null) {
			// Program cache entry is null when inserting conditional function
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				LOGGER.log(SAPILevel.DEBUG , "1570");
			}
			throw new FatalError("Unable to access a conditional function");
		}
		Invocable inv = pc.getConditionalFunctions().get(invNum);
		if (inv == null) {
			// Function is null when inserting conditional function
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE , "1571");
			}
			throw new FatalError("Unable to access a conditional function");
		}
		runtime.getFunctions().addFunction(inv);
		return;
	}
	
	/**
	 * @param classnum .
	 * @param runtime .
	 */
	public static void opADDCLASS(RuntimeInterpreter runtime, int classnum) {
		jhADDCLASS(runtime, classnum);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param classnum .
	 */
	public static void jhADDCLASS(RuntimeInterpreter runtime, int classnum) {
		HashMap<PHPMethod, PHPMethod> oldToNewMethods = new HashMap<PHPMethod, PHPMethod>();
		HashMap<PHPPropertyDescriptor, PHPPropertyDescriptor> oldToNewProperties = new HashMap<PHPPropertyDescriptor, PHPPropertyDescriptor>();
		// deep copy so runtime modification does not change the cached PHPClass
		ProgramCacheEntry pc = runtime.getExecutingProgramCacheEntry();
		if (pc == null) {
			// Current Program is null when inserting conditional class
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE , "1569");
			}
			throw new FatalError("Unable to access a conditional class");
		}
		PHPClass  oldClass = pc.getConditionalClasses().get(classnum);
		if (oldClass == null) {
			// Conditional Function not found on cache enrty
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE , "1568");
			}
			throw new FatalError("Unable to access a conditional class");
		}
		PHPClass newClass = new PHPClass(oldClass, oldToNewMethods, oldToNewProperties);
		HashMap<PHPClass, PHPClass> oldToNewClasses = new HashMap<PHPClass, PHPClass>();
		oldToNewClasses.put(oldClass, newClass);
		newClass.fixClassReferences(oldToNewClasses);
		runtime.getClasses().addPHPClass(newClass);
		return;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 */
	public static void opCHKCLASS(RuntimeInterpreter runtime, NameString opstring) {
		jhCHKCLASS(runtime, opstring);
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 */
	public static void jhCHKCLASS(RuntimeInterpreter runtime, NameString opstring) {
		// attempt to complete class definition
		if (opstring.lowerCase() == "self" || opstring.lowerCase() == "parent") {
			// compile time error?
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null, "Class.InvalidName", new Object[] { opstring });
			return;
		}
		PHPClass phpClass = runtime.getClasses().getPHPClassWithoutChecks(opstring);
		phpClass.resolve(runtime);
		return;
	}

	/**
	 * @param runtime .
	 * @param operand .
	 * @param stack .
	 */
	public static void opINSTANCEOF(RuntimeInterpreter runtime,
			Operand operand, PHPStack stack) {
		NameString className = operand == null ? null : operand.name;
		opINSTANCEOF(runtime, className, stack);
	}
	
	/**
	 * @param runtime .
	 * @param className .
	 * @param stack .
	 */
	public static void opINSTANCEOF(RuntimeInterpreter runtime,
			NameString className, PHPStack stack) {
		PHPValue result;
		if (className == null) {
			PHPValue matchValue = stack.pop();
			PHPValue object = stack.pop();
			result = jhINSTANCEOF_PV(object, matchValue, runtime);
		} else {
			PHPValue object = stack.pop();
			result = jhINSTANCEOF_PV(object, runtime, className);
		}
		stack.push(result);
		return;
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @param className .
	 * @return true if object instance of className
	 */
	public static PHPValue jhINSTANCEOF_PV(PHPValue object, RuntimeInterpreter runtime, NameString className) {
		return PHPValue.createBool(jhINSTANCEOF(object, runtime, className));
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @param className .
	 * @return true if object instance of className
	 */
	public static boolean jhINSTANCEOF(PHPValue object, RuntimeInterpreter runtime, NameString className) {
		
		if (object.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			return false;
		}

		Classes classes = runtime.getClasses();
		if (!classes.isClassDefined(className, false)) {
			return false;
		}
		
		return ObjectFacade.instanceOf(runtime, object, classes.getPHPClass(className));
	}

	/**
	 * @param matchValue .
	 * @param object .
	 * @param runtime .
	 * @return true if object instanceof type matchValue ( name or instance) 
	 */
	public static PHPValue jhINSTANCEOF_PV(PHPValue object, PHPValue matchValue, RuntimeInterpreter runtime) {
		return PHPValue.createBool(jhINSTANCEOF(object, matchValue, runtime));
	}
	
	/**
	 * @param object .
	 * @param matchValue .
	 * @param runtime .
	 * @return true if object instanceof type matchValue ( name or instance) 
	 */
	public static boolean jhINSTANCEOF(PHPValue object, PHPValue matchValue, RuntimeInterpreter runtime) {

		PHPClass classToMatch;
		switch (matchValue.getType()) {
		case PHPTYPE_OBJECT:
			classToMatch = ObjectFacade.getPHPClass(matchValue);
			break;
		case PHPTYPE_STRING:
			Classes classes = runtime.getClasses();
			// className must be a j.l.String so assume matchValue can be
			// retrieved as a j.l.String.
			NameString className = new NameString(matchValue.getJavaString());
			if (!classes.isClassDefined(className, false)) {
				return false;
			}
			classToMatch = classes.getPHPClass(className);
			break;
			
		default:
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null, "Class.NotObjectOrString", null);
			return false;
		}
		
		if (object.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			return false;
		}
			
		return ObjectFacade.instanceOf(runtime, object, classToMatch);
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 * @param classes .
	 * @param catchStack .
	 * @param pc .
	 * @return xx 
	 */
	public static int opTHROW(RuntimeInterpreter runtime, PHPStack stack,
			Classes classes, Stack<CatchStackEntry> catchStack, int pc) {
		// exception object is on the stack
		PHPValue val = stack.peek();
		
		opTHROW_CHECK(runtime, val, classes);
		
		if (catchStack.size() > 0) {
			// goto catch block leaving value on the stack
			CatchStackEntry catchEntry = catchStack.pop();
			pc = catchEntry.getCatchPC();
			if (stack.size() > catchEntry.getStackSize() + 1) {
				PHPValue exception = stack.pop();
				stack.restoreStack(catchEntry.getStackSize());
				stack.push(exception);
			}
		} else {
			val = stack.pop();
			runtime.getErrorHandler().abortSilence();
			throw new ExceptionWrapper(runtime, val);
		}
		return pc;
	}
	
	/**
	 * @param exception .
	 * @param runtime .
	 */
	public static void jhTHROW(PHPValue exception, RuntimeInterpreter runtime) {

		opTHROW_CHECK(runtime, exception, runtime.getClasses());
		runtime.getErrorHandler().abortSilence();
		throw new ExceptionWrapper(runtime, exception);
	}


	/**
	 * @param runtime .
	 * @param exception .
	 * @param classes .
	 */
	public static void opTHROW_CHECK(RuntimeInterpreter runtime, PHPValue exception,
			Classes classes) {
		// value must be an object
		if (exception.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Throw.NotAnObject", null);
		}

		// object must be an instance of the Exception class
		if (!ObjectFacade.instanceOf(runtime, exception, classes
				.getPHPClass(PHPException.PHP_CLASS_NAMESTRING))) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Throw.NotDerivedFromException", null);
		}
	}

	/**
	 * @param opinteger .
	 * @param stack .
	 * @param catchStack .
	 * @param pc .
	 */
	public static void opTRY_ENTER(int opinteger, PHPStack stack,
			Stack<CatchStackEntry> catchStack, int pc) {
		catchStack.push(new CatchStackEntry(pc + opinteger, stack.size()));
		return;
	}

	/**
	 * @param catchStack .
	 */
	public static void opTRY_EXIT(Stack<CatchStackEntry> catchStack) {
		catchStack.pop();
		return;
	}

	/**
	 * @param stack .
	 */
	public static void opPREPARE_ARG_BY_VALUE(PHPStack stack) {
		stack.push(ArgumentSemantics.passByValue(stack.pop()));
	}
	
	/**
	 * @param stack .
	 */
	public static void opPREPARE_ARG_BY_REFERENCE(PHPStack stack) {
		stack.push(ArgumentSemantics.passByReferenceWarn(stack.pop()));
	}
	
	/**
	 * @param stack .
	 */
	public static void opPREPARE_ARG_PREFER_REFERENCE(PHPStack stack) {
		stack.push(ArgumentSemantics.passByReference(stack.pop()));
	}
	
	/**
	 * @param value .
	 * @return value
	 */
	public static PHPValue jhPREPARE_ARG_BY_VALUE(PHPValue value) {
		return ArgumentSemantics.passByValue(value);
	}
	
	/**
	 * @param value .
	 * @return value
	 */
	public static PHPValue jhPREPARE_ARG_PREFER_REFERENCE(PHPValue value) {
		return ArgumentSemantics.passByReference(value);
	}
	
	/**
	 * @param value .
	 * @return value 
	 */
	public static PHPValue jhPREPARE_ARG_BY_REFERENCE(PHPValue value) {
		return ArgumentSemantics.passByReferenceWarn(value);
	}

	/**
	 * @param stack .
	 * @param argIndex .
	 * @param invocableStack .
	 */
	public static void opPREPARE_ARG_DYNAMIC_TARGET(
			PHPStack stack, int argIndex,
			InvocableStack invocableStack) {
		
		stack.push(jhPREPARE_ARG_DYNAMIC_TARGET(stack.pop(), invocableStack.peek(), argIndex));
		return;
	}
	
	/**
	 * @param arg .
	 * @param obj ExecutableCode or InvocableStackEntry .
	 * @param argIndex .
	 * @return prepared argument
	 */
	public static PHPValue jhPREPARE_ARG_DYNAMIC_TARGET(PHPValue arg, Object obj,  
			int argIndex) {
				
		Invocable invocable;
		// transitional code, will always be ExecutableCode when method calls are fully thunked
		// TODO: remove test and change argument type
		if (obj instanceof InvocableStackEntry) {
			invocable = ((InvocableStackEntry)obj).getInvocable();
		} else {
			assert (obj instanceof ExecutableCode);
			invocable = ((ExecutableCode)obj).getInvocable();
		}
		XAPIPassSemantics passSemantics = invocable.getParameterPassSemantics(argIndex);
		return ArgumentSemantics.passSemantics(arg, passSemantics);
	}
	
	/**
	 * @param val .
	 * @param runtime .
	 * @return NameString
	 */
	private static NameString jhFUNCTION_NAMESTRING(PHPValue val, RuntimeInterpreter runtime) {
		if (!(val.getType() == PHPValue.Types.PHPTYPE_STRING)) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Function.NameNotString", null);
		}
		return new NameString(val.getJavaString());
	}
	
	/**
	 * @param functionName .
	 * @param runtime .
	 * @return ExecutableCode . 
	 */
	public static ExecutableCode jhFIND_FUNCTION(RuntimeInterpreter runtime, NameString functionName) {
		return runtime.getFunctions().lookupScriptFunction(functionName).getThunk();
	}
	
	/**
	 * @param nameValue .
	 * @param runtime .
	 * @return InvocableStackEntry
	 */
	public static ExecutableCode jhFIND_FUNCTION(PHPValue nameValue, RuntimeInterpreter runtime) {
		return jhFIND_FUNCTION(runtime, jhFUNCTION_NAMESTRING(nameValue, runtime));
	}
	
	/**
	 * @param runtime .
	 * @param operand .
	 * @param invocableStack .
	 */
	public static void opFIND_FUNCTION(RuntimeInterpreter runtime,
			Operand operand, InvocableStack invocableStack) {
		NameString functionName;
		if (operand == null) {
			functionName = jhFUNCTION_NAMESTRING(runtime.getStack().pop(), runtime);
		} else {
			functionName = operand.name;
		}
		invocableStack.push(new InvocableStackEntry(runtime.getFunctions().lookupScriptFunction(functionName)));
	}

	/**
	 * @param runtime .
	 * @param stack .
	 * @param invocableStack .
	 * @param methodName .
	 */
	public static void opFIND_METHOD(RuntimeInterpreter runtime,
			PHPStack stack, InvocableStack invocableStack, NameString methodName) {
		PHPValue baseObject = stack.pop();
		
		if (methodName == null) {
			PHPValue methodValue = stack.pop();
			invocableStack.push(jhFIND_METHOD(methodValue, baseObject, runtime));
		} else {
			invocableStack.push(jhFIND_METHOD(baseObject, runtime, methodName));
		}
		

		
		return;
	}
	
	/**
	 * @param methodValue .
	 * @param baseObject .
	 * @param runtime .
	 * @return ise
	 */
	public static InvocableStackEntry jhFIND_METHOD(PHPValue methodValue, PHPValue baseObject, RuntimeInterpreter runtime) {
		
		if (!(methodValue.getType() == PHPValue.Types.PHPTYPE_STRING)) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.MethodNameNotString", null);
		}
		NameString methodName = new NameString(methodValue.getJavaString());
		return jhFIND_METHOD(baseObject, runtime, methodName);
	}
	
	/**
	 * @param methodName .
	 * @param baseObject .
	 * @param runtime .
	 * @return ise
	 */
	public static InvocableStackEntry jhFIND_METHOD(PHPValue baseObject, RuntimeInterpreter runtime, NameString methodName) {
		

		if (baseObject.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			Object[] inserts = { methodName };
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Object.CallOnNonObject", inserts);
			return null;
		}

		if (methodName.sameAs(MagicMethodInfo.CLONE.getName())) {
			// TODO: direct calls to __clone() should trigger a fatal at parse or compile time.
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.DirectCallToClone", null);
		}

		PHPClass phpClass = ObjectFacade.getPHPClass(baseObject);

		// Obtain method to invoke using object handler. Might return __call if
		// the method does not exist or is not visible.
		PHPMethodAndCallType methodAndCallType = ObjectFacade.getMethod(runtime, baseObject, methodName, true);
		if (methodAndCallType == null) {
			Object[] inserts = { phpClass.getName(), methodName };
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null, "Class.UndefinedMethod", inserts);
			return null;
		}
	
		return new InvocableStackEntry(baseObject, methodAndCallType.getMethod(),
				methodName, methodAndCallType.getImplicitCallType());
	}

	/** 
	 * @param methodEntry .
	 * @param noArgs .
	 * @param stack .
	 * @param runtime .
	 * @param isReturnValueRedundant .
	 */
	public static void innerINVOKE_METHOD(RuntimeInterpreter runtime,
			int noArgs, PHPStack stack, InvocableStackEntry methodEntry, boolean isReturnValueRedundant) {
		
		// interesting args are on the stack so adapt
		PHPValue[] args = null;
		if (noArgs > 0) {
			args = new PHPValue[noArgs];
			for (int index = noArgs - 1; index >= 0; index--) {
				args[index] = stack.pop();
			}
		}
		
		PHPValue returnVal;
		switch (methodEntry.getImplicitCallType()) {
		case NotImplicit:
			if (methodEntry.getMethod().isStatic()) {
				returnVal = methodEntry.getMethod().invoke(runtime, null, isReturnValueRedundant, args);
			} else {
				returnVal = methodEntry.getMethod().invoke(runtime, methodEntry.getBaseObject(), isReturnValueRedundant, args);
			}
			break;
		case __Call:
			returnVal = methodEntry.getMethod().invokeImplicit__call(runtime, methodEntry.getMethodName(), methodEntry.getBaseObject(), isReturnValueRedundant, args);			break;
		case __CallStatic:
			returnVal = methodEntry.getMethod().invokeImplicit__callStatic(runtime, methodEntry.getMethodName(), isReturnValueRedundant, args);
			break;
		default:
			assert false;
			returnVal = null;
		}

		stack.push(returnVal);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param noArgs .
	 * @param stack .
	 * @param invocableStack .
	 * @param isReturnValueRedundant .
	 */
	public static void opINVOKE_METHOD(RuntimeInterpreter runtime,
			int noArgs, PHPStack stack, InvocableStack invocableStack,
			boolean isReturnValueRedundant) {
		
		InvocableStackEntry methodEntry = invocableStack.pop();
		innerINVOKE_METHOD(runtime, noArgs, stack, methodEntry, isReturnValueRedundant);
	}
	
	/** 
	 * @param args method arguments.
	 * @param methodEntry .
	 * @param runtime .
	 * @param isReturnValueRedundant .
	 * @return method return value .
	 */
	public static PHPValue jhINVOKE_METHOD(PHPValue[] args, InvocableStackEntry methodEntry, RuntimeInterpreter runtime,
			boolean isReturnValueRedundant) {
		
		switch (methodEntry.getImplicitCallType()) {
		case NotImplicit:
			if (methodEntry.getMethod().isStatic()) {
				return methodEntry.getMethod().invoke(runtime, null, isReturnValueRedundant, args);
			} else {
				return methodEntry.getMethod().invoke(runtime, methodEntry.getBaseObject(),
						isReturnValueRedundant, args);
			}
		case __Call:
			return methodEntry.getMethod().invokeImplicit__call(runtime, methodEntry.getMethodName(), methodEntry.getBaseObject(), isReturnValueRedundant, args);
		case __CallStatic:
			return methodEntry.getMethod().invokeImplicit__callStatic(runtime, methodEntry.getMethodName(), isReturnValueRedundant, args);
		default:
			assert false;
			return null;
		}
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param stack .
	 * @param invocableStack .
	 * @param isReturnValueRedundant .
	 */
	public static void opINVOKE_FUNCTION(RuntimeInterpreter runtime,
			int opinteger, PHPStack stack, InvocableStack invocableStack,
			boolean isReturnValueRedundant) {
		innerINVOKE_FUNCTION(runtime, opinteger, stack,
				invocableStack.pop().getInvocable(), isReturnValueRedundant);
	}
	
	/**
	 * Wrap invocable.call() and adapt to arguments/result on PHPStack.
	 * @param runtime .
	 * @param noArgs .
	 * @param stack .
	 * @param invocable .
	 * @param isReturnValueRedundant .
	 */
	public static void innerINVOKE_FUNCTION(RuntimeInterpreter runtime,
			int noArgs, PHPStack stack, Invocable invocable,
			boolean isReturnValueRedundant) {
		
		PHPValue[] args = new PHPValue[noArgs];
		for (int i = noArgs - 1; i >= 0; i--) {
			args[i] = stack.pop();
		}
		PHPValue rValue = invocable.call(runtime, null, isReturnValueRedundant, args);
		stack.push(rValue);
		return;
	}
	
	/**
	 * @param args function arguments.
	 * @param executable .
	 * @param runtime .
	 * @param isReturnValueRedundant .
	 * @return function return value
	 */
	public static PHPValue jhINVOKE_FUNCTION(PHPValue[] args, ExecutableCode executable, RuntimeInterpreter runtime,
			boolean isReturnValueRedundant) {
		
		if (isReturnValueRedundant) {
			executable.executeVoidFunction(runtime, args);
			return null;
		} else {
			return executable.executeFunction(runtime, args);
		}
	}

	/**
	 * @param argIndex .
	 * @param opbranchTable .
	 * @param invocableStack .
	 * @param pc .
	 * @return new pc 
	 */
	public static int opARG_CONTEXT(int argIndex, int[] opbranchTable,
			InvocableStack invocableStack, int pc) {

		switch (innerARG_CONTEXT(invocableStack.peek(), argIndex)) {
		case ByValue:
			pc += opbranchTable[0];
			break;
		case ByReference:
		case PreferByReference:
			pc += opbranchTable[1];
			break;
		default:
			assert false;
		}
		return pc;
	}
	
	/**
	 * @param obj ExecutableCode or InvocableStackEntry.
	 * @param argIndex .
	 * @return pass Semantic indicator
	 */
	private static XAPIPassSemantics innerARG_CONTEXT(Object obj, int argIndex) {
		
		Invocable inv;
		// transitional code, will always be ExecutableCode when method calls are fully thunked
		// TODO: remove test and change argument type
		if (obj instanceof InvocableStackEntry) {
			inv = ((InvocableStackEntry)obj).getInvocable();
		} else { 
			assert (obj instanceof ExecutableCode);
			inv = ((ExecutableCode)obj).getInvocable();
		}
		return inv.getParameterPassSemantics(argIndex);
	}

	/**
	 * @param obj ExecutableCode or InvocableStackEntry.
	 * @param argIndex .
	 * @return value(0) reference(1)
	 */
	public static int jhARG_CONTEXT(Object obj, int argIndex) {
		switch(innerARG_CONTEXT(obj, argIndex)) {
		case ByReference:
		case PreferByReference:
			return 1; 
		case ByValue:
			return 0;
		default: 
			assert false;
			return 0;
		}
	}
	/**
	 * @param runtime .
	 * @param argIndex .
	 * @param invocableStack .
	 */
	public static void opPREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE(
			RuntimeInterpreter runtime,
			int argIndex,
			InvocableStack invocableStack) {
		
		InvocableStackEntry entry = invocableStack.peek();
		PHPStack stack = runtime.getStack();
		
		stack.push(jhPREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE(stack.pop(), entry, runtime, argIndex));
		return;
	}
	
	/**
	 * @param arg .
	 * @param obj ExecutableCode or InvocableStackEntry .
	 * @param runtime .
	 * @param argIndex .
	 * @return prepared argument
	 */
	public static PHPValue jhPREPARE_ARG_DYNAMIC_TARGET_NOT_REFERABLE(PHPValue arg, Object obj,  
			RuntimeInterpreter runtime, int argIndex) {
		
		Invocable invocable;
		// transitional code, will always be ExecutableCode when method calls are fully thunked
		// TODO: remove test and change argument type
		if (obj instanceof InvocableStackEntry) {
			invocable = ((InvocableStackEntry)obj).getInvocable();
		} else {
			assert (obj instanceof ExecutableCode);
			invocable = ((ExecutableCode)obj).getInvocable();
		}
		
		switch (invocable.getParameterPassSemantics(argIndex)) {
		case ByValue:
		case PreferByReference:
			break;
		default:
			jhERROR(runtime, ErrorType.E_ERROR, "ParamDecl.ConstantByRef");
		}
		// The argument is non-referable, so always pass by value. 
		return jhPREPARE_ARG_BY_VALUE(arg);
	}

	/** 
	 * @param stack .
	 */
	public static void opNEWARRAY(PHPStack stack) {
		stack.push(jhNEWARRAY());
		return;
	}
	
	/**
	 * @return new empty array PHPValue 
	 */
	public static PHPValue jhNEWARRAY() {
		return PHPValue.createArray();
	}

	/** 
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_REF_ARRAY(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {
		PHPValue indexable = stack.pop();
		PHPValue value = stack.pop();
		if (opbool) {
			stack.push(jhASSIGN_REF_ARRAY_RET_VAL(value, indexable, runtime));
		} else {
			jhASSIGN_REF_ARRAY(value, indexable, runtime);
		}

		return;
	}
	
	/** 
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhASSIGN_REF_ARRAY_RET_VAL(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignReferenceNoKey(runtime, indexable, value);
		return value;
	}
	
	/** 
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 */
	public static void jhASSIGN_REF_ARRAY(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignReferenceNoKey(runtime, indexable, value);
		return;
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_ARRAY(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {
		PHPValue indexable = stack.pop();
		PHPValue value = stack.pop();

		if (opbool) {
			stack.push(jhASSIGN_VAL_ARRAY_RETURN(value, indexable, runtime));
		} else {
			jhASSIGN_VAL_ARRAY(value, indexable, runtime);
		}
		
		return;
	}
	
	/** 
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_ARRAY(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignValueNoKey(runtime, indexable, value);
		return;
	}
	
	/** 
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 * @return value cloned if referenced
	 */
	public static PHPValue jhASSIGN_VAL_ARRAY_RETURN(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignValueNoKey(runtime, indexable, value);
		return value.cloneIfReferenced();
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param key .
	 * @param stack .
	 */
	public static void opASSIGN_REF_INDEX(RuntimeInterpreter runtime,
			boolean opbool, PHPValue key, PHPStack stack) {
		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		PHPValue value = stack.pop();
		if (opbool) {
			stack.push(jhASSIGN_REF_INDEX_RETURN(value, key, indexable, runtime));
		} else {
			jhASSIGN_REF_INDEX(value, key, indexable, runtime);
		}

		return;
	}

	/**
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 */
	public static void jhASSIGN_REF_INDEX(PHPValue value, PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignReference(runtime, indexable, key, value);
		return;
	}
	
	/**
	 * constant key variant.
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 */
	public static void jhASSIGN_REF_INDEX(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		Indexable.assignReference(runtime, indexable, key, value);
		return;
	}
	
	/**
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhASSIGN_REF_INDEX_RETURN(PHPValue value, PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignReference(runtime, indexable, key, value);
		return value;
	}
	
	/**
	 * constant key variant.
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhASSIGN_REF_INDEX_RETURN(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		Indexable.assignReference(runtime, indexable, key, value);
		return value;
	}
	
	/**
	 * @param runtime .
	 * @param leaveCloneOnStack .
	 * @param key .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_INDEX(RuntimeInterpreter runtime,
			boolean leaveCloneOnStack, PHPValue key, PHPStack stack) {
		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		PHPValue value = stack.pop();
		
		if (leaveCloneOnStack) {
			stack.push(jhASSIGN_VAL_INDEX_RETURN(value, key, indexable, runtime));
		} else {
			jhASSIGN_VAL_INDEX(value, key, indexable, runtime);
		}
		
		return;
	}
	
	/**
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return clone of value
	 */
	public static PHPValue jhASSIGN_VAL_INDEX_RETURN(PHPValue value, PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {

		jhASSIGN_VAL_INDEX(value, key, indexable, runtime);

		if (indexable.getType() == PHPValue.Types.PHPTYPE_STRING) {
			byte[] contents = indexable.getByteArray();
			PHPValue result  = PHPValue.createString(new byte[] { contents[key.getInt()] });
			return result.cloneIfReferenced();
		} else {
			return value.cloneIfReferenced();
		}
	}
	
	/**
	 * constant key variant.
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return clone of value
	 */
	public static PHPValue jhASSIGN_VAL_INDEX_RETURN(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {

		jhASSIGN_VAL_INDEX(value, key, indexable, runtime);

		if (indexable.getType() == PHPValue.Types.PHPTYPE_STRING) {
			byte[] contents = indexable.getByteArray();
			PHPValue result  = PHPValue.createString(new byte[] { contents[key.getInt()] });
			return result.cloneIfReferenced();
		} else {
			return value.cloneIfReferenced();
		}
	}
	
	/**
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_INDEX(PHPValue value, PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.assignValue(runtime, indexable, key, value);
	}
	
	/**
	 * constant key variant.
	 * @param value .
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_INDEX(PHPValue value, PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		Indexable.assignValue(runtime, indexable, key, value);
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opARRAY_APPEND(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {
		// like ASSIGN_VAL_ARRAY but different stack order and leaves indexable on stack
		PHPValue value = stack.pop();
		PHPValue indexable = stack.peek();

		if (opbool) {
			jhARRAY_APPEND_REF(indexable, value, runtime);
		} else {
			jhARRAY_APPEND_VAL(indexable, value, runtime);
		}
		return;
	}
	
	/**
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 * @return indexable
	 */
	public static PHPValue jhARRAY_APPEND_REF(PHPValue indexable, PHPValue value, RuntimeInterpreter runtime) {
		// like ASSIGN_REF_ARRAY but different stack order and leaves indexable on stack (!!!!!)
		Indexable.assignReferenceNoKey(runtime, indexable, value);
		return indexable;
	}
	
	/**
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 * @return indexable
	 */
	public static PHPValue jhARRAY_APPEND_VAL(PHPValue indexable, PHPValue value, RuntimeInterpreter runtime) {
		// like ASSIGN_VAL_ARRAY but different stack order and leaves indexable on stack (!!!!!)
		Indexable.assignValueNoKey(runtime, indexable, value);
		return indexable;
	}

	/** 
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opARRAY_INSERT(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {
		// like ASSIGN_VAL_INDEX but different stack order and leaves indexable
		// on stack
		PHPValue value = stack.pop();
		PHPValue key = stack.pop();
		PHPValue indexable = stack.peek();
		
		if (opbool) {
			jhARRAY_INSERT_REF(indexable, key, value, runtime);
		} else {
			jhARRAY_INSERT_VAL(indexable, key, value, runtime);
		}
		return;
	}
	
	/** 
	 * @param indexable .
	 * @param key .
	 * @param value .
	 * @param runtime .
	 * @return indexable
	 */
	public static PHPValue jhARRAY_INSERT_VAL(PHPValue indexable, PHPValue key, PHPValue value, RuntimeInterpreter runtime) {
		// like ASSIGN_VAL_INDEX but different stack order and leaves indexable
		// on stack

		// Stop resources as array indexes unless they are cast to integers first!
		if (key.getType() == PHPValue.Types.PHPTYPE_RESOURCE) {
			runtime.raiseExecError(ErrorType.E_WARNING, 
				null, "Array.IllegalKey", null);
			return indexable;
		}

		Indexable.assignValue(runtime, indexable, key, value);
		return indexable;
	}
	
	/** 
	 * @param indexable .
	 * @param key .
	 * @param value .
	 * @param runtime .
	 * @return indexable
	 */
	public static PHPValue jhARRAY_INSERT_REF(PHPValue indexable, PHPValue key, PHPValue value, RuntimeInterpreter runtime) {
		// like ASSIGN_VAL_INDEX but different stack order and leaves indexable
		// on stack

		// Stop resources as array indexes unless they are cast to integers first!
		if (key.getType() == PHPValue.Types.PHPTYPE_RESOURCE) {
			runtime.raiseExecError(ErrorType.E_WARNING, 
				null, "Array.IllegalKey", null);
			return indexable;
		}
		
		Indexable.assignReference(runtime, indexable, key, value);
		return indexable;
	}

	/**
	 * @param runtime .
	 * @param key .
	 */
	public static void opINDEX_R(RuntimeInterpreter runtime, PHPValue key) {
		PHPStack stack = runtime.getStack();
		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		stack.push(jhINDEX_R(key, indexable, runtime));
	}
	
	/**
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_R(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		return Indexable.getReading(key, indexable, runtime);
	}
	
	/**
	 * constant key variant.
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_R(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		return Indexable.getReading(key, indexable, runtime);
	}
	
	/**
	 * @param runtime .
	 * @param key .
	 */
	public static void opINDEX_FE(RuntimeInterpreter runtime, PHPValue key) {
		PHPStack stack = runtime.getStack();
		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		stack.push(jhINDEX_FE(key, indexable, runtime));
	}

	/**
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhINDEX_FE(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		return Indexable.getReadingPrepFE(key, indexable, runtime);
	}
	
	/**
	 * constant key variant.
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhINDEX_FE(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		return Indexable.getReadingPrepFE(key, indexable, runtime);
	}

	/**
	 * @param runtime .
	 * @param key .
	 * @param keyIsStacked .
	 */
	public static void opINDEX_W(RuntimeInterpreter runtime, PHPValue key, boolean keyIsStacked) {
		PHPStack stack = runtime.getStack();
		PHPValue indexable = stack.pop();
		if (key == null) {
			if (keyIsStacked) {
				key = stack.pop();
			}
		}
		if (key == null) {
			stack.push(jhINDEX_W(indexable, runtime));
		} else {
			stack.push(jhINDEX_W(key, indexable, runtime));
		}
		return;
	}
	
	/**
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_W(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		
		return Indexable.getWritable(key, indexable, runtime);
	}
	
	/**
	 * constant key variant.
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_W(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		
		return Indexable.getWritable(key, indexable, runtime);
	}
	
	/**
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_W(PHPValue indexable, RuntimeInterpreter runtime) {
		
		return Indexable.getWritable(indexable, runtime);
	}

	/**
	 * @param runtime .
	 * @param key .
	 * @param keyIsStacked .
	 */
	public static void opINDEX_RW(RuntimeInterpreter runtime, PHPValue key, boolean keyIsStacked) {
		PHPStack stack = runtime.getStack();
		PHPValue indexable = stack.pop();
		if (key == null) {
			if (keyIsStacked) {
				key = stack.pop();
			}
		}
		if (key == null) {
			stack.push(jhINDEX_RW(indexable, runtime));
		} else {
			stack.push(jhINDEX_RW(key, indexable, runtime));
		}
		return;
	}
	
	/**
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_RW(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		
		return Indexable.getReadingPrepWrite(key, indexable, runtime);
	}
	
	/**
	 * constant key variant.
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_RW(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		
		return Indexable.getReadingPrepWrite(key, indexable, runtime);
	}
	
	/**
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_RW(PHPValue indexable, RuntimeInterpreter runtime) {
		
		return Indexable.getReadingPrepWrite(indexable, runtime);
	}
	
	
	/**
	 * @param runtime .
	 * @param key .
	 * @param keyIsStacked .
	 */
	public static void opINDEX_RW_OPASSIGN(RuntimeInterpreter runtime, PHPValue key, boolean keyIsStacked) {
		PHPStack stack = runtime.getStack();

		PHPValue indexable = stack.pop();
		if (key == null) {
			if (keyIsStacked) {
				key = stack.pop();
			}
		}

		// if we are leaving the key and indexable on the stack, we must
		// place the operand above them for the subsequent operation.
		PHPValue operand =  stack.pop();

		PHPValue result;
		if (key != null) {
			result = jhINDEX_RW_OPASSIGN(key, indexable, runtime);
		} else {
			result = jhINDEX_RW_OPASSIGN_NOKEY(null, indexable, runtime);
		}

		stack.push(key);
		stack.push(indexable);
		stack.push(operand);

		stack.push(result);
		return;
	}

	/**
	 * @param key .
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_RW_OPASSIGN(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		
		return Indexable.getReadingPrepWriteOpassign(key, indexable, runtime);
	}
	
	/**
	 * @param dummyKey ignored
	 * @param indexable .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_RW_OPASSIGN_NOKEY(PHPValue dummyKey, PHPValue indexable, RuntimeInterpreter runtime) {
		
		return Indexable.getReadingPrepWriteOpassign(indexable, runtime);
	}
	
	/**
	 * @param runtime .
	 * @param key .
	 */
	public static void opINDEX_U(RuntimeInterpreter runtime, PHPValue key) {
		PHPStack stack = runtime.getStack();
		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		stack.push(jhINDEX_U(key, indexable, runtime));
	}
	
	/**
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_U(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		return Indexable.getPrepUnset(key, indexable, runtime);
	}
	
	/**
	 * constant key variant.
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_U(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		return Indexable.getPrepUnset(key, indexable, runtime);
	}

	/**
	 * @param runtime .
	 * @param key .
	 */
	public static void opINDEX_I(RuntimeInterpreter runtime, PHPValue key) {
		PHPStack stack = runtime.getStack();
		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		stack.push(jhINDEX_I(key, indexable, runtime));
	}
	
	/**
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_I(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		return Indexable.getPrepIsset(key, indexable, runtime);
	}
	
	/**
	 * constant key variant.
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return entry
	 */
	public static PHPValue jhINDEX_I(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		return Indexable.getPrepIsset(key, indexable, runtime);
	}

	/** 
	 * @param runtime .
	 * @param doEmpty .
	 * @param key .
	 * @param stack .
	 */
	public static void opISSET_INDEX(RuntimeInterpreter runtime,
			boolean doEmpty, PHPValue key, PHPStack stack) {

		PHPValue indexable = stack.pop();
		if (key == null) {
			key = stack.pop();
		}
		if (doEmpty) {
			stack.push(jhEMPTY_INDEX(key, indexable, runtime));
		} else {
			stack.push(jhISSET_INDEX(key, indexable, runtime));
		}
		return;
	}

	/** 
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return boolean true if set
	 */
	public static PHPValue jhEMPTY_INDEX(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {

		return PHPValue.createBool(Indexable.isSet(runtime, indexable, key, true));
	}
	
	/** 
	 * constant key variant.
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return boolean true if set
	 */
	public static PHPValue jhEMPTY_INDEX(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {

		return PHPValue.createBool(Indexable.isSet(runtime, indexable, key, true));
	}
	
	/** 
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return boolean true if set
	 */
	public static PHPValue jhISSET_INDEX(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {

		return PHPValue.createBool(Indexable.isSet(runtime, indexable, key, false));
	}
	
	/** 
	 * constant key variant.
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 * @return boolean true if set
	 */
	public static PHPValue jhISSET_INDEX(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {

		return PHPValue.createBool(Indexable.isSet(runtime, indexable, key, false));
	}
	
	/**
	 * @param runtime .
	 * @param key .
	 * @param stack .
	 */
	public static void opUNSET_INDEX(RuntimeInterpreter runtime,
			PHPValue key, PHPStack stack) {

		PHPValue indexable = stack.pop();
		if (key == null) { 
			key = stack.pop();
		}
		jhUNSET_INDEX(key, indexable, runtime);
		return;
	}
	
	/**
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 */
	public static void jhUNSET_INDEX(PHPValue key, PHPValue indexable, RuntimeInterpreter runtime) {
		Indexable.unset(runtime, indexable, key);
		return;
	}

	/**
	 * constant key variant.
	 * @param indexable .
	 * @param key .
	 * @param runtime .
	 */
	public static void jhUNSET_INDEX(PHPValue indexable, RuntimeInterpreter runtime, PHPValue key) {
		Indexable.unset(runtime, indexable, key);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opSTATIC_PROPERTY(RuntimeInterpreter runtime,
			boolean opbool, NameString opstring, PHPStack stack) {

		PHPValue memberNameValue = stack.pop();
		stack.push(jhSTATIC_PROPERTY(memberNameValue, runtime, opstring, opbool));
		return;
	}
	
	/**
	 * @param memberNameValue .
	 * @param runtime .
	 * @param className .
	 * @param requireWritable .
	 * @return value
	 */
	public static PHPValue jhSTATIC_PROPERTY(PHPValue memberNameValue,
			RuntimeInterpreter runtime, NameString className, boolean requireWritable) {

		PHPClass phpClass = runtime.getClasses().getPHPClass(className);
		String memberName = memberNameValue.getJavaString();
		return ClassFacade.getStaticPropertyValue(runtime, 
			phpClass, memberName, true, requireWritable);
	}

	/** 
	 * @param stack .
	 */
	public static void opARRAY_INIT_CHECK(PHPStack stack) {
		jhARRAY_INIT_CHECK(stack.peek());
		return;
	}
	
	/** 
	 * @param val .
	 * @return val
	 */
	public static PHPValue jhARRAY_INIT_CHECK(PHPValue val) {

		// Null, boolean false and the empty string are implicitly initialised to empty array.
		if (val.getType() == PHPValue.Types.PHPTYPE_NULL
				|| (val.getType() == PHPValue.Types.PHPTYPE_BOOLEAN && !val.getBoolean())
				|| (val.getType() == PHPValue.Types.PHPTYPE_STRING && StringFacade.strlen(val) == 0)) {
			val.setArray(new PHPArray());
		}
		return val;
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opASSIGN_REF_PROPERTY(RuntimeInterpreter runtime,
			boolean opbool, String fieldName, PHPStack stack) {

		PHPValue object = stack.pop();
		PHPValue returnValue;
		if (fieldName == null) {
			PHPValue fieldValue = stack.pop();
			PHPValue value = stack.pop();
			returnValue = jhASSIGN_REF_PROPERTY(value, fieldValue, object, runtime);
		} else {
			PHPValue value = stack.pop();
			returnValue = jhASSIGN_REF_PROPERTY(value, object, runtime, fieldName);
		}
		if (opbool) {
			stack.push(returnValue);
		}
	}
	
	/**
	 * @param value .
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhASSIGN_REF_PROPERTY(PHPValue value, PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		String field = fieldValue.getJavaString();
		if (field.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
			return value;
		}
		
		ObjectFacade.assignPropertyReference(runtime, object, field, value);
		return value;
	}
	
	/**
	 * @param value .
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhASSIGN_REF_PROPERTY(PHPValue value, PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		// TODO: can this be a compile time error? - or compile to error generating code 
		if (fieldName.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
			return value;
		}
		
		ObjectFacade.assignPropertyReference(runtime, object, fieldName, value);
		return value;
	}

	/**
	 * @param runtime .
	 * @param returnValue .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_PROPERTY(RuntimeInterpreter runtime,
			boolean returnValue, String fieldName, PHPStack stack) {

		PHPValue object = stack.pop();
		if (fieldName == null) {
			PHPValue fieldValue = stack.pop();
			PHPValue value = stack.pop();
			if (returnValue) {
				stack.push(jhASSIGN_VAL_PROPERTY_RETURN(value, fieldValue, object, runtime));
			} else {
				jhASSIGN_VAL_PROPERTY(value, fieldValue, object, runtime);
			}
		} else {
			PHPValue value = stack.pop();
			if (returnValue) {
				stack.push(jhASSIGN_VAL_PROPERTY_RETURN(value, object, runtime, fieldName));
			} else {
				jhASSIGN_VAL_PROPERTY(value, object, runtime, fieldName);
			}
		}
	}
	
	/**
	 * @param value .
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return value cloned if referenced
	 */
	public static PHPValue jhASSIGN_VAL_PROPERTY_RETURN(PHPValue value, PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		String fieldName = fieldValue.getJavaString();
		return jhASSIGN_VAL_PROPERTY_RETURN(value, object, runtime, fieldName);
	}
	
	/**
	 * @param value .
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 * @return value cloned if referenced
	 */
	public static PHPValue jhASSIGN_VAL_PROPERTY_RETURN(PHPValue value, PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		if (fieldName.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
		} else {
			ObjectFacade.assignPropertyValue(runtime, object, fieldName, value);
		}
		
		return value.cloneIfReferenced();
	}
	
	/**
	 * @param value .
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_PROPERTY(PHPValue value, PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		String fieldName = fieldValue.getJavaString();
		jhASSIGN_VAL_PROPERTY(value, object, runtime, fieldName);
	}
	
	/**
	 * @param value .
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_PROPERTY(PHPValue value, PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		if (fieldName.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
		} else {
			ObjectFacade.assignPropertyValue(runtime, object, fieldName, value);
		}
	}

	// still need this ?
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_PROPERTY2(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack) {

		PHPValue value = stack.pop();
		PHPValue object = stack.pop();
		PHPValue fieldValue = stack.pop();
		
		if (opbool) {
			stack.push(jhASSIGN_VAL_PROPERTY2_RETURN(fieldValue, object, value, runtime));
		} else {
			jhASSIGN_VAL_PROPERTY2(fieldValue, object, value, runtime);
		}
		return;
	}

	/**
	 * @param fieldValue .
	 * @param object .
	 * @param value .
	 * @param runtime .
	 * @return value cloned if referenced
	 */
	public static PHPValue jhASSIGN_VAL_PROPERTY2_RETURN(PHPValue fieldValue, PHPValue object, PHPValue value, RuntimeInterpreter runtime) {

		String field = fieldValue.getJavaString();
		if (field.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
		} else {
			ObjectFacade.assignPropertyValue(runtime, object, field, value);
		}
		
		return value.cloneIfReferenced();
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param value .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_PROPERTY2(PHPValue fieldValue, PHPValue object, PHPValue value, RuntimeInterpreter runtime) {

		String field = fieldValue.getJavaString();
		if (field.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
		} else {
			ObjectFacade.assignPropertyValue(runtime, object, field, value);
		}
	}
	/** 
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_PROP_INDEXED(RuntimeInterpreter runtime, boolean opbool, PHPStack stack) {
		
		PHPValue val = stack.pop();
		PHPValue indexable = stack.pop();
		PHPValue key = stack.pop();

		if (opbool) {
			stack.push(jhASSIGN_VAL_PROP_INDEXED_RETURN(key, indexable, val, runtime));
		} else {
			jhASSIGN_VAL_PROP_INDEXED(key, indexable, val, runtime);
		}
		return;
	}
	
	/**
	 * @param key .
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 * @return value cloned if referenced
	 */
	public static PHPValue jhASSIGN_VAL_PROP_INDEXED_RETURN(PHPValue key, PHPValue indexable, PHPValue value,
			RuntimeInterpreter runtime) {
		
		if (indexable.getType() == PHPValue.Types.PHPTYPE_OBJECT) {
			Indexable.assignValue(runtime, indexable, key, value);			
		}
		return value.cloneIfReferenced();
	}
	
	/**
	 * @param key .
	 * @param indexable .
	 * @param value .
	 * @param runtime .
	 */
	public static void jhASSIGN_VAL_PROP_INDEXED(PHPValue key, PHPValue indexable, PHPValue value,
			RuntimeInterpreter runtime) {
		
		if (indexable.getType() == PHPValue.Types.PHPTYPE_OBJECT) {
			Indexable.assignValue(runtime, indexable, key, value);			
		}
		return;
	}
	
	/**
	 * @param runtime .
	 * @param doEmpty .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opISSET_PROPERTY(RuntimeInterpreter runtime,
			boolean doEmpty, String fieldName, PHPStack stack) {

		PHPValue object = stack.pop();
		
		if (doEmpty) {
			if (fieldName == null) {
				PHPValue fieldValue = stack.pop();
				stack.push(jhEMPTY_PROPERTY(fieldValue, object, runtime));
			} else {
				stack.push(jhEMPTY_PROPERTY(object, runtime, fieldName));
			}
		} else {
			if (fieldName == null) {
				PHPValue fieldValue = stack.pop();
				stack.push(jhISSET_PROPERTY(fieldValue, object, runtime));
			} else {
				stack.push(jhISSET_PROPERTY(object, runtime, fieldName));
			}
		}
		return;
	}

	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return PHPValue true if set
	 * 
	 * TODO: optimisation: return boolean
	 */
	public static PHPValue jhISSET_PROPERTY(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		String fieldName = fieldValue.getJavaString();
		return jhISSET_PROPERTY(object, runtime, fieldName);
	}
	
	/**
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 * @return PHPValue true if set
	 * 
	 * TODO: optimisations: return boolean
	 */
	public static PHPValue jhISSET_PROPERTY(PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		if (object.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			return PHPValue.createBool(false);
		}
		return PHPValue.createBool(ObjectFacade.isPropertySet(runtime, object, fieldName, CheckType.NonNull));
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return PHPValue true if empty 
	 * 
	 * TODO: optimisations: return boolean
	 */
	public static PHPValue jhEMPTY_PROPERTY(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		String fieldName = fieldValue.getJavaString();
		return jhEMPTY_PROPERTY(object, runtime, fieldName);
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @param fieldName .
	 * @return PHPValue true if empty 
	 * 
	 * TODO: optimisation: return boolean
	 */
	public static PHPValue jhEMPTY_PROPERTY(PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		if (object.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			return PHPValue.createBool(true);
		}
		return PHPValue.createBool(!ObjectFacade.isPropertySet(runtime, object, fieldName, CheckType.EnsureTrue));
	}
	
	/**
	 * @param runtime .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opUNSET_PROPERTY(RuntimeInterpreter runtime, String fieldName, 
			PHPStack stack) {

		PHPValue object = stack.pop();
		if (fieldName == null) {
			PHPValue fieldValue = stack.pop();
			jhUNSET_PROPERTY(fieldValue, object, runtime);
		} else {
			jhUNSET_PROPERTY(object, runtime, fieldName);
		}
		return;
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 */
	public static void jhUNSET_PROPERTY(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		String field = fieldValue.getJavaString();
		ObjectFacade.unsetProperty(runtime, object, field);
		return;
	}

	/**
	 * @param object .
	 * @param runtime .
	 * @param fieldName .
	 */
	public static void jhUNSET_PROPERTY(PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		ObjectFacade.unsetProperty(runtime, object, fieldName);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param field . 
	 * @param stack .
	 */
	public static void opPROPERTY_R(RuntimeInterpreter runtime,
			String field, PHPStack stack) {

		PHPValue object = stack.pop();
		if (field == null) {
			PHPValue fieldValue = stack.pop();
			stack.push(jhPROPERTY_R(fieldValue, object, runtime));
		} else {
			stack.push(jhPROPERTY_R(object, runtime, field));
		}
		return;
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime . 
	 * @return property 
	 */
	public static PHPValue jhPROPERTY_R(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		return ObjectFacade.getPropertyValue(runtime, object, fieldValue.getJavaString(), false, true, false);
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @param field . 
	 * @return property 
	 */
	public static PHPValue jhPROPERTY_R(PHPValue object, RuntimeInterpreter runtime,
			String field) {

		return ObjectFacade.getPropertyValue(runtime, object, field, false, true, false);
	}


	/** 
	 * @param runtime .
	 * @param field .
	 * @param stack .
	 */
	public static void opPROPERTY_FE(RuntimeInterpreter runtime, String field, PHPStack stack) {
		PHPValue object = stack.pop();
		if (field == null) {
			PHPValue fieldValue = stack.pop();
			stack.push(jhPROPERTY_FE(fieldValue, object, runtime));
		} else {
			stack.push(jhPROPERTY_FE(object, runtime, field));
		}
		return;
	}
	
	/** 
	 * @param object .
	 * @param runtime .
	 * @param field .
	 * @return property 
	 */
	public static PHPValue jhPROPERTY_FE(PHPValue object, RuntimeInterpreter runtime, String field) {
		return ObjectFacade.getPropertyValue(runtime, object, field, false, true, true);
	}
	
	/** 
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return property 
	 */
	public static PHPValue jhPROPERTY_FE(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {
		return ObjectFacade.getPropertyValue(runtime, object, fieldValue.getJavaString(), false, true, true);
	}
	
	/**
	 * @param runtime .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opPROPERTY_I(RuntimeInterpreter runtime, String fieldName, 
			PHPStack stack) {

		PHPValue object = stack.pop();
		if (fieldName == null) {
			PHPValue fieldValue = stack.pop();
			stack.push(jhPROPERTY_I(fieldValue, object, runtime));
		} else {
			stack.push(jhPROPERTY_I(object, runtime, fieldName));
		}
		return;
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return property
	 */
	public static PHPValue jhPROPERTY_I(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {

		return ObjectFacade.getPropertyValue(runtime, object, fieldValue.getJavaString(), false, false,
				false);
	}
	
	/**
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 * @return property
	 */
	public static PHPValue jhPROPERTY_I(PHPValue object, RuntimeInterpreter runtime, String fieldName) {

		return ObjectFacade.getPropertyValue(runtime, object, fieldName, false, false,
				false);
	}

	/**
	 * @param runtime .
	 * @param stack .
	 * @param fieldName .
	 */
	public static void opPROPERTY_U(RuntimeInterpreter runtime, String fieldName,
			PHPStack stack) {

		PHPValue object = stack.pop();
		if (fieldName == null) {
			PHPValue fieldValue = stack.pop();
			stack.push(jhPROPERTY_U(fieldValue, object, runtime));
		} else {
			stack.push(jhPROPERTY_U(object, runtime, fieldName));
		}
		return;
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @return property PHPValue
	 */
	public static PHPValue jhPROPERTY_U(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime) {
		return ObjectFacade.getPropertyValue(runtime, object, fieldValue.getJavaString(), false, true, true);
	}
	
	/**
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 * @return property PHPValue
	 */
	public static PHPValue jhPROPERTY_U(PHPValue object, RuntimeInterpreter runtime, String fieldName) {
		return ObjectFacade.getPropertyValue(runtime, object, fieldName, false, true, true);
	}

	/**
	 * @param fieldName .
	 * @param runtime .
	 * @param warnOnInstantiation .
	 * @param stack .
	 */
	public static void opPROPERTY_RW(RuntimeInterpreter runtime, String fieldName, 
			boolean warnOnInstantiation, PHPStack stack) {

		PHPValue object = stack.pop();
		if (fieldName == null) {
			PHPValue fieldValue = stack.pop();
			stack.push(jhPROPERTY_RW(fieldValue, object, runtime, warnOnInstantiation));
		} else {
			stack.push(jhPROPERTY_RW(object, runtime, fieldName, warnOnInstantiation));
		}
		return;
	}
	
	/**
	 * @param fieldValue .
	 * @param object .
	 * @param runtime .
	 * @param warnOnInstantiation .
	 * @return property value
	 */
	public static PHPValue jhPROPERTY_RW(PHPValue fieldValue, PHPValue object, RuntimeInterpreter runtime,
			boolean warnOnInstantiation) {

		String field = fieldValue.getJavaString();
		if (field.equals("")) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.PropertyNameEmpty", null);
			return null;
		}
		return jhPROPERTY_RW(object, runtime, field, warnOnInstantiation);
	}
	
	/**
	 * @param fieldName .
	 * @param object .
	 * @param runtime .
	 * @param warnOnInstantiation .
	 * @return property value
	 */
	public static PHPValue jhPROPERTY_RW(PHPValue object, RuntimeInterpreter runtime,
			String fieldName, boolean warnOnInstantiation) {
		
		PHPValue result = propertyReadPreparingWrite(runtime, object, fieldName, warnOnInstantiation);
		
		if (object.getType() == PHPValue.Types.PHPTYPE_OBJECT 
				&& result.getType() != PHPValue.Types.PHPTYPE_OBJECT
				&& !(result.isReferenced() && result.isWritable())) {
			// Getter must have returned a temporary or non writable value. 
			Object[] inserts = { ObjectFacade.getPHPClass(object).getName(), fieldName };
			runtime.raiseExecError(PHPErrorHandler.E_NOTICE, null, 
					"Object.PropertyModificationHasNoEffect", inserts);
		}
		
		return result;
	}

	/**
	 * @param runtime .
	 * @param warnOnInstanciation .
	 * @param opinteger .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opPROPERTY_RW_INCDEC(RuntimeInterpreter runtime,
			String fieldName, boolean warnOnInstanciation, int opinteger, PHPStack stack) {

		PHPValue object = stack.pop();
		PHPValue result;
		PHPValue field;
		if (fieldName == null) {
			field = stack.pop();
			result = jhPROPERTY_RW_INPLACE(field, object, runtime, warnOnInstanciation);
		} else {
			field = PHPValue.createString(fieldName); // undo optimisation for interpreter only
			result = jhPROPERTY_RW_INPLACE(object, runtime, fieldName, warnOnInstanciation);
		}
		if (opinteger != 0) {
			stack.push(jhCLONE(result));
		}
		stack.push(field);
		stack.push(object);
		stack.push(result);
		return;
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opSTATIC_PROPERTY_RW_INCDEC(RuntimeInterpreter runtime,
			boolean opbool, NameString opstring, PHPStack stack) {

		PHPValue memberNameValue = stack.pop();
		PHPValue result = jhSTATIC_PROPERTY_RW_INCDEC(memberNameValue, runtime, opstring, opbool);		
		if (opbool) {
			stack.push(jhCLONE(result));
		}
		stack.push(memberNameValue);
		stack.push(result);
		return;
	}
	
	/**
	 * @param memberNameValue .
	 * @param runtime .
	 * @param className .
	 * @param requireWritable .
	 * @return value
	 */
	public static PHPValue jhSTATIC_PROPERTY_RW_INCDEC(PHPValue memberNameValue,
			RuntimeInterpreter runtime, NameString className, boolean requireWritable) {

		PHPClass phpClass = runtime.getClasses().getPHPClass(className);
		String memberName = memberNameValue.getJavaString();
		return ClassFacade.getStaticPropertyValue(runtime, 
			phpClass, memberName, true, requireWritable);
	}

	/**
	 * @param runtime .
	 * @param warnOnInstanciation .
	 * @param fieldName .
	 * @param stack .
	 */
	public static void opPROPERTY_RW_OPASSIGN(RuntimeInterpreter runtime,
			boolean warnOnInstanciation, String fieldName, PHPStack stack) {

		PHPValue object = stack.pop();
		PHPValue result;
		PHPValue rhs;
		if (fieldName == null) {
			PHPValue field = stack.pop();
			rhs = stack.pop();
			result = jhPROPERTY_RW_INPLACE(field, object, runtime, warnOnInstanciation);
			stack.push(field);
		} else {
			rhs = stack.pop();
			result = jhPROPERTY_RW_INPLACE(object, runtime, fieldName, warnOnInstanciation);
			stack.push(PHPValue.createString(fieldName)); // undo our good work unboxing, rare case
		}
		stack.push(object);
		stack.push(result);
		stack.push(rhs);
		return;
	}
	
	/**
	 * @param field .
	 * @param object .
	 * @param runtime .
	 * @param warnOnInstanciation .
	 * @return result
	 */
	public static PHPValue jhPROPERTY_RW_INPLACE(PHPValue field, PHPValue object, RuntimeInterpreter runtime,
			boolean warnOnInstanciation) {

		return jhPROPERTY_RW_INPLACE(object, runtime, field.getJavaString(), warnOnInstanciation);
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @param warnOnInstanciation .
	 * @param fieldName .
	 * @return result
	 */
	public static PHPValue jhPROPERTY_RW_INPLACE(PHPValue object, RuntimeInterpreter runtime,
			String fieldName, boolean warnOnInstanciation) {

		return propertyReadPreparingWrite(runtime, object, fieldName, warnOnInstanciation);
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 */
	public static void opLOAD_STATIC(RuntimeInterpreter runtime,
			String opstring) {

		StaticVariableScope scope = runtime.getStackFrame()
				.getStaticVariableScope();
		scope.loadStaticVariable(runtime, opstring);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * not tested 
	 */
	public static void jhLOAD_STATIC(RuntimeInterpreter runtime,
			String opstring) {

		StaticVariableScope scope = runtime.getStackFrame()
				.getStaticVariableScope();
		scope.loadStaticVariable(runtime, opstring);
		return;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param opBool .
	 * @param stack .
	 */
	public static void opASSIGN_REF_STATIC_PROPERTY(
			RuntimeInterpreter runtime, NameString opstring, boolean opBool, PHPStack stack) {

		PHPValue memberNameValue = stack.pop();
		PHPValue value = stack.pop();
		if (opBool) {
			stack.push(jhASSIGN_REF_STATIC_PROPERTY(value, memberNameValue, runtime, opstring));
		} else {
			jhASSIGN_REF_STATIC_PROPERTY(value, memberNameValue, runtime, opstring);
		}
		return;
	}
	
	/**
	 * @param value .
	 * @param memberNameValue .
	 * @param runtime .
	 * @param opstring .
	 * @return value
	 */
	public static PHPValue jhASSIGN_REF_STATIC_PROPERTY(PHPValue value, PHPValue memberNameValue,
			RuntimeInterpreter runtime, NameString opstring) {

		String memberName = memberNameValue.getJavaString();
		PHPClass phpClass = runtime.getClasses().getPHPClass(opstring);
		phpClass.assignStaticPropertyReference(runtime, memberName, value);
		return value;
	}

	/**
	 * @param runtime .
	 * @param className .
	 * @param opBool .
	 * @param stack .
	 */
	public static void opASSIGN_VAL_STATIC_PROPERTY(
			RuntimeInterpreter runtime, NameString className, boolean opBool, PHPStack stack) {

		PHPValue memberNameValue = stack.pop();
		PHPValue val = stack.pop();
		if (opBool) {
			stack.push(jhASSIGN_VAL_STATIC_PROPERTY_RETURN(val, memberNameValue, runtime, className));
		} else {
			jhASSIGN_VAL_STATIC_PROPERTY(val, memberNameValue, runtime, className);
		}
		return;
	}

	/**
	 * @param val .
	 * @param memberNameValue .
	 * @param runtime .
	 * @param className .
	 */
	public static void jhASSIGN_VAL_STATIC_PROPERTY(PHPValue val, PHPValue memberNameValue,
			RuntimeInterpreter runtime, NameString className) {

		PHPClass phpClass = runtime.getClasses().getPHPClass(className);
		ClassFacade.assignStaticPropertyValue(runtime, phpClass, memberNameValue.getJavaString(), val);
	}
	
	/**
	 * @param val .
	 * @param memberNameValue .
	 * @param runtime .
	 * @param className .
	 * @return value cloned if referenced
	 */
	public static PHPValue jhASSIGN_VAL_STATIC_PROPERTY_RETURN(PHPValue val, PHPValue memberNameValue, 
			RuntimeInterpreter runtime, NameString className) {

		PHPClass phpClass = runtime.getClasses().getPHPClass(className);
		ClassFacade.assignStaticPropertyValue(runtime, phpClass, memberNameValue.getJavaString(), val);
		return val.cloneIfReferenced();
	}
	
	/**
	 * @param runtime .
	 * @param stack .
	 * @param classes .
	 */
	public static void opOBJECT_INIT_CHECK(RuntimeInterpreter runtime,
			PHPStack stack, Classes classes) {

		PHPValue object = stack.peek();
		
		// Null, boolean false and the empty string are implicitly initialised to instance of stdclass.
		if (object.getType() == PHPValue.Types.PHPTYPE_NULL
				|| (object.getType() == PHPValue.Types.PHPTYPE_BOOLEAN && !object.getBoolean())
				|| (object.getType() == PHPValue.Types.PHPTYPE_STRING && StringFacade.strlen(object) == 0)) {
			runtime.raiseExecError(PHPErrorHandler.E_STRICT, null, "Object.ImplicitInstantiation", null);
			// create new instance in the PHPValue
			object.setObject(classes.getPHPClass(PHPStdClass.CLASS_NAMESTRING));
		}
		
		return;
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @return object
	 */
	public static PHPValue jhOBJECT_INIT_CHECK(PHPValue object, RuntimeInterpreter runtime) {
		
		// Null, boolean false and the empty string are implicitly initialised to instance of stdclass.
		if (object.getType() == PHPValue.Types.PHPTYPE_NULL
				|| (object.getType() == PHPValue.Types.PHPTYPE_BOOLEAN && !object.getBoolean())
				|| (object.getType() == PHPValue.Types.PHPTYPE_STRING && StringFacade.strlen(object) == 0)) {
			runtime.raiseExecError(PHPErrorHandler.E_STRICT, null, "Object.ImplicitInstantiation", null);
			// create new instance in the PHPValue
			object.setObject(runtime.getClasses().getPHPClass(PHPStdClass.CLASS_NAMESTRING));
		}
		
		return object;
	}

	/**
	 * @param runtime .
	 * @param stack .
	 * @param classes .
	 */
	public static void opOBJECT_INIT_SPECIALCHECK(RuntimeInterpreter runtime,
			PHPStack stack, Classes classes) {

		PHPValue object = stack.peek();
		
		// Null, boolean false and the empty string are implicitly initialised to instance of stdclass.
		if (object.getType() == PHPValue.Types.PHPTYPE_NULL) {
			runtime.raiseExecError(PHPErrorHandler.E_STRICT, null, "Object.ImplicitInstantiation", null);
		}
		return;
	}
	
	/**
	 * @param object .
	 * @param runtime .
	 * @return object .
	 */
	public static PHPValue jhOBJECT_INIT_SPECIALCHECK(PHPValue object, RuntimeInterpreter runtime) {

		// Null, boolean false and the empty string are implicitly initialised to instance of stdclass.
		if (object.getType() == PHPValue.Types.PHPTYPE_NULL) {
			runtime.raiseExecError(PHPErrorHandler.E_STRICT, null, "Object.ImplicitInstantiation", null);
		}
		return object;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opUNSET_STATIC_PROPERTY_ERROR(
			RuntimeInterpreter runtime, NameString opstring, PHPStack stack) {

		jhUNSET_STATIC_PROPERTY_ERROR(stack.pop(), runtime, opstring);
		return;
	}
	
	/**
	 * @param memberNameValue .
	 * @param runtime .
	 * @param opstring .
	 */
	public static void jhUNSET_STATIC_PROPERTY_ERROR(PHPValue memberNameValue,
			RuntimeInterpreter runtime, NameString opstring) {

		Object[] inserts = { opstring, memberNameValue.getJavaString() };
		runtime.raiseExecError(ErrorType.E_ERROR, null, "Class.UnsetOnStatic",
				inserts);
		return;
	}

	/**
	 * @param runtime .
	 * @param opstring .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opISSET_STATIC_PROPERTY(RuntimeInterpreter runtime,
			NameString opstring, boolean opbool, PHPStack stack) {

		PHPValue memberNameValue = stack.pop();
		if (opbool) {
			stack.push(jhEMPTY_STATIC_PROPERTY(memberNameValue, runtime, opstring));
		} else {
			stack.push(jhISSET_STATIC_PROPERTY(memberNameValue, runtime, opstring));
		}
		return;
	}
	
	/**
	 * @param memberNameValue .
	 * @param runtime .
	 * @param opstring .
	 * @return boolean PHPValue 
	 */
	public static PHPValue jhEMPTY_STATIC_PROPERTY(PHPValue memberNameValue, RuntimeInterpreter runtime,
			NameString opstring) {

		String memberName = memberNameValue.getJavaString();
		PHPClass phpClass = runtime.getClasses().getPHPClass(opstring);
		PHPValue val = ClassFacade.getStaticPropertyValue(runtime, 
			phpClass, memberName, false, false);
		
		return PHPValue.createBool(!val.getBoolean());
	}
	
	/**
	 * @param memberNameValue .
	 * @param runtime .
	 * @param opstring .
	 * @return boolean PHPValue 
	 */
	public static PHPValue jhISSET_STATIC_PROPERTY(PHPValue memberNameValue, RuntimeInterpreter runtime,
			NameString opstring) {

		String memberName = memberNameValue.getJavaString();
		PHPClass phpClass = runtime.getClasses().getPHPClass(opstring);
		return PHPValue.createBool(ClassFacade.isStaticPropertySet(runtime, phpClass, memberName, CheckType.NonNull));
	}

	/** 
	 * @param runtime .
	 * @param className .
	 * @param stack .
	 */
	public static void opCLASS_CONSTANT(RuntimeInterpreter runtime,
			NameString className, PHPStack stack) {

		stack.push(jhCLASS_CONSTANT(stack.pop(), runtime, className));
		return;
	}
	
	/**
	 * @param constNameValue .
	 * @param runtime .
	 * @param className .
	 * @return class constant PHPValue
	 */
	public static PHPValue jhCLASS_CONSTANT(PHPValue constNameValue, RuntimeInterpreter runtime, NameString className) {

		String constName = constNameValue.getJavaString();
		PHPClass phpClass = runtime.getClasses().getPHPClass(className);
		return phpClass.getConstantValue(runtime, constName);
	}

	/**
	 * @param runtime .
	 * @param varName .
	 * @param stack .
	 */
	public static void opMAKE_GLOBAL(RuntimeInterpreter runtime,
			String varName, PHPStack stack) {

		if (varName == null) {
			jhMAKE_GLOBAL(stack.pop(), runtime);
		} else {
			jhMAKE_GLOBAL(runtime, varName);
		}
	}

	/**
	 * @param varNameValue .
	 * @param runtime .
	 */
	public static void jhMAKE_GLOBAL(PHPValue varNameValue, RuntimeInterpreter runtime) {
		// Make local variable $varName refer to global variable of the same name.

		String varName = jhINTERNED_STRING(varNameValue);
		
		PHPValue globalValue = runtime.getGlobals().getWritable(varName);
		runtime.getLocals().assignRef(varName, globalValue);
		return;
	}
	
	/**
	 * @param varName .
	 * @param runtime .
	 */
	public static void jhMAKE_GLOBAL(RuntimeInterpreter runtime, String varName) {
		// Make local variable $varName refer to global variable of the same name.
		
		PHPValue globalValue = runtime.getGlobals().getWritable(varName);
		runtime.getLocals().assignRef(varName, globalValue);
		return;
	}
	
	/**
	 * Called by L2.
	 * @param varName .
	 * @param runtime .
	 * @return global value
	 */
	public static PHPValue jhGET_GLOBAL(RuntimeInterpreter runtime, String varName) {
		// Make local variable $varName refer to global variable of the same name.
		PHPValue globalValue = runtime.getGlobals().getWritable(varName);
		return globalValue;
	}

	
	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param stack .
	 */
	public static void opINCLUDE(RuntimeInterpreter runtime, int opinteger,
			PHPStack stack) {
		stack.push(jhINCLUDE(stack.pop(), runtime, opinteger));
	}
	

	/**
	 * @param fileValue . 
	 * @param runtime .
	 * @param includeType .
	 * @return result .
	 */
	public static PHPValue jhINCLUDE(PHPValue fileValue, RuntimeInterpreter runtime, int includeType) {
		return jhINCLUDE(fileValue.getJavaString(), runtime, includeType);
	}
		
	/**
	 * @param fileName .
	 * @param runtime .
	 * @param includeType .
	 * @return result .
	 */
	public static PHPValue jhINCLUDE(String fileName, RuntimeInterpreter runtime, int includeType) {
		
		// do mapping of opinteger to StackFrame.IncludeOrEvalType
		StackFrameType frameType = IncludeEval.mapIntToStackFrameType(includeType);
		StackFrame specialStackFrame = new StackFrameIncludeImpl(runtime, frameType);
		runtime.setNewStackFrame(specialStackFrame);
		try {
			PHPValue val = IncludeEval.includeFile(runtime, fileName, includeType);
			return val;
		} finally {
			runtime.collapseStackFrame();
		}
	}

	/**
	 * @param runtime .
	 * @param fileName .
	 * @param stack .
	 */
	public static void opEVAL(RuntimeInterpreter runtime, String fileName, PHPStack stack) {	
		PHPValue val = stack.pop();
		stack.push(jhEVAL(val, runtime, fileName));
		return;
	}

	/**
	 * @param val .
	 * @param runtime .
	 * @param fileName .
	 * @return PHPValue result
	 */
	public static PHPValue jhEVAL(PHPValue val, RuntimeInterpreter runtime, String fileName) {
		return IncludeEval.evaluate(runtime, val, fileName);
	}
	
	/**
	 * @param runtime .
	 * @param opphpvalue .
	 * @param wantWritable .
	 * @param stack .
	 */
	public static void opINDEX_ENCAPS(RuntimeInterpreter runtime,
			PHPValue opphpvalue, boolean wantWritable, PHPStack stack) {

		// special case for encaps. Should always be a read.
		PHPValue arrayKey = opphpvalue;
		if (arrayKey == null) {
			arrayKey = stack.pop();
		}
		PHPValue array = stack.pop();
		if (wantWritable) {
			stack.push(jhINDEX_ENCAPS_W(array, arrayKey, runtime));
		} else {
			stack.push(jhINDEX_ENCAPS(array, arrayKey, runtime));
		}
		return;
	}
	
	/**
	 * @param array .
	 * @param key .
	 * @param runtime .
	 * @return array entry
	 */
	public static PHPValue jhINDEX_ENCAPS_W(PHPValue array, PHPValue key, RuntimeInterpreter runtime) {
		return ArrayFacade.get(runtime, array, key, true, true, false);
	}
	
	/**
	 * @param array .
	 * @param key .
	 * @param runtime .
	 * @return array entry
	 */
	public static PHPValue jhINDEX_ENCAPS(PHPValue array, PHPValue key, RuntimeInterpreter runtime) {

		return ArrayFacade.get(runtime, array, key, false, false, true);
	}

	/** 
	 * @param runtime .
	 * @param stack .
	 */
	public static void opCLASS_CLONE(RuntimeInterpreter runtime,
			PHPStack stack) {

		PHPValue objectToClone = stack.pop();
		stack.push(jhCLASS_CLONE(objectToClone, runtime));
		return;
	}
	
	/**
	 * @param objectToClone .
	 * @param runtime .
	 * @return cloned object
	 */
	public static PHPValue jhCLASS_CLONE(PHPValue objectToClone, RuntimeInterpreter runtime) {

		if (objectToClone.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Object.CloneOnNonObject", null);
			return objectToClone;
		}

		// The clone object handler is responsible for invoking the __clone()
		// magic method.
		// However, its visibility is checked here, in the interpreter.
		// This way, even if an object's clone handler does not invoke
		// __clone(),
		// the clone operation will be blocked if __clone() is not visible.
		PHPClass classOfClonedObject = ObjectFacade.getPHPClass(objectToClone);
		if (classOfClonedObject.hasMagicMethod(MagicMethodInfo.CLONE)) {
			PHPMethod cloneMagicMethod = classOfClonedObject
					.getMagicMethod(MagicMethodInfo.CLONE);
			if (!cloneMagicMethod.isVisible(runtime)) {
				Object[] inserts = {
						cloneMagicMethod.getVisibility().toString(),
						classOfClonedObject.getName(),
						runtime.getActiveClass()[0] };
				runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
						"Object.invisibleClone", inserts);
			}
		}

		// TODO:check for existence of cloneObject handler. Throw error if it's
		// not available.
		PHPValue clonedObject = ObjectFacade
				.cloneObject(runtime, objectToClone);
		return clonedObject;
	}

	/**
	 * @param runtime .
	 * @param opinteger .
	 * @param stack .
	 * @param classes .
	 * @param invocableStack .
	 * @param pc .
	 * @return new pc
	 */
	public static int opCLASS_NEW(RuntimeInterpreter runtime, int opinteger,
			PHPStack stack, Classes classes, InvocableStack invocableStack,
			int pc) {

		PHPValue classNameValue = stack.pop();
		
		PHPValue instance = jhCLASS_NEW(classNameValue, runtime);
		stack.push(instance);

		InvocableStackEntry ctorEntry = jhFIND_CTOR(instance, runtime);
		
		if (ctorEntry == null) { 
			pc += opinteger; // branch forward over constructor parameters and invoke
		} else {
			// constructor found - prepare to stack args and call
			invocableStack.push(ctorEntry);
		}
		return pc;
	}
	

	
	/**
	 * @param instance .
	 * @param runtime .
	 * @return InvocableStackEntry
	 */
	public static InvocableStackEntry jhFIND_CTOR(PHPValue instance, RuntimeInterpreter runtime) {
		// Instance created now find a constructor.
		// Retrieve constructor through object handler.
		PHPMethod ctor = ObjectFacade.getConstructor(runtime, instance);
		if (ctor == null) {
			return null;
		} 
		return new InvocableStackEntry(instance, ctor, null, ImplicitCallType.NotImplicit);
	}
	
	/** 
	 * @param runtime .
	 * @param classNameValue .
	 * @return new class 
	 */
	public static PHPValue jhCLASS_NEW(PHPValue classNameValue, RuntimeInterpreter runtime) {
		PHPClass phpclass = null;
		switch (classNameValue.getType()) {
		case PHPTYPE_OBJECT:
			phpclass = ObjectFacade.getPHPClass(classNameValue);
			break;
		case PHPTYPE_STRING:
			// className must be a j.l.String so assume rhs can be retrieved as
			// a j.l.String.
			NameString className = new NameString(classNameValue.getJavaString());
			phpclass = runtime.getClasses().getPHPClass(className);
			break;
		default:
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null,
					"Class.NotObjectOrString", null);
			return null;
		}

		PHPValue instance = PHPValue.createObject(phpclass);
		assert ObjectFacade.getPHPClass(instance) == phpclass;
		instance.incReferences();
		assert instance.getReferences() == 1 : "Freshly newed objects should have refcount 1 so that they can be passed/assigned by reference without warning.";
		return instance;
	}


	/**
	 * @param runtime the runtime
	 * @param stack the stack
	 */
	public static void opPREP_NEW_BY_REF(RuntimeInterpreter runtime, PHPStack stack) {
		PHPValue instance = stack.pop();
		instance = jhPREP_NEW_BY_REF(instance);
		stack.push(instance);
	}
	
	/**
	 * @param instance the new instance
	 * @return the new instance, prepared for reference assignment. 
	 */
	public static PHPValue jhPREP_NEW_BY_REF(PHPValue instance) {
		// When assigning new by reference (i.e. when using the deprecated $a =& new C; construct),
		// the instance must be split from any CoW references created in the ctor.
		if (instance.isReferenced() && !instance.isRef()) { 
			instance = instance.clone();
		}
		if (instance.getReferences() == 0) {
			instance.incReferences();
		}
		return instance;
	}

	/**
	 * @param runtime .
	 * @param className .
	 * @param stack .
	 * @param classes .
	 * @param invocableStack .
	 */
	public static void opFIND_STATIC_METHOD(RuntimeInterpreter runtime,
			NameString className, PHPStack stack, Classes classes,
			InvocableStack invocableStack) {

		// TODO: option of 2nd string parameter String methodName = opstring;
		
		invocableStack.push(jhFIND_STATIC_METHOD(stack.pop(), runtime, className));
		return;
	}
	
	/**
	 * @param className .
	 * @param methodNameValue .
	 * @param runtime .
	 * @return invocableStackEntry 
	 */
	public static InvocableStackEntry jhFIND_STATIC_METHOD(PHPValue methodNameValue, RuntimeInterpreter runtime, NameString className) {
		return jhFIND_STATIC_METHOD(runtime, jhFUNCTION_NAMESTRING(methodNameValue, runtime), className);
	}
	
	/**
	 * @param className .
	 * @param methodName .
	 * @param runtime .
	 * @return invocableStackEntry 
	 */
	public static InvocableStackEntry jhFIND_STATIC_METHOD(RuntimeInterpreter runtime, NameString methodName,  NameString className) {

		PHPClass phpClass = runtime.getClasses().getPHPClass(className);

		if (methodName.sameAs(MagicMethodInfo.CLONE.getName())) {
			// TODO: direct calls to __clone() should trigger a fatal at parse or compile time.
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null, "Class.DirectCallToClone", null);
		}

		// Obtain method to invoke using class handler. Might return __call or __callStatic if
		// the method does not exist or is not visible.
		PHPMethodAndCallType methodAndCallType = ClassFacade.getStaticMethod(runtime, phpClass, methodName, true);
		if (methodAndCallType == null) {
			Object[] inserts = { phpClass.getName(), methodName };
			runtime.raiseExecError(PHPErrorHandler.E_ERROR, null, "Class.UndefinedMethod", inserts);
			return null;
		}

		return new InvocableStackEntry(null, methodAndCallType.getMethod(),
				methodName, methodAndCallType.getImplicitCallType());
	}

	/**
	 * @param runtime .
	 * @param noArgs .
	 * @param invocableStack .
	 * @param isReturnValueRedundant .
	 * called by opcode
	 */
	public static void opINVOKE_STATIC_METHOD(RuntimeInterpreter runtime,
			int noArgs, InvocableStack invocableStack, boolean isReturnValueRedundant) {
		innerINVOKE_STATIC_METHOD(runtime, noArgs, invocableStack.pop(), isReturnValueRedundant);
		return;
	}
	

	/**
	 * @param args method arguments.
	 * @param methodEntry .
	 * @param runtime .
	 * @param isReturnValueRedundant .
	 * @return return value from method.
	 */
	public static PHPValue jhINVOKE_STATIC_METHOD(PHPValue[] args, InvocableStackEntry methodEntry, RuntimeInterpreter runtime, boolean isReturnValueRedundant) {
		if (methodEntry.getImplicitCallType() == ImplicitCallType.__Call) {
			return methodEntry.getMethod().invokeImplicit__call(runtime, methodEntry.getMethodName(), null, isReturnValueRedundant, args);
		} else if (methodEntry.getImplicitCallType() == ImplicitCallType.__CallStatic) {
			return methodEntry.getMethod().invokeImplicit__callStatic(runtime, methodEntry.getMethodName(), isReturnValueRedundant, args);
		} else {
			return methodEntry.getMethod().invokeStatically(runtime, isReturnValueRedundant, args);			
		}
	}
	
	/**
	 * @param methodEntry .
	 * @param noArgs .
	 * @param isReturnValueRedundant .
	 * @param runtime .
	 */
	public static void innerINVOKE_STATIC_METHOD(RuntimeInterpreter runtime, int noArgs, InvocableStackEntry methodEntry, boolean isReturnValueRedundant) {
		// interesting args are on the stack so adapt
		PHPValue[] args = null;
		if (noArgs > 0) {
			args = new PHPValue[noArgs];
			for (int index = noArgs - 1; index >= 0; index--) {
				args[index] = runtime.getStack().pop();
			}
		}
		PHPValue returnVal = null;
		if (methodEntry.getImplicitCallType() == ImplicitCallType.NotImplicit) {
			returnVal = methodEntry.getMethod().invokeStatically(runtime, isReturnValueRedundant, args);
		} else if (methodEntry.getImplicitCallType() == ImplicitCallType.__Call) {
			returnVal = methodEntry.getMethod().invokeImplicit__call(runtime, methodEntry.getMethodName(), null, isReturnValueRedundant, args);			
		} else if (methodEntry.getImplicitCallType() == ImplicitCallType.__CallStatic) {
			returnVal = methodEntry.getMethod().invokeImplicit__callStatic(runtime, methodEntry.getMethodName(), isReturnValueRedundant, args);			
		}
		
		runtime.getStack().push(returnVal);
		return;
	}
	
	/**
	 * @param runtime .
	 * @param opstring .
	 * @param stack .
	 */
	public static void opCONSTANT(RuntimeInterpreter runtime,
			String opstring, PHPStack stack) {

		stack.push(jhCONSTANT(runtime, opstring));
		return;
	}
	
	/**
	 * @param runtime .
	 * @param name .
	 * @return value .
	 */
	public static PHPValue jhCONSTANT(RuntimeInterpreter runtime, String name) {

		VarMapConstants constants = runtime.getConstants();
		if (constants.isVariable(name)) {
			// TODO double index to eliminate
			// note: this is *not* equivalent to get(name) and return if not null
			return constants.get(name);
		} else {
			Object[] inserts = { name };
			runtime.raiseExecError(PHPErrorHandler.E_NOTICE, null,
					"Constants.Undefined", inserts);
			return PHPValue.createString(name);
		}
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opEXIT(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {

		if (opbool) {
			// exit expression
			PHPValue val = stack.pop();
			jhEXIT(val, runtime);
		} else {
			// no exit expression
			jhEXIT();
		}
	}
	
	/**
	 * @param val .
	 * @param runtime .
	 */
	public static void jhEXIT(PHPValue val, RuntimeInterpreter runtime) {

		// exit expression
		if (val.getType() == Types.PHPTYPE_STRING) {

			jhECHO(val, runtime);
			TerminateScript ts = new TerminateScript(Reasons.Script, val
					.getJavaString());
			ts.setExitCode(0);
			throw (ts);
		} else {
			// all other types
			TerminateScript ts = new TerminateScript(Reasons.Script, null);
			ts.setExitCode(val.getInt());
			throw (ts);
		}
	}

	/**
	 * Exit / Die.
	 */
	public static void jhEXIT() {
		// no exit expression
		TerminateScript ts = new TerminateScript(Reasons.Script, null);
		ts.setExitCode(0);
		throw (ts);
	}
	
	
	/**
	 * @param runtime .
	 * @param remaining .
	 * @param silent .
	 * @param stack .
	 * @param liStack .
	 */
	public static void opLIST_INIT(RuntimeInterpreter runtime,
			int remaining, boolean silent, PHPStack stack, Stack<ListIterator> liStack) {

		PHPValue val = stack.pop();
		ListIterator li = jhLIST_INIT(val, runtime, remaining);
		liStack.push(li);
		stack.push(jhLIST_INIT(li, silent));
		return;
	}
	
	/**
	 * @param val .
	 * @param runtime .
	 * @param listSize .
	 * @return list iterator
	 */
	public static ListIterator jhLIST_INIT(PHPValue val, RuntimeInterpreter runtime, int listSize) {

		return new ListIterator(runtime, val, listSize);
	}
	
	/**
	 * @param listIterator .
	 * @param silent .
	 * @return first value
	 */
	public static PHPValue jhLIST_INIT(ListIterator listIterator, boolean silent) {

		return listIterator.init(silent);
	}

	/**
	 * @param runtime .
	 * @param silent .
	 * @param stack .
	 * @param liStack .
	 */
	public static void opLIST_NEXT(RuntimeInterpreter runtime, boolean silent,
			PHPStack stack, Stack<ListIterator> liStack) {

		ListIterator li = liStack.peek();
		stack.push(jhLIST_NEXT(li, silent));
		return;
	}
	
	/**
	 * @param iterator .
	 * @param silent .
	 * @return value .
	 */
	public static PHPValue jhLIST_NEXT(ListIterator iterator, boolean silent) {
		return iterator.next(silent);
	}

	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 * @param liStack .
	 * jh not required
	 */
	public static void opLIST_FREE(RuntimeInterpreter runtime,
			boolean opbool, PHPStack stack, Stack<ListIterator> liStack) {

		ListIterator li = liStack.pop();
		if (opbool) { // leave lhs on stack
			stack.push(jhLIST_FREE(li));
		}
		return;
	}
	
	/**
	 * @param iterator .
	 * @return lhs list
	 */
	public static PHPValue jhLIST_FREE(ListIterator iterator) {
		return iterator.getList();
	}

	/**
	 * @param runtime .
	 * @param stack .
	 */
	public static void opRETURN_BY_REF_CHECK(RuntimeInterpreter runtime, PHPStack stack) {
		jhRETURN_BY_REF_CHECK(stack.peek(), runtime);
	}
	
	/**
	 * @param value .
	 * @param runtime .
	 * @return value
	 */
	public static PHPValue jhRETURN_BY_REF_CHECK(PHPValue value, RuntimeInterpreter runtime) {
		if (!value.isReferenced() || !value.isWritable()) {
    		// If the value is temporary or not writable, we haven't returned by reference.
			runtime.raiseExecError(PHPErrorHandler.E_NOTICE, null, "Variables.ReturnByRef", null);
		}
		return value;
	}
	
	/**
	 * @param runtime .
	 * @param opbool .
	 * @param stack .
	 */
	public static void opTICKER(RuntimeInterpreter runtime, boolean opbool,
			PHPStack stack) {
		if (opbool) {
			jhTICK_PUSH(stack.pop(), runtime);
		} else {
			jhTICK_POP(runtime);
		}
		return;
	}
	
	/**
	 * @param value .
	 * @param runtime .
	 */
	public static void jhTICK_PUSH(PHPValue value, RuntimeInterpreter runtime) {
		runtime.getTicker().pushTicks(value.getInt());
	}
	
	/**
	 * @param runtime .
	 */
	public static void jhTICK_POP(RuntimeInterpreter runtime) {
		runtime.getTicker().popTicks();
	}

	/**
	 * @param runtime .
	 * @param opint .
	 * @param stack .
	 */
	public static void opMULTI_CONCAT(RuntimeInterpreter runtime, int opint,
			PHPStack stack) {
		PHPValue[] values = new PHPValue[opint];
		for (int i = opint - 1; i >= 0; i--) {
			values[i] = stack.pop();
		}
		stack.push(jhMULTI_CONCAT(values, runtime));
	}
	
	/**
	 * @param values .
	 * @param runtime .
	 * @return concatenated
	 */
	public static PHPValue jhMULTI_CONCAT(PHPValue[] values, RuntimeInterpreter runtime) {
		return Operators.multi_concat(runtime, values);
	}
	
	/**
	 * @param bool .
	 * @return true if bool is false
	 */
	public static boolean jhIF_FALSE(PHPValue bool) {
		return !bool.getBoolean();
	}
	
	/**
	 * @param bool .
	 * @return true if bool is true
	 */
	public static boolean jhIF_TRUE(PHPValue bool) {
		return bool.getBoolean();
	}
	
	/**
	 * @param value return value .
	 * @param runtime .
	 */
	public static void jhRETURN(PHPValue value, RuntimeInterpreter runtime) {
		runtime.getStack().push(value);
	}
	

	/**
	 * @param runtime .
	 */
	public static void jhCHECK_TIMEOUT(RuntimeInterpreter runtime) {
		runtime.checkTimeout();
	}
	
	/**
	 * @param runtime .
	 * @return PHPStack size
	 */
	public static int jhTRY_ENTER(RuntimeInterpreter runtime) {
		return runtime.getStack().size();
	}
	
	/**
	 * @param runtime .
	 * @param lineNumber the current line number
	 */
	public static void jhCheckForSuspend(RuntimeInterpreter runtime, int lineNumber) {
		runtime.getDebugProvider().checkForSuspend(lineNumber);
	}
	
	/**
	 * Lookup function.
	 * @param runtime .
	 * @param functionName .
	 * @return function table entry
	 */
	public static Invocable jhGetInvocable(RuntimeInterpreter runtime, NameString functionName) {
		return runtime.getFunctions().lookupScriptFunction(functionName);
	}
	
	/**
	 * Lookup function.
	 * @param runtime .
	 * @param functionName .
	 * @return function table entry
	 */
	public static ExecutableCode jhGetExecutable(RuntimeInterpreter runtime, NameString functionName) {
		return runtime.getFunctions().lookupScriptFunction(functionName).getThunk();
	}
	
	/**
	 * @param runtime -
	 *            the runtime
	 * @param varName -
	 *            the variable
	 */
	public static void raiseUndefinedVariableError(RuntimeInterpreter runtime,
			String varName) {
		Object[] inserts = { varName };
		runtime.raiseExecError(PHPErrorHandler.E_NOTICE, null,
				"Variables.Undefined", inserts);
	}

	/**
	 * 
	 * @param runtime .
	 * @param object .
	 * @param field .
	 * @param warnOnInstantiation .
	 * @return property PHPValue
	 */
	private static PHPValue propertyReadPreparingWrite(RuntimeInterpreter runtime, PHPValue object, String field,
			boolean warnOnInstantiation) {
		if (object.getType() == PHPValue.Types.PHPTYPE_NULL
				|| (object.getType() == PHPValue.Types.PHPTYPE_BOOLEAN && !object.getBoolean())
				|| (object.getType() == PHPValue.Types.PHPTYPE_STRING && StringFacade.strlen(object) == 0)) {
			if (warnOnInstantiation) { // warn on instantiation
				runtime.raiseExecError(PHPErrorHandler.E_STRICT, null, "Object.ImplicitInstantiation", null);
			}
			object.copy(PHPValue.createObject(runtime.getClasses().getPHPClass(PHPStdClass.CLASS_NAMESTRING)));
		}
		return ObjectFacade.getPropertyValue(runtime, object, field, true, false, true);
	}


	public boolean isTick() {
		return tick;
	}

	public int getInteger() {
		return integer;
	}

	public String getString() {
		return string;
	}

	public PHPValue getPhpValue() {
		return phpValue;
	}

	public Operand getOperand() {
		return operand;
	}
	
	public String getName() {
		return OPNAME[operation & 0xff];
	}

	/**
	 * @return true if this op may be stepped into - that is, if the debugger
	 *         should consider halting the runtime for this op even if it was
	 *         already halted for another op on the same line.
	 */
	public boolean canBeSteppedInto() {
		switch (this.getOperation()) {
		case Op.CALL:
		case Op.INVOKE_FUNCTION:
		case Op.INVOKE_METHOD:
		case Op.INVOKE_STATIC_METHOD:
		case Op.INCLUDE:
		case Op.PROPERTY_R:
		case Op.ASSIGN_VAL_PROPERTY:
		case Op.UNSET_PROPERTY:
		case Op.ISSET_PROPERTY:
		case Op.INDEX_R:
		case Op.ASSIGN_VAL_INDEX:
		case Op.UNSET_INDEX:
		case Op.ISSET_INDEX:
			return true;
		default:
			return false;
		}
	}

	/**
	 * @return true if the debugger should consider halting the runtime on new
	 *         lines containing this op.
	 */
	public boolean checkLineStep() {
		switch (this.getOperation()) {
		case Op.PUSH:
		case Op.NEWARRAY:
		case Op.ARRAY_INSERT:
			return false;
		default:
			return true;
		}
	}
}
