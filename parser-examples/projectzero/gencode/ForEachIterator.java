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

import com.ibm.p8.engine.core.ErrorType;
import com.ibm.p8.engine.core.PHPValue;
import com.ibm.p8.engine.core.RuntimeInterpreter;
import com.ibm.p8.engine.core.array.ArrayFacade;
import com.ibm.p8.engine.core.array.ArrayIterator;
import com.ibm.p8.engine.core.array.ArrayKey;
import com.ibm.p8.engine.core.array.ArrayNode;
import com.ibm.p8.engine.core.object.ObjectFacade;
import com.ibm.p8.engine.core.object.ObjectIterator;
import com.ibm.p8.engine.core.object.PHPClass;
import com.ibm.p8.engine.core.types.PHPArray;
import com.ibm.p8.engine.library.spl.Traversable;

/**
 * Helper class implementing and keeping state for FE_INIT and FE_NEXT operations.
 *
 */
public final class ForEachIterator {

	private PHPValue iteratedEntity;
	private final boolean assignRef;
	private final boolean arrowSyntax;
	
	/**
	 * Mode.
	 */
	private enum IterationMode { ARRAY, TRAVERSABLE_OBJECT, PLAIN_OBJECT };
	private IterationMode mode;
	
	private PHPValue originalIteratedEntity;
	private ArrayIterator arrayIterator;
	private ObjectIterator objectIterator;
	private PHPArray propertyValueContainer;
	
	private PHPValue myValue; // current value 
	private PHPValue keyValue;   // current key (for arrowSyntax)
	
	/**
	 * Constructor.
	 * @param inIteratedEntity .
	 * @param inArrowSyntax .
	 * @param inAssignRef .
	 */
	public ForEachIterator(PHPValue inIteratedEntity, boolean inArrowSyntax, boolean inAssignRef) {
		iteratedEntity = inIteratedEntity;
		arrowSyntax = inArrowSyntax;
		assignRef = inAssignRef;	
	}
	
	/**
	 * @param runtime .
	 * @return true if initialisation was successful
	 */
	public boolean init(RuntimeInterpreter runtime) {
		
		switch (iteratedEntity.getType()) {
		case PHPTYPE_ARRAY:
			mode = IterationMode.ARRAY;
			return initArray(runtime) && nextArray(runtime, false);
		case PHPTYPE_OBJECT:
			if (ObjectFacade.instanceOf(runtime, iteratedEntity, Traversable.PHP_CLASS_NAMESTRING)) {
				mode = IterationMode.TRAVERSABLE_OBJECT;
				return initTraversable(runtime) && nextTraversable(runtime, false);
			} else {
				mode = IterationMode.PLAIN_OBJECT;
				return initPlain(runtime) && nextPlain(runtime, false);
			}
		default:
			// Stop here if it is not an array/object.
			runtime.raiseExecError(ErrorType.E_WARNING, null, "Foreach.InvalidArgument", null);
			return false;
		}
	}
	
	/**
	 * @param runtime .
	 * @return true if initialisation was successful
	 */
	private boolean initArray(RuntimeInterpreter runtime) {
		if (assignRef && !iteratedEntity.isReferenced()) {
			runtime.raiseExecError(ErrorType.E_COMPILE_ERROR, null, "Foreach.RefToConstantArray", null);
			return false;
		}
		
		// The internal array pointer is automatically reset to the first element of the array (see php.net/foreach) 
		ArrayFacade.reset(runtime, iteratedEntity);
		
		// Foreach operates on the array itself if it is referenced, or a copy if it is not (see php.net/foreach).
		// Furthermore, since php 5.2.2, foreach operates on the array itself when using the &$value notation (not documented).
		originalIteratedEntity = iteratedEntity;		
		if (!originalIteratedEntity.isRef() && !assignRef) {
			iteratedEntity = originalIteratedEntity.clone();
		}
		
		arrayIterator = iteratedEntity.getArray().iterator();
		return true;
	}
	
	/**
	 * @param runtime .
	 * @return true if successful
	 */
	private boolean initPlain(RuntimeInterpreter runtime) {
		propertyValueContainer = ObjectFacade.getProperties(iteratedEntity);
		propertyValueContainer.reset();
		arrayIterator = propertyValueContainer.iterator();
		return true;
	}
	
	
	/**
	 * @param runtime .
	 * @return true if successful
	 */
	private boolean initTraversable(RuntimeInterpreter runtime) {
		
		PHPClass traverableClass = ObjectFacade.getPHPClass(iteratedEntity);
		assert traverableClass.isIterable(runtime);
		objectIterator = traverableClass.getObjectIterator(runtime, iteratedEntity);

		objectIterator.rewind(runtime);
		return true;
	}
	
