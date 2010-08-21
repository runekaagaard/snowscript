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

import java.util.logging.Logger;

import com.ibm.p8.engine.core.FatalError;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.StringEncoder;
import com.ibm.p8.engine.core.ThreadLocalRuntime;
import com.ibm.p8.engine.parser.core.Token;
import com.ibm.p8.utilities.log.P8LogManager;
import com.ibm.phpj.logging.SAPIComponent;
import com.ibm.phpj.logging.SAPILevel;

/**
 * Helper class used to translate java string with un-processed
 * escapes, into PHP byte based strings.
 * 
 */
final class StringEscapeProcessor {

	private static final Logger LOGGER = P8LogManager._instance
	.getLogger(SAPIComponent.Parser);
	
	private int stringPosition = 0;

	private char[] inputChars = null;

	private int length = 0;

	private PHPValue returnVal = null;

	private PHPToken.StringType stringType = null;

	private PHPToken token = null;
	
	/**
	 * Create a PHPString (wrapped in a PHPValue) from a Token that holds a Java
	 * string with escaped characters, using the runtime encoding to encode the
	 * binary representation that backs the PHPString.
	 * 
	 * @param token
	 *            the token representing the string to be processed
	 * @return PHPValue containing the new PHPString
	 */
	public PHPValue phpStringFromToken(Token token) {
		return this.process(token, false);
	}
	
	/**
	 * Create a PHPString (wrapped in a PHPValue) from a Token that
	 * holds a Java string with escaped characters, using the script
	 * encoding to encode the binary representation that backs the PHPString.
	 * 
	 * @param token the token representing the string to be processed
	 * @return PHPValue containing the new PHPString
	 */
	public PHPValue scriptEncodePHPStringFromToken(Token token) {
		return this.process(token, true);
	}
	
	/**
	 * Turn a java string with escapes into a PHPString wrapped in a
	 * PHPValue.
	 * 
	 * @param newToken
	 *            the token representing the string to be processed
	 * @param useScriptEncoding
	 *            true to use the script encoding instead of the runtime
	 *            encoding
	 * @return the PHPValue
	 */
	private PHPValue process(Token newToken, boolean useScriptEncoding) {
		if (newToken instanceof PHPToken) {
			token = (PHPToken) newToken;
		} else {
			// Incorrect token type. Found Token, expected PHPToken
			if (LOGGER.isLoggable(SAPILevel.SEVERE)) {
				LOGGER.log(SAPILevel.SEVERE, "2001");
			}
			// There is nothing a user can do - this is a real runtime
			// fault.
			throw new FatalError(
					"Incorrect token type. Found Token, expected PHPToken");
		}
		
		stringPosition = 0;
		inputChars = token.toString().toCharArray();
		length = inputChars.length;
		returnVal = null;
		PHPValue lengthenedString = null;
		stringType = token.getStringType();

		// Iterate over the whole string
		while (stringPosition < length) {
			if (!atBinaryEscape()) {
				// We have text contents to deal with
				int startPos = stringPosition;
				while (stringPosition < length && !atBinaryEscape()) {
					replaceTextEscape();
					stringPosition++;
				}
				if (stringPosition > startPos) {
					String javaString = String.valueOf(inputChars, startPos,
							stringPosition - startPos);
					lengthenedString = this.createString(javaString,
							useScriptEncoding);
				}
				if (returnVal == null) {
					returnVal = lengthenedString;
				} else {
					returnVal = PHPValue.createString(returnVal,
							lengthenedString);
				}
			} else {
				// We have binary escapes to deal with
				Byte replaceByte = null;
				byte[] replaceBuffer = new byte[length];
				int bufferPosition = 0;
				do {
					replaceByte = processBinaryEscape();
					if (replaceByte != null) {
						replaceBuffer[bufferPosition] = replaceByte;
						bufferPosition++;
						stringPosition++;
					}
				} while (replaceByte != null && stringPosition < length);
				if (bufferPosition > 0) {
					// We have some bytes to add
					lengthenedString = PHPValue.createString(replaceBuffer,
							0, bufferPosition);
				}
				if (returnVal == null) {
					returnVal = lengthenedString;
				} else {
					returnVal = PHPValue.createString(returnVal,
							lengthenedString);
				}
			}
		} // end while(stringPosition < length)

		return returnVal;
	}

	/**
	 * Create a new PHPString from a Java String.
	 * 
	 * @param string
	 *            the source String
	 * @param useScriptEncoding
	 *            true to use the script encoding instead of the runtime
	 *            encoding
	 * @return the new PHPString wrapped in a PHPValue
	 */
	private PHPValue createString(String string, boolean useScriptEncoding) {
		if (useScriptEncoding) {
			StringEncoder scriptEncoder = ThreadLocalRuntime
					.getRuntimeInterpreter().getScriptEncoder();
			byte[] encodedString = scriptEncoder.encode(string);
			return PHPValue.createString(encodedString);
		} else {
			return PHPValue.createString(string);
		}
	}
	
