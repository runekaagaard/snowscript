/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.parser;

import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.Location;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Parse scope.
 */
public class FunctionScope extends Scope {
  private final static L10N L = new L10N(FunctionScope.class);

  private ExprFactory _exprFactory;

  private HashMap<String,Function> _functionMap
  = new HashMap<String,Function>();
  
  private HashMap<String,InterpretedClassDef> _classMap
    = new HashMap<String,InterpretedClassDef>();
  
  private HashMap<String,InterpretedClassDef> _conditionalClassMap;
  
  private HashMap<String,Function> _conditionalFunctionMap;

  FunctionScope(ExprFactory exprFactory, Scope parent)
  {
    super(parent);
    
    _exprFactory = exprFactory;
  }

  /*
   * Returns true if scope is local to a function.
   */
  public boolean isFunction()
  {
    return true;
  }
  
  /**
   * Adds a function.
   */
  public void addFunction(String name,
                          Function function,
                          boolean isTop)
  {
    name = name.toLowerCase();
    
    if (_functionMap.get(name) == null)
      _functionMap.put(name, function);
    
    //_parent.addConditionalFunction(name, function);
    _parent.addFunction(name, function, false);
  }
  
  /*
   *  Adds a function defined in a conditional block.
   */
  @Override
  protected void addConditionalFunction(String name, Function function)
  {
    if (_conditionalFunctionMap == null)
      _conditionalFunctionMap = new HashMap<String,Function>(4);

    _conditionalFunctionMap.put(function.getCompilationName(), function);
    
    _parent.addConditionalFunction(name, function);
  }

  /**
   * Adds a class
   */
  public InterpretedClassDef addClass(Location location,
                                      String name,
                                      String parentName,
                                      ArrayList<String> ifaceList,
                                      int index,
                                      boolean isTop)
  {
    InterpretedClassDef existingClass = _classMap.get(name);

    String []ifaceArray = new String[ifaceList.size()];
    ifaceList.toArray(ifaceArray);

    InterpretedClassDef cl
      = _exprFactory.createClassDef(location,
                                    name, parentName, ifaceArray,
                                    index);
    
    if (existingClass == null)
      _classMap.put(name, cl);
      
    _parent.addConditionalClass(cl);

    return cl;
  }
  
  /*
   *  Adds a conditional class.
   */
  protected void addConditionalClass(InterpretedClassDef def)
  {
    if (_conditionalClassMap == null)
      _conditionalClassMap = new HashMap<String,InterpretedClassDef>(1);
    
    _conditionalClassMap.put(def.getCompilationName(), def);
    
    _parent.addConditionalClass(def);
  }
}

