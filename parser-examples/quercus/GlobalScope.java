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
public class GlobalScope extends Scope {
  private final static L10N L = new L10N(GlobalScope.class);

  private ExprFactory _exprFactory;

  private HashMap<String,Function> _functionMap
    = new HashMap<String,Function>();

  private ArrayList<Function> _functionList
    = new ArrayList<Function>();
  
  private HashMap<String,Function> _conditionalFunctionMap
    = new HashMap<String,Function>();
  
  private HashMap<String,InterpretedClassDef> _classMap
    = new HashMap<String,InterpretedClassDef>();
  
  private ArrayList<InterpretedClassDef> _classList
    = new ArrayList<InterpretedClassDef>();
  
  private HashMap<String,InterpretedClassDef> _conditionalClassMap
    = new HashMap<String,InterpretedClassDef>();

  GlobalScope(ExprFactory exprFactory)
  {
    _exprFactory = exprFactory;
  }

  /*
   * Returns true if scope is global.
   */
  public boolean isGlobal()
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
    if (isTop)
      _functionMap.put(name.toLowerCase(), function);
    
    _functionList.add(function);
  }
  
  /*
   *  Adds a function defined in a conditional block.
   */
  protected void addConditionalFunction(String name, Function function)
  {
    _conditionalFunctionMap.put(name, function);
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
    InterpretedClassDef cl = null;

    if (isTop)
      cl = _classMap.get(name);

    if (cl == null) {
      String []ifaceArray = new String[ifaceList.size()];
      ifaceList.toArray(ifaceArray);

      cl = _exprFactory.createClassDef(location,
                                       name, parentName, ifaceArray,
                                       index);

      if (isTop) {
        cl.setTopScope(true);
        
        _classMap.put(name, cl);
      }
    }
    else {
      // class statically redeclared
      // XXX: should throw a runtime error?
      
      // dummy classdef for parsing only
      cl = _exprFactory.createClassDef(location,
                                       name, parentName, new String[0],
                                       index);
    }
    
    _classList.add(cl);

    return cl;
  }
  
  /*
   *  Adds a class
   */
  protected void addConditionalClass(InterpretedClassDef def)
  {
    _classList.add(def);
  }

  /**
   * Returns the function map.
   */
  public HashMap<String,Function> getFunctionMap()
  {
    return _functionMap;
  }

  /**
   * Returns the function list.  The function list may include multiple
   * functions with the same name, e.g. from inside conditionals.
   */
  public ArrayList<Function> getFunctionList()
  {
    return _functionList;
  }
  
  /**
   * Returns the conditional function map.
   */
  public HashMap<String,Function> getConditionalFunctionMap()
  {
    return _conditionalFunctionMap;
  }

  /**
   * Returns the class map.
   */
  public HashMap<String,InterpretedClassDef> getClassMap()
  {
    return _classMap;
  }

  /**
   * Returns the list of defined classes.  The class list may include
   * conditional classes.
   */
  public ArrayList<InterpretedClassDef> getClassList()
  {
    return _classList;
  }
  
  /**
   * Returns the conditional class map.
   */
  public HashMap<String,InterpretedClassDef> getConditionalClassMap()
  {
    return _conditionalClassMap;
  }
}

