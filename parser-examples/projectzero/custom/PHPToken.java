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
import com.ibm.p8.engine.parser.core.Scanner;

/**
 * Adds some additional constructors to <code>Token</code> for ease of use.
 * 
 */
public class PHPToken extends Token {

	/**
	 * The types of string that a token can represent.
	 */
	enum StringType {
		doubleQuoted, heredoc, backQuoted
	};
	
	private StringType type = null;
	
	public StringType getStringType() {
		return type;
	}
	
	public void setStringType(StringType newType) {
		type = newType;
	}


	/**
	 * @param scanner
	 *            The scanner associated with this token
	 * @param lineNumber
	 *            The line number of the script that this token refers to
	 * @param kind
	 *            The type of parser terminal this token represents
	 * @param start
	 *            The index into the script of the start of this token
	 * @param end
	 *            The index into the script of the end of this token
	 */
	public PHPToken(Scanner scanner, int lineNumber, int kind, int start,
			int end) {
		super();
		setScanner(scanner);
		setLine(lineNumber);
		setStartOffset(start);
		setEndOffset(end);
		setKind(kind);
	}

	/**
	 * @param scanner
	 *            The scanner associated with this token
	 * @param lineNumber
	 *            The line number of the script that this token refers to
	 * @param kind
	 *            The type of parser terminal this token represents
	 * @param start
	 *            The index into the script of the start of this token
	 */
	public PHPToken(Scanner scanner, int lineNumber, int kind, int start) {
		super();
		setScanner(scanner);
		setLine(lineNumber);
		setStartOffset(start);
		setKind(kind);
	}

	/**
	 * Makes some basic checks to see if the token is valid,
	 * such as are the offsets sensible.
	 * 
	 * @return true if the token passes the tests
	 */
	public boolean isValid() {
		if (getStartOffset() > getEndOffset()) {
			return false;
		}
		if (getScanner() == null) {
			return false;
		}
		return true;
	}

}
