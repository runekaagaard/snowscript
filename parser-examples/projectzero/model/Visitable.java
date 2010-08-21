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
package com.ibm.p8.engine.parser.model;

import com.ibm.p8.engine.ast.AstVisitor;

/**
 * Marks a class as visitable.
 */
public interface Visitable {
	/**
	 * Accept a visitor.
	 * @param v the visitor
	 */
	void accept(AstVisitor v);
}
