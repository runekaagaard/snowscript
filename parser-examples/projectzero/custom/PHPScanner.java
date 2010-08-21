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

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.ibm.p8.engine.core.FatalError;
import com.ibm.p8.engine.core.PHPErrorHandler;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.RuntimeInterpreter;
import com.ibm.p8.engine.parser.core.LexStream;
import com.ibm.p8.engine.parser.core.Scanner;
import com.ibm.p8.engine.parser.core.Token;
import com.ibm.p8.utilities.log.P8LogManager;
import com.ibm.p8.utilities.util.ParserUtils;
import com.ibm.phpj.logging.SAPILevel;
import com.ibm.phpj.logging.SAPIComponent;

/**
 * Custom scanner with special behaviour to read tokens. This scanner could be
 * implemented using JikesPG (which can also be used to create scanners).
 * 
 * @see com.ibm.p8.engine.parser.custom.Factory
 */
public final class PHPScanner extends Scanner {

	private static final Logger LOGGER = P8LogManager._instance
			.getLogger(SAPIComponent.Parser);

	private static final boolean DEBUG = false;

	private boolean insideHTML = true;
	
	private boolean insideASP = false;
	
	private boolean needingEchoGenerate = false;

	private boolean lookingForObjectField = false;

	// To keep track of composite strings
	private boolean processingSimpleExpression = false;

	private boolean lookingForTSTRVAR = false;

	private int heredocStart = 0;

	private int heredocEnd = 0;

	private StringEscapeProcessor theStringProcessor = null;

	// Used to produce a list of tokens closer to
	// the php.net list of tokens for the same script.
	private boolean retainAllTokens = false;
	
	/**
	 * Create a PHPString from a Java String with
	 * escape sequences, using the runtime encoding.
	 * 
	 * @param token token containing the Java String
	 * @return PHPValue wrapping a PHPString
	 */
	public static PHPValue phpStringFromEscapedString(Token token) {
		return processEscapedString(token, false);
	}
	
	/**
	 * Create a PHPString from a Java String with
	 * escape sequences, using the script encoding.
	 * 
	 * @param token token containing the Java String
	 * @return PHPValue wrapping a PHPString
	 */
	public static PHPValue scriptEncodePHPStringFromEscapedString(Token token) {
		return processEscapedString(token, true);
	}
	