	/**
	 * 
	 * @param runtime .
	 * @return true until the end of the loop
	 */
	public boolean next(RuntimeInterpreter runtime) {
		switch(mode) {
		case ARRAY:
			return nextArray(runtime, true);
		case TRAVERSABLE_OBJECT:
			return nextTraversable(runtime, true);
		case PLAIN_OBJECT:
			return nextPlain(runtime, true);
		default:
			assert false;
			return false;
		}
	}

	/**
	 * @param runtime .
	 * @param doNext .
	 * @return true if value available
	 */
	private boolean nextArray(RuntimeInterpreter runtime, boolean doNext) {
		
		if (doNext) {
			arrayIterator.next();
			ArrayFacade.next(runtime, originalIteratedEntity);
		}
		
		if (arrayIterator.hasCurrent()) {
			// Confirm iterated entity is still an array/object.
			if (iteratedEntity.getType() != PHPValue.Types.PHPTYPE_ARRAY 
					&& iteratedEntity.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
				runtime.raiseExecError(ErrorType.E_WARNING, null, "Foreach.InvalidArgument", null);
				return false;
			}
	
			// Get key and push if required
			Object key = arrayIterator.getKey();
			
			// Get value
			if (assignRef && ArrayFacade.isKeyExists(runtime, originalIteratedEntity, key)) {
				// If the value is a reference, it should refer to the original
				// array assuming the key is still available.
				myValue = ArrayFacade.get(runtime, originalIteratedEntity, key, false, true, false);
				// will be assigned by ref
			} else {
				// If the value is not a reference or the key is no longer in the
				// original array, value is obtained from the iterated array.
				myValue = ArrayFacade.get(runtime, iteratedEntity, key, false, false, false);
				if (assignRef) {
					myValue = myValue.cloneIfRef(); // ensure assignment is effectively by value
				}
			}

			if (arrowSyntax) {
				keyValue = ArrayKey.getBoxedKey(key);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @param runtime .
	 * @param doNext .
	 * @return true if value available
	 */
	private boolean nextTraversable(RuntimeInterpreter runtime, boolean doNext) {

		if (doNext) {
			objectIterator.next(runtime);
		}
		
		if (objectIterator.valid(runtime)) {
			myValue = objectIterator.current(runtime, assignRef);
			
			if (arrowSyntax) {
				keyValue = objectIterator.key(runtime);
			}
			return true;
		}
		return false;	
	}
	
	/**
	 * @param runtime .
	 * @param doNext .
	 * @return true if value available
	 */
	private boolean nextPlain(RuntimeInterpreter runtime, boolean doNext) {
		
		if (doNext) {
			arrayIterator.next();
			propertyValueContainer.next();
		}
		
		while (arrayIterator.hasCurrent()) {
			// Confirm iterated entity is still an array/object.
			if (iteratedEntity.getType() != PHPValue.Types.PHPTYPE_ARRAY 
					&& iteratedEntity.getType() != PHPValue.Types.PHPTYPE_OBJECT) {
				runtime.raiseExecError(ErrorType.E_WARNING, null, "Foreach.InvalidArgument", null);
				return false;
			}
		
			ArrayNode property = arrayIterator.getCurrentNode();

			if (!property.isVisible(runtime)) {
				// Skip properties that are not visible from the current context.
				arrayIterator.next();
				propertyValueContainer.next();
				continue;
			}

			// set value
			if (assignRef) {
				myValue = arrayIterator.getValueForWriting();
			} else {
				myValue = arrayIterator.getValue();
			}

			// set key, if required
			if (arrowSyntax) {
				keyValue = PHPValue.createString(property.getPlainName());
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @return the value to assign to the iteration variable
	 */
	public PHPValue getValue() {
		return myValue;
	}
	
	/**
	 * @return the value to assign to the iteration key (arrowSyntax only)
	 */
	public PHPValue getKey() {
		return keyValue;
	}
	
	/**
	 * @return true if this iterator supports arrow syntax (provides keys)
	 */
	public boolean isArrow() {
		return arrowSyntax;
	}
}