	/**
	 * Look at the current position for a valid octal or hex escape
	 * sequence, and return the appropriate replacement value.
	 * <code>stringPosition</code> is incremented to move to the end of
	 * the escape.
	 * 
	 * @return The replacement value for the escape.
	 */
	private Byte processBinaryEscape() {
		if (inputChars[stringPosition] != PHPScanner.BACKSLASH_CHAR) {
			return null;
		}

		// is there any string left?
		if (length < stringPosition + 2) {
			stringPosition++;
			return null;
		}

		if (PHPScanner.isOctalDigit(inputChars[stringPosition + 1])) {
			int octLength = 1;
			if (length > stringPosition + 2
					&& PHPScanner.isOctalDigit(inputChars[stringPosition + 2])) {
				octLength++;
				if (length > stringPosition + 3
						&& PHPScanner.isOctalDigit(inputChars[stringPosition + 3])) {
					octLength++;
				}
			}
			String octString = new String(inputChars, stringPosition + 1,
					octLength);
			stringPosition = stringPosition + octLength;
			return (byte) Integer.parseInt(octString, PHPScanner.OCTAL_RADIX);
		}

		if (inputChars[stringPosition + 1] == 'X'
				|| inputChars[stringPosition + 1] == 'x') {
			// potential hex escape
			if (length > stringPosition + 2
					&& PHPScanner.isHexDigit(inputChars[stringPosition + 2])) {
				if (length > stringPosition + 3
						&& PHPScanner.isHexDigit(inputChars[stringPosition + 3])) {
					String hexString = new String(inputChars,
							stringPosition + 2, 2);
					stringPosition += 3;
					return (byte) Integer.parseInt(hexString, 16);

				} else {
					String hexString = new String(inputChars,
							stringPosition + 2, 1);
					stringPosition += 2;
					return (byte) Integer.parseInt(hexString, 16);
				}
			} else {
				stringPosition++; /* move on - its not an escape */
			}
		}

		// No valid escape found.
		return null;
	}

	/**
	 * Test if the current position in the string buffer is the start of a
	 * valid octal or hex escape.
	 * 
	 * @return whether it is or not.
	 */
	private boolean atBinaryEscape() {

		if (inputChars[stringPosition] != PHPScanner.BACKSLASH_CHAR) {
			return false;
		}

		// no more characters
		if (stringPosition > inputChars.length - 2) {
			return false;
		}
		if (PHPScanner.isOctalDigit(inputChars[stringPosition + 1])) {
			// valid octal escape
			return true;
		}

		if (inputChars[stringPosition + 1] == 'X'
				|| inputChars[stringPosition + 1] == 'x') {
			// potential hex escape

			// no more chars after the X
			if (stringPosition > inputChars.length - 3) {
				return false;
			}
			if (PHPScanner.isHexDigit(inputChars[stringPosition + 2])) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Replace a character escape sequence with the appropriate character.
	 * Character escapes are all escapes apart from hex and octal (binary)
	 * sequences.
	 * 
	 * @return true if we have replace an escape.
	 */
	private boolean replaceTextEscape() {
		if (inputChars[stringPosition] == PHPScanner.BACKSLASH_CHAR) {
			char replacementChar;
			// could be a trailing slash
			if (stringPosition > inputChars.length - 2) {
				return false;
			}
			switch (inputChars[stringPosition + 1]) {
			case PHPScanner.DOUBLEQUOTE_CHAR:
				if (stringType != PHPToken.StringType.doubleQuoted) {
					return false;
				}
				replacementChar = PHPScanner.DOUBLEQUOTE_CHAR;
				break;
			case 'n':
				replacementChar = PHPScanner.LF_CHAR;
				break;
			case 'f':
				replacementChar = PHPScanner.FORMFEED_CHAR;
				break;	
			case 'v':
				replacementChar = PHPScanner.VERTICAL_TAB_CHAR;
				break;		
			case 't':
				replacementChar = PHPScanner.TAB_CHAR;
				break;
			case 'r':
				replacementChar = PHPScanner.CR_CHAR;
				break;
			case PHPScanner.BACKSLASH_CHAR:
				replacementChar = PHPScanner.BACKSLASH_CHAR;
				break;
			case PHPScanner.DOLLAR_CHAR:
				replacementChar = PHPScanner.DOLLAR_CHAR;
				break;
			default:
				return false;
			}

			inputChars[stringPosition] = replacementChar;
			if (stringPosition + 2 < length) {
				System.arraycopy(inputChars, stringPosition + 2,
						inputChars, stringPosition + 1, length
								- (stringPosition + 2));
			}
			length = length - 1;
			return true;
		}

		return false;
	}
} // End of StringEscapeProcessor