	/**
	 * Create a PHPString from a Java String with escape sequences.
	 * 
	 * @param token
	 *            token containing the Java String
	 * @param useScriptEncoding
	 *            true to use the script encoding rather than the runtime
	 *            encoding
	 * @return PHPValue wrapping a PHPString
	 */
	private static PHPValue processEscapedString(Token token, boolean useScriptEncoding) {
		Scanner scanner = token.getScanner();
		if (!(scanner instanceof PHPScanner)) {
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE, "2006");
			}
			throw new FatalError("Scanner class is not instanceof PHPScanner");
		}
		PHPScanner phpScanner = (PHPScanner) scanner;
		if (phpScanner.theStringProcessor == null) {
			phpScanner.theStringProcessor = new StringEscapeProcessor();
		}
		
		if (useScriptEncoding) {
			return phpScanner.theStringProcessor.scriptEncodePHPStringFromToken(token);
		} else {
			return phpScanner.theStringProcessor.phpStringFromToken(token);
		}
	}

	/**
	 * Used to deal with expansion of variables inside strings (simple syntax).
	 * eg: $a = "$b[key]";
	 */
	private enum SimpleStringExpressionState {
		seenObjectOperator, insideBrackets, seenIdentifier, seenArrayKey
	};

	/**
	 * Used to deal with expansion of variables inside strings (complex syntax).
	 * eg: $a = "{$b->c()};
	 */
	private enum ComplexStringExpressionState {
		inDoublequotes, inHeredoc, inBackquotes, inExpression
	};

	private java.util.Stack<SimpleStringExpressionState> simpleStringExpressionStack = new java.util.Stack<SimpleStringExpressionState>();

	private java.util.Stack<ComplexStringExpressionState> complexStringExpressionStack = new java.util.Stack<ComplexStringExpressionState>();

	public static final char EOF_CHAR = '\uffff', LF_CHAR = '\n', FORMFEED_CHAR = '\f',
			CR_CHAR = '\r', TAB_CHAR = '\t', VERTICAL_TAB_CHAR = 0xB, BACKQUOTE_CHAR = '`',
			DOUBLEQUOTE_CHAR = '"', SINGLEQUOTE_CHAR = '\'', LT_CHAR = '<',
			GT_CHAR = '>', QUESTION_CHAR = '?', DOLLAR_CHAR = '$',
			OPENCURLY_CHAR = '{', CLOSECURLY_CHAR = '}', BACKSLASH_CHAR = '\\',
			SLASH_CHAR = '/', PERIOD_CHAR = '.', OPENSQUARES_CHAR = '[',
			CLOSESQUARES_CHAR = ']', STAR_CHAR = '*', HASH_CHAR = '#',
			EQUALS_CHAR = '=', PERCENT_CHAR = '%';

	public static final int DECIMAL_RADIX = 10, HEX_RADIX = 16,
			OCTAL_RADIX = 8;

	/**
	 * Called when we encounter the start of an encapsulated string. Allows us
	 * to save our state on the state stack
	 * 
	 * @param state
	 *            The type of string(heredoc, backquotes, doublequotes) that we
	 *            have moved into.
	 * @see #endStringState(ComplexStringExpressionState)
	 */
	private void startStringState(ComplexStringExpressionState state) {
		complexStringExpressionStack.push(state);
	}

	/**
	 * Must be called when we end processing an encapsulated string. Allows us
	 * to pop state off the state stack
	 * 
	 * @param state
	 *            The type of string we are leaving
	 * @see #startStringState(ComplexStringExpressionState)
	 */
	private void endStringState(ComplexStringExpressionState state) {
		assert (complexStringExpressionStack.peek() == state) : "Invalid Scanner state";
		complexStringExpressionStack.pop();
	}

	/**
	 * Must be called if processing a string and we encounter the complex
	 * variable syntax start. This is either : <code>${</code> or
	 * <code>{$</code>
	 * 
	 * @see #endStringState(ComplexStringExpressionState)
	 * @see #endExpression()
	 */
	private void startExpression() {
		complexStringExpressionStack
				.push(ComplexStringExpressionState.inExpression);
	}

	/**
	 * Must be called when we get to the end of a complex variable inside a
	 * string. Sets the appropriate state.
	 * 
	 * @return Token for the end of expression char
	 * @see #startExpression()
	 */
	private Token endExpression() {
		assert (complexStringExpressionStack.peek() == ComplexStringExpressionState.inExpression) : "Invalid Scanner state";
		complexStringExpressionStack.pop();

		PHPToken token = new PHPToken(this, charStream.getLine(),
				T_CURLY_CLOSE, charStream.getIndex(), charStream.getIndex());

		// Scanner:Token:Expression:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1502", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}

		nextChar = getChar();
		return token;
	}

	/**
	 * Check to see if we are processing a complex variable expression inside a
	 * string.
	 * 
	 * @return true if we are, otherwise false
	 */
	private boolean processingExpression() {
		if (!complexStringExpressionStack.isEmpty()) {
			if (complexStringExpressionStack.peek() == ComplexStringExpressionState.inExpression) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check to see if we are in the middle of processing a composite string.
	 * 
	 * @return boolean true if we are, otherwise false
	 */
	private boolean processingString() {
		if (!complexStringExpressionStack.isEmpty()) {
			if (complexStringExpressionStack.peek() != ComplexStringExpressionState.inExpression) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Skip spaces in the script. Whitespace can include new lines, which isn't
	 * always what we want to skip. Here we just skip spaces and tabs (not new
	 * lines or comments) This method always skips, whatever state the scanner
	 * is in.
	 * 
	 * @see #skip_spaces()
	 */
	private void skipTabsAndSpaces() {
		if (!processingString()) {
			while (nextChar != EOF_CHAR
					&& (Character.getType(nextChar) == Character.SPACE_SEPARATOR || nextChar == TAB_CHAR)) {
				nextChar = getChar();
			}
		}
		return;
	}

	/**
	 * Return a comment token, if it is the next token.
	 * (Will not return a doc comment token).
	 * 
	 * @return A comment token, or null.
	 */
	private Token findNonDocComment() {

		if (nextChar == SLASH_CHAR && peekChar(1) == SLASH_CHAR
				|| nextChar == HASH_CHAR) {
			// c++/shell style single line comment
			Token token = new Token();
			token.setKind(Token.T_COMMENT);
			token.setScanner(this);
			token.setStartOffset(charStream.getIndex());
			token.setLine(charStream.getLine());
			
			while (true) {

				if (nextChar == EOF_CHAR) {
					break;
				}

				if (nextChar == LF_CHAR || nextChar == CR_CHAR) {
					break;
				}

				if (nextChar == QUESTION_CHAR && peekChar(1) == GT_CHAR) {
					break;
				}

				nextChar = getChar();
			}
			token.setEndOffset(charStream.getIndex() - 1);
			return token;
		}

		if (nextChar == SLASH_CHAR && peekChar(1) == STAR_CHAR) {
			// multi line c style comment
			Token token = new Token();
			token.setKind(Token.T_COMMENT);
			token.setScanner(this);
			token.setStartOffset(charStream.getIndex());
			token.setLine(charStream.getLine());
			
			nextChar = getChar();
			nextChar = getChar(); /* ignore slash-star-slash */
			char prevChar;
			do {
				if (nextChar == EOF_CHAR) {
					break;
				}

				prevChar = nextChar;
				nextChar = getChar();
				if (nextChar == SLASH_CHAR && prevChar == STAR_CHAR) {
					nextChar = getChar();
					break;
				}
			} while (true);
			token.setEndOffset(charStream.getIndex() - 1);
			return token;
		}

		return null; // we haven't found a comment
	}

	/**
	 * Find a single doc comment.
	 * 
	 * @return the token if we found one, else null
	 */

	private Token findDocComment() {

		if (nextChar != SLASH_CHAR || peekChar(1) != STAR_CHAR
				|| peekChar(2) != STAR_CHAR
				|| !Character.isWhitespace(peekChar(3))) {
			return null;
		}

		// we have a doc comment to find

		Token token = new Token();
		token.setScanner(this);
		token.setStartOffset(charStream.getIndex());
		token.setLine(charStream.getLine());

		// skip to the start of the comment.
		// <slash><star><star><slash> is not a doc comment
		// <slash><star><star><star><slash> is a doc comment
		nextChar = getChar();
		nextChar = getChar();
		nextChar = getChar();

		if (nextChar == SLASH_CHAR) {
			nextChar = getChar();
			return null;
		}
		do {
			if ((nextChar == STAR_CHAR && peekChar(1) == SLASH_CHAR)) {
				nextChar = getChar();
				token.setEndOffset(charStream.getIndex());
				token.setKind(Token.T_DOC_COMMENT);
				nextChar = getChar();
				return token;
			}
			nextChar = getChar();
		} while (nextChar != EOF_CHAR);

		// we reached the end of file.
		token.setEndOffset(charStream.getIndex());
		token.setKind(Token.T_DOC_COMMENT);
		return token;
	}

	/**
	 * 
	 * @param theChar
	 *            The char to be checked
	 * @return True if in the range 0-7
	 */
	public static boolean isOctalDigit(char theChar) {
		if (theChar >= '0' && theChar <= '7') {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param theChar
	 *            to be checket
	 * @return true if in the ranges 0-9, a-f or A-F
	 */
	public static boolean isHexDigit(char theChar) {
		if ((theChar >= '0' && theChar <= '9')
				|| (theChar >= 'a' && theChar <= 'f')
				|| (theChar >= 'A' && theChar <= 'F')) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param theChar
	 *            to be checket
	 * @return true if in the ranges 0-9
	 */
	public static boolean isDecimalDigit(char theChar) {
		if (theChar >= '0' && theChar <= '9') {
			return true;
		}
		return false;
	}

	private static final int CHAR_CR = 13;
	private static final int CHAR_LF = 10;

	/**
	 * Set the end offset for a halt compiler.
	 * 
	 * @param t - the token
	 */
	public void setEndOffset(Token t) {
		if (runtime != null) {
			int offset = t.getEndOffset() + 1;
			int next = charStream.getChar(offset);
			if (next == CHAR_CR) {
				next = (int) charStream.getChar(offset + 1);
				if (next == CHAR_LF) {
					offset += 1;
				}
			}
			runtime.getConstants().assignValue("__COMPILER_HALT_OFFSET__",
					PHPValue.createInt(offset));
		}
	}

	/**
	 * Read the next token in the input. In PHP there are roughly two main
	 * sections: Literal HTML text and PHP code. Depending on what section the
	 * scanner is in, different logic is used to scan for tokens. Special logic
	 * is needed for handling Strings. Rather that returning a full string and
	 * parsing it later, we split up strings into logical components, so we can
	 * simply scan them in our grammar.
	 * 
	 * @return The next token in the stream
	 * @throws IOException
	 *             if something goes wrong.
	 * 
	 */
	@Override
	public Token getNextToken() throws IOException {

		Token docToken = null;

		// We must handle this first to ensure we deal with spaces correctly.
		// $test = "${a}" - this represents the variable $a
		// $test = "${ a} - here 'a' represents a named constant
		if (lookingForTSTRVAR) {
			lookingForTSTRVAR = false;
			Token token = readStringVarname();
			if (token != null) {
				return token;
			}
		}
		
		if (needingEchoGenerate) {
			// we got a tag followed by an equals
			assert (nextChar == '=');
			Token t = new PHPToken(this, charStream.getLine(), T_ECHO, charStream.getIndex(),
					charStream.getIndex() + 1);
			nextChar = getChar();
			needingEchoGenerate = false;
			return t;
		}

		// These two also need to be done before we skip spaces
		if (insideHTML) {
			return readHtmlFragment();
		} else if (processingString()) {
			return continueStringHandler();
		}

		// It's now safe to move past any spaces or comments.
		if (retainAllTokens == false) {
			do {
				skip_spaces();
				Token t = findDocComment();
				if (t != null) {

					// Scanner:Token:DocComment
					if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
						Object[] inserts = new Object[] { formatDebug(t) };
						LOGGER.log(SAPILevel.DEBUG, "1504", inserts);
					}

					if (DEBUG) {
						printDebug(t);
					}

					// we have a doc comment , but don't put it in the tree.
					// this might be a reasonable solution. I am providing
					// a linked list of tokens from the following token, such
					// that all the doc can be retrieved and related to its
					// following token.
					if (docToken != null) {
						t.setDoc(docToken);
					}
					docToken = t;
				}
				skip_spaces();
			} while (findNonDocComment() != null);
		} else {
			skip_spaces();
			Token commentToken = findNonDocComment();
			if (commentToken == null) {
				commentToken = findDocComment();
			}
			if (commentToken != null) {
				return commentToken;
			}
		}
		
		// Deal with everything that isn't an identifier
		switch (nextChar) {
		case CLOSECURLY_CHAR:
			if (processingExpression()) {
				return endExpression();
			}
			break;
		case SINGLEQUOTE_CHAR:
			lookingForObjectField = false;
			return readConstantString();
		case BACKQUOTE_CHAR:
			lookingForObjectField = false;
			return startBackquoteHandler();
			// check for the start of a binary string
		case 'B':
		case 'b':
			if (peekChar(1) == DOUBLEQUOTE_CHAR) {
				Token t = new PHPToken(this, charStream.getLine(),
						T_BINARY, charStream.getIndex(),
						charStream.getIndex());
				nextChar = getChar();
				return t;
			} else if (peekChar(1) == SINGLEQUOTE_CHAR) {
				Token t = new PHPToken(this, charStream.getLine(),
						T_BINARY, charStream.getIndex(),
						charStream.getIndex());
				nextChar = getChar();
				return t;
			} else if (peekChar(1) == LT_CHAR 
					   && peekChar(2) == LT_CHAR
					   && peekChar(3) == LT_CHAR) {
				Token t = new PHPToken(this, charStream.getLine(),
						T_BINARY, charStream.getIndex(),
						charStream.getIndex());
				nextChar = getChar();
				return t;
			}
			break;
		case DOUBLEQUOTE_CHAR:
			lookingForObjectField = false;
			return startStringHandler();
		case LT_CHAR:
			if (peekChar(1) == LT_CHAR && peekChar(2) == LT_CHAR) {
				lookingForObjectField = false;
				Token ret1 = startHeredocHandler();
				if (ret1 != null && docToken != null) {
					ret1.setDoc(docToken);
				}
				return ret1;
			}
			break;
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			lookingForObjectField = false;
			Token ret2 = readNumber();
			if (ret2 != null && docToken != null) {
				ret2.setDoc(docToken);
			}
			return ret2;
		case PERIOD_CHAR:
			if (Character.isDigit(peekChar(1))) {
				// float, e.g.: .5
				lookingForObjectField = false;
				Token ret3 = readNumber();
				if (ret3 != null && docToken != null) {
					ret3.setDoc(docToken);
				}
				return ret3;
			}
			break;
		case QUESTION_CHAR:
			if (peekChar(1) == GT_CHAR && !insideASP) {
				Token ret4 = readEndOfPhpMarker();
				if (ret4 != null && docToken != null) {
					ret4.setDoc(docToken);
				}
				return ret4;
			}
			break;
		case PERCENT_CHAR:
			if (insideASP) {
				if (peekChar(1) == GT_CHAR) {
					Token ret4 = readEndOfPhpMarker();
					if (ret4 != null && docToken != null) {
						ret4.setDoc(docToken);
					}
					insideASP = false;
					return ret4;
				}
			}
			break;
		case EOF_CHAR:
			// we are terminating.
			return null;

		default:
			// do nothing
			break;
		}

		// Only thing left are identifiers.
		Token ret5 = readIdentifier();
		if (ret5 != null && docToken != null) {
			ret5.setDoc(docToken);
		}
		return ret5;
	}

	/**
	 * Print debug info about the token to stdout.
	 * 
	 * @param token
	 *            The token to be printed
	 * 
	 */
	private void printDebug(Token token) {
		printDebug(Token.getTokenName(token.getKind()), token.getLine(), token
				.getStartOffset(), token.getEndOffset());
	}

	/**
	 * Print debug info about the token to a string for trace points.
	 * 
	 * @param token
	 *            The token to be printed
	 * @return - string the debug string.
	 */
	private String formatDebug(Token token) {
		return formatDebug(Token.getTokenName(token.getKind()),
				token.getLine(), token.getStartOffset(), token.getEndOffset());

	}

	/**
	 * Print debug info to a string.
	 * 
	 * @param info
	 *            The message to print
	 * @param line
	 *            The line on which the token appears
	 * @param start
	 *            The start charStream.getIndex() of the script that the message
	 *            refers to
	 * @param end
	 *            The end charStream.getIndex() of the script that the message
	 *            refers to
	 * @return - String - the debug string.
	 */

	private String formatDebug(String info, int line, int start, int end) {
		int lineStart = charStream.getLineOffset(line);
		String ret = "Token[ type:" + info + " line:" + line + "(base:"
				+ lineStart + ")" + " from:" + (start - lineStart) + " to:"
				+ (end - lineStart) + "]";
		return ret;
	}

	/**
	 * Print debug info to std out.
	 * 
	 * @param info
	 *            The message to print
	 * @param line
	 *            The line on which the token appears
	 * @param start
	 *            The start charStream.getIndex() of the script that the message
	 *            refers to
	 * @param end
	 *            The end charStream.getIndex() of the script that the message
	 *            refers to
	 */

	private void printDebug(String info, int line, int start, int end) {
		System.out.print(formatDebug(info, line, start, end));
		System.out.print("[");
		for (int i = start; i <= end; i++) {
			System.out.print(charStream.getChar(i));
		}
		System.out.println("]");
	}

	/**
	 * @return true if the next token in the stream is a start tag: <?php
	 */
	private boolean isStartTag() {
		if (nextChar == LT_CHAR) {
			if (peekChar(1) == QUESTION_CHAR) {
				if ((peekChar(2) == 'p' || peekChar(2) == 'P')
						&& (peekChar(3) == 'h' || peekChar(3) == 'H')
						&& (peekChar(4) == 'p' || peekChar(4) == 'P')) {
					if (Character.isWhitespace(peekChar(5))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * @return true if the next token in the stream is a short start tag: <?
	 */
	private boolean isShortStartTag() {
		if (!short_tags) {
			return false;
		}
		if (nextChar == LT_CHAR) {
			if (peekChar(1) == QUESTION_CHAR) {
				if (Character.isWhitespace(peekChar(2)) || peekChar(2) == EQUALS_CHAR) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @return true if the next token in the stream is an ASP start tag: <%
	 */
	private boolean isASPStartTag() {
		if (!asp_tags) {
			return false;
		}
		if (nextChar == LT_CHAR) {
			if (peekChar(1) == PERCENT_CHAR) {
				if (Character.isWhitespace(peekChar(2)) || peekChar(2) == EQUALS_CHAR) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Read a literal HTML text fragment up to the next php-starts-here marker.
	 * 
	 * @return HTML token containing literal text, including layout and spacing.
	 */
	private Token readHtmlFragment() {
		
		// check for an immediate start tag
		if (isStartTag() || isShortStartTag() || isASPStartTag()) {
			insideHTML = false;
			if (isASPStartTag()) {
				insideASP = true;
			}
			// return start token (unless discarding)
			Token token = new PHPToken(this, charStream.getLine(), T_OPEN_TAG,
					charStream.getIndex());
			while (!(Character.isWhitespace(nextChar) || nextChar == EQUALS_CHAR)) {
				nextChar = getChar();
			}
			// start tag token should contain either an equals or whitespace char
			if (nextChar == EQUALS_CHAR) {
				needingEchoGenerate = true;
			} else {
				// whitespace
				nextChar = getChar();
			}
			token.setEndOffset(charStream.getIndex() - 1);
			if (!retainAllTokens) {
				return null;
			}
			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
		
		// no immediate start tag found, so make a token out of everything up to the start tag or EOF
		Token token = new PHPToken(this, charStream.getLine(), T_INLINE_HTML,
				charStream.getIndex());				
		while (nextChar != EOF_CHAR) {
			nextChar = getChar();
			if (isStartTag() || isShortStartTag() || isASPStartTag()) {
				break;
			}
		}
		token.setEndOffset(charStream.getIndex() - 1);

		// Scanner:Token:HTML2:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1506", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}
		return token;
	}

	/*
	 * Lookup tables to allow us to do matching of cast expressions
	 */
	private static final char[][] CASTS = {
			new char[] { 'i', 'n', 't', 'e', 'g', 'e', 'r' },
			new char[] { 'b', 'o', 'o', 'l', 'e', 'a', 'n' },
			new char[] { 'd', 'o', 'u', 'b', 'l', 'e' },
			new char[] { 'b', 'i', 'n', 'a', 'r', 'y' },
			new char[] { 's', 't', 'r', 'i', 'n', 'g' },
			new char[] { 'o', 'b', 'j', 'e', 'c', 't' },
			new char[] { 'f', 'l', 'o', 'a', 't' },
			new char[] { 'a', 'r', 'r', 'a', 'y' },
			new char[] { 'r', 'e', 'a', 'l' },
			new char[] { 'b', 'o', 'o', 'l' }, new char[] { 'i', 'n', 't' }, };

	private static final int INT_CAST = T_INT_CAST,
			DOUBLE_CAST = T_DOUBLE_CAST, BINARY_CAST = T_STRING_CAST,
			STRING_CAST = T_STRING_CAST,
			ARRAY_CAST = T_ARRAY_CAST, OBJECT_CAST = T_OBJECT_CAST,
			BOOL_CAST = T_BOOL_CAST, FLOAT_CAST = T_FLOAT_CAST;

	private static final int[] CAST_VALUES = {
	/* 0 */INT_CAST,
	/* 1 */BOOL_CAST,
	/* 2 */DOUBLE_CAST,
	/* 3 */BINARY_CAST,
	/* 4 */STRING_CAST,
	/* 5 */OBJECT_CAST,
	/* 6 */FLOAT_CAST,
	/* 7 */ARRAY_CAST,
	/* 8 */DOUBLE_CAST,
	/* 9 */BOOL_CAST,
	/* 10 */INT_CAST, };

	/**
	 * A cast token represents <code>(int)</code> or similar. Because the
	 * brackets are part of the token, and there can be whitespace inside the
	 * brackets, we can't just match as a keyword.
	 * 
	 * @return Token or null if we don't match a cast
	 */
	private Token matchCasts() {

		if (nextChar != '(') {
			return null;
		}

		// Find the closing bracket
		int castIndex = charStream.getIndex() + 1;
		while (charStream.getChar(castIndex) != ')'
				&& charStream.getChar(castIndex) != EOF_CHAR) {
			castIndex++;
		}

		if (nextChar == EOF_CHAR) {
			return null;
		}

		int endbracket = castIndex;

		// Find the start of the text inside the brackets
		castIndex = charStream.getIndex() + 1;
		while (Character.isWhitespace(charStream.getChar(castIndex))) {
			castIndex++;
		}
		int startcast = castIndex;

		// Match the text against possible cast expressions
		for (int n = 0; n < CASTS.length; n++) {
			char[] cast = CASTS[n];
			int length = cast.length;
			if (matchKeyword(cast, length, startcast)) {
				// we've found bracket:spaces:cast:something
				// to match, 'something' must be space:bracket
				int whiteSpaceIndex = startcast + length;
				while (whiteSpaceIndex < endbracket) {
					if (!Character.isWhitespace(charStream
							.getChar(whiteSpaceIndex))) {
						// Found non-whitespace before the end bracket. Can't be
						// a cast.
						return null;
					}
					whiteSpaceIndex++;
				}

				Token token = new Token();
				token.setScanner(this);
				token.setStartOffset(charStream.getIndex());
				token.setLine(charStream.getLine());
				token.setEndOffset(endbracket);
				token.setKind(CAST_VALUES[n]);
				charStream.moveIndex(token.getEndOffset()
						- token.getStartOffset());
				nextChar = getChar();

				// Scanner:Token:Cast:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1507", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
				return token;
			}
		}
		return null;
	}

	/**
	 * Override from Scanner so that debug can be added Also allows matchCasts
	 * to be called before we do keywords.
	 * 
	 * @return Token null if we don't find a keyword, otherwise the appropriate
	 *         token
	 */
	@Override
	public Token matchKeywords() {
		// We may have a cast expression.
		Token token = matchCasts();
		if (token != null) {
			return token;
		}

		int start = charStream.getIndex();
		for (int n = 0; n < KEYWORDS.length; n++) {
			char[] keyword = KEYWORDS[n];
			int length = keyword.length;
			if (matchKeyword(keyword, length, charStream.getIndex())) {
				token = new Token();
				token.setScanner(this);
				token.setStartOffset(start);
				token.setLine(charStream.getLine());
				token.setEndOffset(charStream.getIndex() + length - 1);
				token.setKind(KEYWORD_VALUES[n]);
				charStream.moveIndex(length - 1);
				nextChar = getChar();

				// Scanner:Token:Keyword:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1508", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
				return token;
			}
		}
		return null;
	}

	/**
	 * Check in <code>contents</code> starting at <code>offset</code> to see
	 * if it matches the contents of <code>keyword</code>. The check is done
	 * case insensitive.
	 * 
	 * @param keyword
	 *            The keyword to match against
	 * @param length
	 *            The length of <code>keyword</code>
	 * @param offset
	 *            The start point in <code>contents</code> for checking
	 * @return true if we find a match
	 */
	private boolean matchKeyword(char[] keyword, int length, int offset) {
		for (int n = 0; n < length; n++) {
			if (keyword[n] != Character.toLowerCase(charStream
					.getChar(offset++))) {
				return false; // not a match
			}
		}
		if (ParserUtils.isPHPIdentifierPart(charStream.getChar(offset - 1))
				|| charStream.getChar(offset - 1) == DOLLAR_CHAR) {
			if (ParserUtils.isPHPIdentifierPart(charStream.getChar(offset))) {
				return false; // just a partial match?
			}
		}

		if (length == 2) {
			if (keyword[0] == '-' && keyword[1] == GT_CHAR) {
				lookingForObjectField = true;
			}
		}

		return true;
	}

	/**
	 * When we get to this method, we tried to read strings, numbers, and
	 * keyword symbols. Nothing matched any known patterns sofar, so now we read
	 * the next identifier.
	 * 
	 * @return the next token that represents an Identifier.
	 */
	private Token readIdentifier() {
		Token token;

		// If we're looking for an object field, we can't look for keywords
		// as there could be a naming clash, eg $a->if
		if (!(lookingForObjectField && ParserUtils.isPHPIdentifierStart(nextChar))) {
			token = matchKeywords();
			if (token != null) {
				return token;
			}
		}

		// We haven't matched a keyword, so the next token must be an identifier
		lookingForObjectField = false;
		int start = charStream.getIndex();
		char firstChar = charStream.getChar(start);
		if (firstChar == DOLLAR_CHAR) {
			nextChar = getChar();
		}
		if (ParserUtils.isPHPIdentifierStart(nextChar)) {
			while (ParserUtils.isPHPIdentifierPart(nextChar)) {
				nextChar = getChar();
			}
		} else {
			// This is an identifier with no identifying characters - its
			// a syntax error , so we will treat it as a string, and let the
			// parser find it.
			token = new PHPToken(this, charStream.getLine(), T_STRING, start,
					charStream.getIndex());
			nextChar = getChar();
			return token;
		}

		if (firstChar == DOLLAR_CHAR) {
			token = new PHPToken(this, charStream.getLine(), T_VARIABLE, start,
					charStream.getIndex() - 1);
		} else {
			token = new PHPToken(this, charStream.getLine(), T_STRING, start,
					charStream.getIndex() - 1);
		}

		// Scanner:Token:Identifier:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1509", inserts);
		}

		if (token.getEndOffset() < token.getStartOffset()) {
			if (runtime != null) {
				runtime.raisePreExecError(PHPErrorHandler.E_PARSE,
						"Scanner.invalidCharacters", null,
						token.getScanner().fileName, token.getLine());
			} else {
				if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
					LOGGER.log(SAPILevel.SEVERE, "1536.1");
				}
				throw new FatalError("Invalid characters in an identifier.");
			}
		}

		if (DEBUG) {
			printDebug(token);
		}
		return token;
	}

	/**
	 * Read a PHP identifier according to the normal rules. This is only used
	 * straight after we find a '${' token, so we can return the correct type of
	 * token
	 * 
	 * @return T_STRING_VARNAME token if we find one, else null
	 */
	private Token readStringVarname() {
		if (ParserUtils.isPHPIdentifierStart(nextChar)) {
			PHPToken token = new PHPToken(this, charStream.getLine(),
					T_STRING_VARNAME, charStream.getIndex());
			while (ParserUtils.isPHPIdentifierPart(nextChar)) {
				nextChar = getChar();
			}
			token.setEndOffset(charStream.getIndex() - 1);

			// Scanner:Token:Varname:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1510", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}

			// as isPart is always true if isStart is true - we should never
			// get into this state - so throw a fatal error and we will
			// catch the problem.
			if (token.getEndOffset() < token.getStartOffset()) {
				if (runtime != null) {
					runtime.raisePreExecError(PHPErrorHandler.E_PARSE,
							"Scanner.invalidCharacters", null, token
									.getScanner().fileName, token.getLine());
				} else {
					if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
						LOGGER.log(SAPILevel.SEVERE, "1536.2");
					}
					throw new FatalError(
							"Invalid characters in a variable name");
				}
			}
			return token;
		}
		return null;
	}

	/**
	 * We found '?>'. This defines the end of the PHP code in the enclosing HTML
	 * file. We add a semicolon to ensure termination of the block of script (as
	 * per php.net), skip the '?>', and start reading an HTML fragment from here
	 * on.
	 * 
	 * @return The final semicolon token
	 */
	private Token readEndOfPhpMarker() {
		int start = charStream.getIndex();

		nextChar = getChar();
		nextChar = getChar();
		insideHTML = true;

		// PHP adds a semicolon to the end of every termination
		// of a block of script. To be consistent we do the same.
		Token token = new Token();
		token.setScanner(this);
		token.setStartOffset(start);
		token.setLine(charStream.getLine());
		token.setEndOffset(start + 1);
		if (retainAllTokens == false) {
			token.setKind(T_SEMICOLON);
		} else {
			token.setKind(T_CLOSE_TAG);
		}

		// Scanner:Token:AdditionalColon:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1511", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}

		// If we have a new line immediately following the
		// marker, skip it. This is to match php.net behaviour
		if (nextChar == CR_CHAR && peekChar(1) == LF_CHAR) {
			getChar();
			nextChar = getChar();
		} else if (nextChar == LF_CHAR) {
			nextChar = getChar();
		}

		return token;

	}

	/**
	 * Special case for hexadecimal numbers.
	 * 
	 * @param start -
	 *            the start offset
	 * @return - the token
	 */
	private Token doHex(int start) {
		nextChar = getChar();
		nextChar = getChar();
		int numstart = charStream.getIndex();
		while (isHexDigit(nextChar)) {
			nextChar = getChar();
		}
		int end = charStream.getIndex();

		String hexnum = new String(getContents(), numstart, end - numstart);

		try {
			int value = Integer.parseInt(hexnum, HEX_RADIX);
			IntegerToken token = new IntegerToken(value);
			token.setScanner(this);
			token.setKind(Token.T_LNUMBER);
			token.setStartOffset(start);
			token.setEndOffset(end - 1);
			token.setLine(charStream.getLine());

			// Scanner:Token:Integer:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1512", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		} catch (NumberFormatException e) {

			double value = 0;
			for (int i = numstart; i < end; i++) {
				value = (value * 16) + getValueOf(charStream.getChar(i));
			}
			DoubleToken token = new DoubleToken(value);
			token.setScanner(this);
			token.setKind(Token.T_DNUMBER);
			token.setStartOffset(start);
			token.setEndOffset(end - 1);
			token.setLine(charStream.getLine());

			// Scanner:Token:Double
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1513", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
	}

	/**
	 * Special case for hexadecimal numbers.
	 * 
	 * @param start -
	 *            the start offset
	 * @return - the token
	 */
	private Token doDecimal(int start) {

		while (isDecimalDigit(nextChar)) {
			nextChar = getChar();
		}
		if (nextChar == '.' || nextChar == 'e' || nextChar == 'E') {
			// its really a decimal
			return doDouble(start);
		}

		int end = charStream.getIndex();
		String decnum = new String(getContents(), start, end - start);

		try {
			int value = Integer.parseInt(decnum, DECIMAL_RADIX);
			IntegerToken token = new IntegerToken(value);
			token.setScanner(this);
			token.setKind(Token.T_LNUMBER);
			token.setStartOffset(start);
			token.setEndOffset(end - 1);
			token.setLine(charStream.getLine());

			// Scanner:Token:Decimal:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1514", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		} catch (NumberFormatException e) {
			double value = 0;
			for (int i = start; i < end; i++) {
				value = (value * 10) + getValueOf(charStream.getChar(i));
			}
			DoubleToken token = new DoubleToken(value);
			token.setScanner(this);
			token.setKind(Token.T_DNUMBER);
			token.setStartOffset(start);
			token.setEndOffset(end - 1);
			token.setLine(charStream.getLine());

			// Scanner:Token:Double2:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1515", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
	}

	/**
	 * Get the value of a hex char.
	 * 
	 * @param c -
	 *            the character
	 * @return - the integer value
	 */
	private int getValueOf(char c) {
		switch (c) {
		case 'F':
		case 'f':
			return 15;
		case 'E':
		case 'e':
			return 14;
		case 'D':
		case 'd':
			return 13;
		case 'C':
		case 'c':
			return 12;
		case 'B':
		case 'b':
			return 11;
		case 'A':
		case 'a':
			return 10;
		default:
			return c - '0';
		}
	}

	/**
	 * Find an octal value.
	 * 
	 * @param start -
	 *            the start offset
	 * @return - the token.
	 */
	private Token doOctal(int start) {

		while (isOctalDigit(nextChar)) {
			nextChar = getChar();
		}
		if (nextChar == '.' || nextChar == 'e' || nextChar == 'E') {
			// its really a decimal
			return doDouble(start);
		}

		// behavour I think is right - start
		// if (isDecimalDigit(nextChar)) {
		// return doDecimal(start,negate);
		// }
		// behaviour end

		int end = charStream.getIndex();

		// alternative behaviour
		while (isDecimalDigit(nextChar)) {
			nextChar = getChar();
			if (nextChar == '.' || nextChar == 'e' || nextChar == 'E') {
				// its really a decimal
				return doDouble(start);
			}
		}
		// end behaviour

		String octnum = new String(getContents(), start, end - start);

		try {
			int value = Integer.parseInt(octnum, OCTAL_RADIX);
			IntegerToken token = new IntegerToken(value);
			token.setScanner(this);
			token.setKind(Token.T_LNUMBER);
			token.setStartOffset(start);
			token.setEndOffset(end - 1);
			token.setLine(charStream.getLine());

			// Scanner:Token:Octal:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1516", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		} catch (NumberFormatException e) {
			double value = 0;
			for (int i = start + 1; i < end; i++) {
				value = (value * 8) + getValueOf(charStream.getChar(i));
			}
			DoubleToken token = new DoubleToken(value);
			token.setScanner(this);
			token.setKind(Token.T_DNUMBER);
			token.setStartOffset(start);
			token.setEndOffset(end - 1);
			token.setLine(charStream.getLine());

			// Scanner:Token:Double3
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1517", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
	}

	/**
	 * Process a decimal.
	 * 
	 * @param start -
	 *            the start of the token
	 * @return - the token
	 */
	private Token doDouble(int start) {
		while (isDecimalDigit(nextChar)) {
			nextChar = getChar();
		}

		if (nextChar == PERIOD_CHAR) {
			nextChar = getChar();
			while (isDecimalDigit(nextChar)) {
				nextChar = getChar();
			}
		}

		// and the exponent
		if (nextChar == 'e' || nextChar == 'E') {
			/* make sure we really have an exponent */
			if (isDecimalDigit(peekChar(1)) ||
			    ((peekChar(1) == '+' || peekChar(1) == '-')	&&
				    isDecimalDigit(peekChar(2)))) {
				nextChar = getChar();
				if (nextChar == '+' || nextChar == '-') {
					nextChar = getChar();
				}
				while (isDecimalDigit(nextChar)) {
					nextChar = getChar();
				}
			}
		}

		int end = charStream.getIndex();
		String dnum = new String(getContents(), start, end - start);
		DoubleToken token = new DoubleToken(Double.parseDouble(dnum));
		token.setScanner(this);
		token.setKind(Token.T_DNUMBER);
		token.setStartOffset(start);
		token.setLine(charStream.getLine());
		token.setEndOffset(end - 1);

		// Scanner:Token:Double4:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1518", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}
		return token;
	}

	/**
	 * Read a number.
	 * 
	 * @return an integer token if the number fits, otherwise a float token (as
	 *         per PHP spec). When the constant is specified as a real number, a
	 *         float is also returned.
	 */
	private Token readNumber() {
		//
		// ---------------------------------------------------------------------------------------
		// GRAMMAR FOR PHP CONSTANTS
		// ---------------------------------------------------------------------------------------
		// From: http://www.php.net/manual/en/language.types.integer.php
		//			 
		// hexadecimal : 0[xX][0-9a-fA-F]+
		//			 
		// octal : 0[0-7]+
		//			 
		// decimal : [1-9][0-9]*
		// | 0
		//			 
		// integer : [+-]?decimal // this we solve in php.g
		// | [+-]?hexadecimal
		// | [+-]?octal
		//
		// lnum : [0?9]+ // rewritten to make more readable
		// dnum : lnum
		// | [0?9]*[\.]lnum
		// | lnum[\.][0?9]*
		// float : dnum [eE][+?]? lnum
		//

		int start = charStream.getIndex();

		if (nextChar == '0') {
			// hexadecimal...
			if (peekChar(1) == 'x' || peekChar(1) == 'X') {
				return doHex(start);
			} else {
				// possibly octal...
				if (!isDecimalDigit(peekChar(1))) {
					// its just a lone zero
					return doDecimal(start);
				}
				// octal processing will sort out if its a decimal
				return doOctal(start);
			}
		} else {
			return doDecimal(start);
		}
	}

	/**
	 * Read a constant string, do not replace variable expressions. We handle
	 * two escapes, a single quote (') and a backslash (\). A double backslash
	 * (\\) is interpreted as an escaped single backslach, wherever it appears A
	 * sequence of \' is interpreted as a singlequote. A single backslash
	 * followed by any other char is not a valid escape and is left as is.
	 * 
	 * @return the constant string up to the next unescapte single quote.
	 * @see #stringContentHandler()
	 */
	private Token readConstantString() {
		int start = charStream.getIndex() + 1;
		nextChar = getChar();
		while (nextChar != EOF_CHAR && nextChar != SINGLEQUOTE_CHAR) {
			if (nextChar == BACKSLASH_CHAR) {
				// Handle escapes
				if (peekChar(1) == SINGLEQUOTE_CHAR) {
					deleteChars(charStream.getIndex(), 1);
				} else if (peekChar(1) == BACKSLASH_CHAR) {
					deleteChars(charStream.getIndex(), 1);
				}
			}
			nextChar = getChar();
		}

		Token token = new Token();
		token.setScanner(this);
		token.setLine(charStream.getLine());
		token.setStartOffset(start);
		token.setEndOffset(charStream.getIndex() - 1);
		token.setKind(T_CONSTANT_ENCAPSED_STRING);

		// Scanner:Token:Encapsed:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1519", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}

		if (nextChar != EOF_CHAR) {
			nextChar = getChar();
		}

		return token;
	}

	/**
	 * Begin reading a backquoted string.
	 * 
	 * @return Token T_BACK_QUOTE, the quote itself
	 */
	private Token startBackquoteHandler() {
		Token token = new Token();
		token.setScanner(this);
		token.setStartOffset(charStream.getIndex());
		token.setLine(charStream.getLine());
		token.setEndOffset(charStream.getIndex());
		token.setKind(T_BACK_QUOTE);
		startStringState(ComplexStringExpressionState.inBackquotes);
		nextChar = getChar();

		// Scanner:Token:BackQuote:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1520", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}
		return token;
	}

	/**
	 * Begin reading a double quoted string.
	 * 
	 * @return Token representing the initial double quotes
	 */
	private Token startStringHandler() {
		Token token = new Token();
		token.setScanner(this);
		token.setStartOffset(charStream.getIndex());
		token.setLine(charStream.getLine());
		token.setEndOffset(charStream.getIndex());
		token.setKind(T_DOUBLE_QUOTE);
		startStringState(ComplexStringExpressionState.inDoublequotes);
		nextChar = getChar();

		// Scannner:Token:DoubleQuote:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1521", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}
		return token;

	}

	/**
	 * Begin reading a heredoc string.
	 * 
	 * @return Token representing the heredoc marker text
	 */
	private Token startHeredocHandler() {
		Token token = new PHPToken(this, charStream.getLine(), T_START_HEREDOC,
				charStream.getIndex());
		nextChar = getChar(); // move past heredoc string, '<<<'
		nextChar = getChar();
		nextChar = getChar();
		skipTabsAndSpaces();
		heredocStart = charStream.getIndex();
		// Heredoc identifier extends to the end of the line,
		while (nextChar != CR_CHAR && nextChar != LF_CHAR) {
			nextChar = getChar();
		}
		// but ignore trailing whitespace
		int endPoint = charStream.getIndex() - 1;
		for (; endPoint > heredocStart; endPoint--) {
			if (!Character.isWhitespace(charStream.getChar(endPoint))) {
				break;
			}
		}
		heredocEnd = endPoint;
		token.setEndOffset(endPoint);

		startStringState(ComplexStringExpressionState.inHeredoc);

		// Scanner:Token:HereDoc:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1522", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}

		// The heredoc string starts on the line below, so we skip
		// a single new line - unless the very next line is the end of the
		// heredoc

		if (foundEndHeredoc(heredocStart, heredocEnd, charStream.getIndex()) == 0) {

			if (nextChar == CR_CHAR && peekChar(1) == LF_CHAR) {
				getChar();
				nextChar = getChar();
			} else if (nextChar == LF_CHAR) {
				nextChar = getChar();
			}
		}

		return token;

	}

	/**
	 * Continue scanning an encapsulated string.
	 * 
	 * @return A token representing the next section of the string or an end
	 *         marker(quote or heredoc marker)
	 * @throws java.io.IOException exception
	 */
	private Token continueStringHandler() throws java.io.IOException {
		switch (complexStringExpressionStack.peek()) {
		case inDoublequotes:
			return continueDoublequoteHandler();
		case inHeredoc:
			return continueHeredocHandler();
		case inBackquotes:
			return continueBackquoteHandler();
		default:
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE, "2000");
			}
			throw new FatalError("Invalid Scanner state");
		}
	}

	/**
	 * Scan the next section of a heredoc string.
	 * 
	 * @return Token The next section of the string or end of heredoc marker if
	 *         we find the end of the string
	 * @throws java.io.IOException exception
	 */
	private Token continueHeredocHandler() throws java.io.IOException {
		// endOfHereDoc is a short cut but it can't always work, so we have to
		// use foundEndHereDoc as well

		int index = charStream.getIndex();

		int movex = foundEndHeredoc(heredocStart, heredocEnd, index);

		if (movex > 0) {
			endStringState(ComplexStringExpressionState.inHeredoc);
			
			// move past the end symbol
			charStream.moveIndex(movex - 1);
			nextChar = getChar();

			processingSimpleExpression = false;
			simpleStringExpressionStack.clear();

			// end_heredoc token should not include whitespace
			while (Character.isWhitespace(charStream.getChar(index))) {
				index++;
			}
			
			Token token = new PHPToken(this, charStream.getLine(),
					T_END_HEREDOC, index);
			token.setEndOffset(charStream.getIndex());

			// Scanner:Token:EndHereDoc:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1523", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
		return stringContentHandler();
	}

	/**
	 * Scan the next section of a double quoted string.
	 * 
	 * @return Token, either the next section of the string or double quote if
	 *         we find the end of the string
	 */
	private Token continueDoublequoteHandler() {
		if (nextChar == EOF_CHAR || nextChar == DOUBLEQUOTE_CHAR) {
			Token token = new PHPToken(this, charStream.getLine(),
					T_DOUBLE_QUOTE, charStream.getIndex(), charStream
							.getIndex());

			endStringState(ComplexStringExpressionState.inDoublequotes);
			processingSimpleExpression = false;
			simpleStringExpressionStack.clear();

			nextChar = getChar();

			// Scanner:Token:DQString:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1524", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
		return stringContentHandler();
	}

	/**
	 * Scan the next section of a backquoted string.
	 * 
	 * @return Token, either next section of string or back quote if we find the
	 *         end of the string
	 */
	private Token continueBackquoteHandler() {
		if (nextChar == EOF_CHAR || nextChar == BACKQUOTE_CHAR) {
			Token token = new PHPToken(this, charStream.getLine(),
					T_BACK_QUOTE, charStream.getIndex(), charStream.getIndex());

			endStringState(ComplexStringExpressionState.inBackquotes);
			processingSimpleExpression = false;
			simpleStringExpressionStack.clear();

			if (nextChar != EOF_CHAR) {
				nextChar = getChar();
			}

			// Scanner:Token:BQString:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1525", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}
		return stringContentHandler();
	}

	/**
	 * Check at the current charStream.getIndex() for a valid escape sequence,
	 * and if found, move forwards through the char stream to skip it.
	 * 
	 * @see #processEscapedString
	 * 
	 * @return true if we have processed an escape, otherwise false
	 */

	private boolean handleEscape() {
		// escape sequences
		//
		// \n linefeed (LF or 0x0A (10) in ASCII)
		// \v vertical tab (0xB in ASCII)
		// \f formfeed (0xC (12) in ASCII)
		// \r carriage return (CR or 0x0D (13) in ASCII)
		// \t horizontal tab (HT or 0x09 (9) in ASCII)
		// \\ backslash
		// \$ dollar sign
		// \{ dollar sign
		// \" double-quote
		// \[0-7]{1,3} character in octal notation
		// \x[0-9A-Fa-f]{1,2} character in hexadecimal notation, cl: and X?
		//

		switch (peekChar(1)) {
		case DOUBLEQUOTE_CHAR:
			if (complexStringExpressionStack.peek() == ComplexStringExpressionState.inDoublequotes) {
				nextChar = charStream.getChar();
				return true;
			} else {
				return false;
			}
		case 'n':
		case 'v':
		case 'f':	
		case 't':
		case 'r':
		case BACKSLASH_CHAR:
		case DOLLAR_CHAR:
		case OPENCURLY_CHAR:
			nextChar = charStream.getChar();
			return true;
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
			// An octal sequence can be \7, \77 or \777
			int length = 1;
			if (isOctalDigit(peekChar(2))) {
				length++;
				if (isOctalDigit(peekChar(3))) {
					length++;
				}
			}
			for (int i = 0; i < length; i++) {
				nextChar = charStream.getChar();
			}
			return true;
		case 'x':
		case 'X':
			// Hex escape must have two digits
			if (isDecimalDigit(peekChar(2))) {
				if (isDecimalDigit(peekChar(3))) {
					nextChar = charStream.getChar();
					nextChar = charStream.getChar();
				}
				return true;
			} else {
				return false;
			}
		default:
			// no legal replacement - just treat as a string
		}
		return false;
	}

	/**
	 * Scan the next section of a simple variable expression inside a string.
	 * This is a variable expansion with no curly brackets, eg:
	 * <code>echo "$a";</code>
	 * 
	 * @return the next token in the simple expression we are processing or null
	 *         if we don't find an appropriate continuation
	 */
	private PHPToken continueSimpleStringExpression() {
		PHPToken token = null;
		switch (simpleStringExpressionStack.peek()) {
		case seenObjectOperator:
			// This is always the end of the expression
			processingSimpleExpression = false;
			if (ParserUtils.isPHPIdentifierStart(nextChar)) {
				token = new PHPToken(this, charStream.getLine(), T_STRING,
						charStream.getIndex());
				nextChar = getChar();
				while (ParserUtils.isPHPIdentifierPart(nextChar)) {
					nextChar = getChar();
				}
				token.setEndOffset(charStream.getIndex() - 1);

				// Scanner:Token:TString:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1526", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
			}
			break;
		case insideBrackets:
			// The array key can either be a simple variable (ie no array/object
			// fields)
			if (nextChar == DOLLAR_CHAR && ParserUtils.isPHPIdentifierStart(peekChar(1))) {
				token = new PHPToken(this, charStream.getLine(), T_VARIABLE,
						charStream.getIndex());
				nextChar = getChar();
				while (ParserUtils.isPHPIdentifierPart(nextChar)) {
					nextChar = getChar();
				}
				token.setEndOffset(charStream.getIndex() - 1);
			}

			// Or it can be a numeric charStream.getIndex()
			if (Character.isDigit(nextChar)) {
				token = new PHPToken(this, charStream.getLine(), T_NUM_STRING,
						charStream.getIndex());
				while (Character.isDigit(nextChar)) {
					nextChar = getChar();
				}
				token.setEndOffset(charStream.getIndex() - 1);
			}

			// Or it can be a string key
			// We currently match php.net in that not all valid keys are
			// possible here. eg echo "$ar[2b]\n"; returns an error, even 
			// though normally '2b' would okay.
			if (nextChar != CLOSESQUARES_CHAR) {				
				if (Character.isWhitespace(nextChar) || nextChar == '\'') {
					// Error cases.
					// We match php.net by returning T_ENCAPSED_WHITESPACE (which should
					// cause an invalid sequence of tokens), and allow the parser to 
					// detect/report the error.
					token = new PHPToken(this, charStream.getLine(),
							T_ENCAPSED_AND_WHITESPACE, charStream.getIndex(),
							charStream.getIndex());
					nextChar = getChar();
				} else {
					token = new PHPToken(this, charStream.getLine(), T_STRING,
							charStream.getIndex());
					while (!(nextChar == CLOSESQUARES_CHAR || Character
							.getType(nextChar) == Character.SPACE_SEPARATOR)) {
						nextChar = getChar();
					}
					token.setEndOffset(charStream.getIndex() - 1);
				}
			}			
						
			if (token != null) {

				// Scanner:Token:TString2:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1527", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
				if (token.getEndOffset() < token.getStartOffset()) {
					if (runtime != null) {
						runtime
								.raisePreExecError(PHPErrorHandler.E_PARSE,
										"Scanner.invalidCharacters", null,
										token.getScanner().fileName, token
												.getLine());
					} else {
						if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
							LOGGER.log(SAPILevel.SEVERE, "1536.3");
						}
						throw new FatalError(
								"Unexpected characters inside square brackets");
					}
				}
				simpleStringExpressionStack
						.push(SimpleStringExpressionState.seenArrayKey);
			}
			break;
		case seenIdentifier:
			// After a label, we can either have an array access
			if (nextChar == OPENSQUARES_CHAR) {
				token = new PHPToken(this, charStream.getLine(),
						T_BRACKET_OPEN, charStream.getIndex(), charStream
								.getIndex());
				nextChar = getChar();

				// Scanner:Token:SeenIdent:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1528", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
				simpleStringExpressionStack
						.push(SimpleStringExpressionState.insideBrackets);
			}
			// Or the object operator
			if (nextChar == '-' && peekChar(1) == GT_CHAR) {
				token = new PHPToken(this, charStream.getLine(),
						T_OBJECT_OPERATOR, charStream.getIndex(), charStream
								.getIndex() + 1);
				nextChar = getChar();
				nextChar = getChar();

				// Scanner:Token:ObOperator:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1529", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
				simpleStringExpressionStack
						.push(SimpleStringExpressionState.seenObjectOperator);
			}
			// Or we have got to the end of our expression
			break;
		case seenArrayKey:
			// This is always the end of a simple expression
			processingSimpleExpression = false;
			if (nextChar == CLOSESQUARES_CHAR) {
				token = new PHPToken(this, charStream.getLine(),
						T_BRACKET_CLOSE, charStream.getIndex(), charStream
								.getIndex());

				// Scanner:Token:SeenArray:
				if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
					Object[] inserts = new Object[] { formatDebug(token) };
					LOGGER.log(SAPILevel.DEBUG, "1530", inserts);
				}

				if (DEBUG) {
					printDebug(token);
				}
				nextChar = getChar();
			}
			break;
		default:
			// Do nothing
			break;
		} // end switch on stringStateStack2

		if (token == null) {
			// We didn't find anything to continue the expression
			processingSimpleExpression = false;
		}
		return token;
	}

	/**
	 * Read the next element of an encapsulated string, ie a string where we
	 * expand escape sequences and variable names. This can either be part of a)
	 * a double quoted string, b) a heredoc string or c) a backquoted string.
	 * Each section of the string is returned as a seperate token. As an
	 * example, the following string:<br/>
	 * <code>"Hi, this is \"PHP\" running on $vm, written by $info['vendor']."</code><br/>
	 * would be scanned as
	 * 
	 * <pre>
	 *  1. STRING			'Hi, this is &quot;PHP&quot; running on '
	 *  2. VARIABLE			'vm'
	 *  3. STRING			', written by '
	 *  4. VARIABLE			'info'
	 *  5. LEFT_BRACKET		'['
	 *  6. CONSTANT_STRING	'vendor'
	 *  7. RIGHT_BRACKET		']'
	 *  8. STRING			'.'
	 * </pre>
	 * 
	 * The stringContentHandler() method scans for the next section of the
	 * string In the example above, for STRING 1., the escaped characters are
	 * replaced. Then, when the first '$' is detected, the STRING token is
	 * ended, and we return. When come back to scan for the next token, we
	 * continue where we left off, and will scan a variable. We continue like
	 * this till we hit the appropriate end of string marker.
	 * 
	 * @return the appropriate token for the next section of the string
	 */

	private Token stringContentHandler() {
		if (processingSimpleExpression) {
			PHPToken token = continueSimpleStringExpression();
			if (token != null) {
				return token;
			}
		} // We haven't found anything to continue a simple expression

		if (nextChar == DOLLAR_CHAR && peekChar(1) == OPENCURLY_CHAR) {
			// Found the start of a complex variable expression.
			// We push a state of inside expression so that we scan as
			// per normal php code next time round
			PHPToken token = new PHPToken(this, charStream.getLine(),
					T_DOLLAR_OPEN_CURLY_BRACES, charStream.getIndex(),
					charStream.getIndex() + 1);
			nextChar = getChar();
			nextChar = getChar();
			startExpression();
			lookingForTSTRVAR = true;

			// Scanner:Token:OpenC:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1531", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		} else if (nextChar == OPENCURLY_CHAR && peekChar(1) == DOLLAR_CHAR) {
			// Found the start of a complex variable expression.
			// We push a state of inside expression so that we scan as
			// per normal php code next time round
			PHPToken token = new PHPToken(this, charStream.getLine(),
					T_CURLY_OPEN, charStream.getIndex(),
					charStream.getIndex() + 1);
			nextChar = getChar();
			startExpression();

			// Scanner:Token:OpenC2:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1532", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		} else if (nextChar == DOLLAR_CHAR && ParserUtils.isPHPIdentifierStart(peekChar(1))) {
			// Found a variable start, and start processing a simple expression
			PHPToken token = new PHPToken(this, charStream.getLine(),
					T_VARIABLE, charStream.getIndex());
			nextChar = getChar();
			while (nextChar != EOF_CHAR && ParserUtils.isPHPIdentifierPart(nextChar)) {
				nextChar = getChar();
			}
			token.setEndOffset(charStream.getIndex() - 1);
			processingSimpleExpression = true;
			simpleStringExpressionStack.clear();
			simpleStringExpressionStack
					.push(SimpleStringExpressionState.seenIdentifier);

			// Scanner:Token:seenIdent2:
			if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
				Object[] inserts = new Object[] { formatDebug(token) };
				LOGGER.log(SAPILevel.DEBUG, "1533", inserts);
			}

			if (DEBUG) {
				printDebug(token);
			}
			return token;
		}

		// now we just have string contents to deal with
		PHPToken token = new PHPToken(this, charStream.getLine(),
				T_ENCAPSED_AND_WHITESPACE, charStream.getIndex());

		switch (complexStringExpressionStack.peek()) {
		case inBackquotes:
			token.setStringType(PHPToken.StringType.backQuoted);
			break;
		case inDoublequotes:
			token.setStringType(PHPToken.StringType.doubleQuoted);
			break;
		case inHeredoc:
			token.setStringType(PHPToken.StringType.heredoc);
			break;
		default:
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE, "2003");
			}
			throw new FatalError("Invalid Scanner state");
		}

		while (!endOfStringFragment()) {
			if (nextChar == BACKSLASH_CHAR) {
				handleEscape();
			}
			nextChar = getChar();
		}

		token.setEndOffset(charStream.getIndex() - 1);

		// Scanner:Token:String:
		if (LOGGER.isLoggable(SAPILevel.DEBUG)) {
			Object[] inserts = new Object[] { formatDebug(token) };
			LOGGER.log(SAPILevel.DEBUG, "1534", inserts);
		}

		if (DEBUG) {
			printDebug(token);
		}
		return token;

	}

	/**
	 * Check to see if we are at the end of an encapsulated string fragment
	 * (heredoc, back quotes, or double quotes). This will be a variable (simple
	 * or complex) or an end of string marker. Sets endOfHeredoc = true if we
	 * find the end of a heredoc string
	 * 
	 * @return boolean true if the next char is the end of the string
	 */
	private boolean endOfStringFragment() {
		if (nextChar == DOLLAR_CHAR
				&& (ParserUtils.isPHPIdentifierStart(peekChar(1)) || peekChar(1) == OPENCURLY_CHAR)) {
			return true;
		}

		if (nextChar == OPENCURLY_CHAR && peekChar(1) == DOLLAR_CHAR) {
			return true;
		}

		if (nextChar == EOF_CHAR) {
			return true;
		}

		// Escaped quotes are skipped by the handleEscape function
		if (nextChar == DOUBLEQUOTE_CHAR
				&& complexStringExpressionStack.peek() == ComplexStringExpressionState.inDoublequotes) {
			return true;
		}

		if (nextChar == BACKQUOTE_CHAR
				&& complexStringExpressionStack.peek() == ComplexStringExpressionState.inBackquotes) {
			return true;
		}

		if (complexStringExpressionStack.peek() == ComplexStringExpressionState.inHeredoc
				&& foundEndHeredoc(heredocStart, heredocEnd, charStream
						.getIndex()) > 0) {
			// note - we do not need to move on in this case - the next token
			// will
			// find the here doc end tag.
			return true;
		}

		return false;
	}

	/**
	 * Looks to see if the current position is at the end of a here doc string.
	 * Here doc ends when we find the:
	 * <ul>
	 * <li>a new line</li>
	 * <li>the here doc marker</li>
	 * <li>and then either:
	 * <ul>
	 * <li>a new line</li>
	 * <li>a semicolon and then a new line</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * Anything else, and we continue with the heredoc string.
	 * 
	 * @param markerStartPos
	 *            in the char stream, the index of the start of the heredoc
	 *            marker text
	 * @param markerEndPos
	 *            in the char stream, the index of the end of the heredoc marker
	 *            text
	 * @param index
	 *            the index into the char stream where we start looking for the
	 *            closing heredoc marker
	 * @return zero if we do not find a match, or the length to move on if we
	 *         do.
	 */
	private int foundEndHeredoc(int markerStartPos, int markerEndPos, int index) {

		int pos = index;

		// check for preceding EOL
		if (charStream.getChar(pos) == CR_CHAR) {
			pos++;
		}

		if (charStream.getChar(pos) == LF_CHAR) {
			pos++;
		} else {
			return 0;
		}

		// check for the marker
		if (charStream.getChar(pos) != charStream.getChar(markerStartPos)) {
			return 0;
		}

		int length = markerEndPos - markerStartPos + 1;

		for (int i = 1; i < length; i++) {
			pos++;
			if (charStream.getChar(pos) != charStream.getChar(markerStartPos
					+ i)) {
				return 0;
			}
		}

		// accepted string for end - move on
		pos++;

		// we will accept ;<cr><lf> or <cr><lf>

		if (charStream.getChar(pos) == ';') {
			if (charStream.getChar(pos + 1) == LF_CHAR) {
				return pos - index;
			}
			if ((charStream.getChar(pos + 1) == CR_CHAR)
					&& charStream.getChar(pos + 2) == LF_CHAR) {
				return pos - index;
			}
			return 0;
		} else {
			if (charStream.getChar(pos) == LF_CHAR) {
				return pos - index;
			}
			if ((charStream.getChar(pos) == CR_CHAR)
					&& charStream.getChar(pos + 1) == LF_CHAR) {
				return pos - index;
			}
			return 0;
		}
	}

	/**
	 * Deletes <code>len</code> characters from the char array containing the
	 * script we are parsing, starting at <code>offset</code>.<br/> Used
	 * primarily when parsing strings that contain escaped characters like \n,
	 * \t, \043 and \xA9
	 * 
	 * @param offset
	 *            The offset to start deleting from
	 * @param len
	 *            The number of characters to delete
	 */
	private void deleteChars(int offset, int len) {
		charStream.deleteChars(offset, len);
	}

	/**
	 * Get a char array containing the entire contents of the char stream.
	 * 
	 * @return the contents of the char stream.
	 */
	@Override
	public char[] getContents() {
		return charStream.getContents();
	}

	/**
	 * Given a token, state whether its a keyword or not. This is very dependent
	 * on the fixed token types in scanner
	 * 
	 * @param t -
	 *            the token.
	 * @return boolean true if its a language element
	 */
	public boolean isLanguageElement(Token t) {
		int kind = t.getKind();
		switch (kind) {
		case T_INCLUDE: // 45 ,
		case T_INCLUDE_ONCE: // 46 ,
		case T_EVAL: // 47 ,
		case T_REQUIRE: // 48 ,
		case T_REQUIRE_ONCE: // 49 ,
		case T_COMMA: // 76 ,
		case T_LOGICAL_OR: // 109 ,
		case T_LOGICAL_XOR: // 110 ,
		case T_LOGICAL_AND: // 111 ,
		case T_PRINT: // 50 ,
		case T_EQUAL: // 10 ,
		case T_PLUS_EQUAL: // 12 ,
		case T_MINUS_EQUAL: // 13 ,
		case T_MUL_EQUAL: // 14 ,
		case T_DIV_EQUAL: // 15 ,
		case T_CONCAT_EQUAL: // 16 ,
		case T_MOD_EQUAL: // 17 ,
		case T_AND_EQUAL: // 18 ,
		case T_OR_EQUAL: // 19 ,
		case T_XOR_EQUAL: // 20 ,
		case T_SL_EQUAL: // 21 ,
		case T_SR_EQUAL: // 22 ,
		case T_QUESTION: // 77 ,
		case T_COLON: // 73 ,
		case T_BOOLEAN_OR: // 78 ,
		case T_BOOLEAN_AND: // 74 ,
		case T_VERTICAL_LINE: // 71 ,
		case T_CARET: // 69 ,
		case T_AMPERSAND: // 23 ,
		case T_IS_EQUAL: // 112 ,
		case T_IS_NOT_EQUAL: // 113 ,
		case T_SHR_EQUAL: // 114 ,
		case T_IS_IDENTICAL: // 115 ,
		case T_IS_NOT_IDENTICAL: // 116 ,
		case T_LT: // 117 ,
		case T_IS_SMALLER_OR_EQUAL: // 118 ,
		case T_GT: // 119 ,
		case T_IS_GREATER_OR_EQUAL: // 120 ,
		case T_SL: // 29 ,
		case T_SR: // 30 ,
		case T_PLUS: // 2 ,
		case T_MINUS: // 3 ,
		case T_PERIOD: // 31 ,
		case T_ASTERISK: // 24 ,
		case T_SLASH: // 25 ,
		case T_PERCENT: // 26 ,
		case T_EXCLAMATION: // 51 ,
		case T_INSTANCEOF: // 121 ,
		case T_TILDE: // 52 ,
		case T_INC: // 4 ,
		case T_DEC: // 5 ,
		case T_INT_CAST: // 53 ,
		case T_DOUBLE_CAST: // 54 ,
		case T_STRING_CAST: // 55 ,
		case T_ARRAY_CAST: // 56 ,
		case T_OBJECT_CAST: // 57 ,
		case T_BOOL_CAST: // 58 ,
		case T_UNSET_CAST: // 59 ,
		case T_FLOAT_CAST: // 60 ,
		case T_AT_MARK: // 61 ,
		case T_BRACKET_OPEN: // 79 ,
		case T_NEW: // 36 ,
		case T_CLONE: // 62 ,
		case T_EXIT: // 63 ,
		case T_DIE: // 64 ,
		case T_IF: // 80 ,
		case T_ELSEIF: // 122 ,
		case T_ELSE: // 123 ,
		case T_ENDIF: // 124 ,
		case T_ECHO: // 82 ,
		case T_DO: // 83 ,
		case T_WHILE: // 75 ,
		case T_ENDWHILE: // 126 ,
		case T_FOR: // 84 ,
		case T_ENDFOR: // 127 ,
		case T_FOREACH: // 85 ,
		case T_ENDFOREACH: // 128 ,
		case T_DECLARE: // 86 ,
		case T_ENDDECLARE: // 129 ,
		case T_AS: // 100 ,
		case T_SWITCH: // 87 ,
		case T_ENDSWITCH: // 130 ,
		case T_CASE: // 101 ,
		case T_DEFAULT: // 102 ,
		case T_BREAK: // 88 ,
		case T_CONTINUE: // 89 ,
		case T_FUNCTION: // 103 ,
		case T_CONST: // 131 ,
		case T_RETURN: // 90 ,
		case T_TRY: // 91 ,
		case T_CATCH: // 132 ,
		case T_THROW: // 92 ,
		case T_USE: // 93 ,
		case T_GLOBAL: // 94 ,
		case T_STATIC: // 72 ,
		case T_ABSTRACT: // 95 ,
		case T_FINAL: // 96 ,
		case T_PRIVATE: // 104 ,
		case T_PROTECTED: // 105 ,
		case T_PUBLIC: // 106 ,
		case T_VAR: // 133 ,
		case T_UNSET: // 97 ,
		case T_ISSET: // 66 ,
		case T_EMPTY: // 67 ,
		case T_HALT_COMPILER: // 134 ,
		case T_CLASS: // 107 ,
		case T_INTERFACE: // 135 ,
		case T_EXTENDS: // 136 ,
		case T_IMPLEMENTS: // 137 ,
		case T_OBJECT_OPERATOR: // 108 ,
		case T_DOUBLE_ARROW: // 138 ,
		case T_LIST: // 37 ,
		case T_ARRAY: // 34 ,
		case T_CLASS_C: // 38 ,
		case T_METHOD_C: // 39 ,
		case T_FUNC_C: // 40 ,
		case T_LINE_C: // 41 ,
		case T_FILE_C: // 42 ,
		case T_COMMENT: // 145 ,
		case T_DOC_COMMENT: // 139 ,
		case T_OPEN_TAG: // 146 ,
		case T_OPEN_TAG_WITH_ECHO: // 147 ,
		case T_CLOSE_TAG: // 148 ,
		case T_START_HEREDOC: // 68 ,
		case T_END_HEREDOC: // 140 ,
		case T_DOLLAR_OPEN_CURLY_BRACES: // 6 ,
		case T_CURLY_OPEN: // 9 ,
		case T_PAAMAYIM_NEKUDOTAYIM: // 141 ,
		case T_SEMICOLON: // 43 ,
		case T_LPAR: // 35 ,
		case T_RPAR: // 98 ,
		case T_CURLY_CLOSE: // 99 ,
		case T_BRACKET_CLOSE: // 142 ,
		case T_DOUBLE_QUOTE: // 27 ,
		case T_SINGLE_QUOTE: // 28 ,
		case T_BACK_QUOTE: // 44 ,
		case T_DOLLAR: // 8 ,
			return true;
		default:
			return false;
		}
	}

	/**
	 * Get a <code>LexStream</code> including comment, whitespace and start tag tokens.
	 * This method is intended to give a list of tokens that is closer to the php.net
	 * list of tokens for the same script. The returned LexStream will not be
	 * executable.
	 * 
	 * @param runtime .
	 * @param script .
	 * @return A <code>LexStream</code> containing all tokens
	 * @throws java.io.IOException exception
	 */
	public LexStream getAllTokens(RuntimeInterpreter runtime, byte[] script) throws java.io.IOException {
		this.retainAllTokens = true;
		LexStream lstream =  this.scan(runtime, script, "dummy.php");
		
		// create a list of tokens suitable for output
		ArrayList<Token> outputList = new ArrayList<Token>();
		
		// discard the first token, which is always invalid.
		lstream.tokens.remove(0);
		
		int index = 0;
		int line = 0;
		
		for (Token token : lstream.tokens) {

			if (token.getStartOffset() > index) {
				// construct missing whitespace token
				// (whitespace is discarded by the scanner)
				Token whitespaceToken = new Token();
				whitespaceToken.setKind(Token.T_WHITESPACE);
				whitespaceToken.setLine(line);
				whitespaceToken.setStartOffset(index);
				whitespaceToken.setEndOffset(token.getStartOffset() - 1);
				whitespaceToken.setScanner(this);
				outputList.add(whitespaceToken);
			}
			
			if (token.getKind() != Token.$eof) {
				// don't include $eof token
				outputList.add(token);
			}
			index = token.getEndOffset() + 1;
			line = token.getLine();
		}
		
		// replace token list
		lstream.tokens = outputList;
		
		return lstream;
	}
	
}
