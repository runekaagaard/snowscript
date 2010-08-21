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

/**
 * 
 */
public class ForEachParams {
    private boolean bool1;
    private boolean bool2;
   
    /**
     * @param b1 .
     * @param b2 .
     */
    public ForEachParams(boolean b1, boolean b2) {
    	bool1 = b1;
    	bool2 = b2;
    }
    public boolean getBool1() {
    	return bool1;
    }
    public boolean getBool2() {
    	return bool2;
    }
}
