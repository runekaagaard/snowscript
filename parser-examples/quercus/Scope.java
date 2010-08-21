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

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.Location;

import java.util.ArrayList;

/**
 * Parse scope.
 */
abstract public class Scope {
  
  protected Scope _parent;
  
  public Scope()
  {
  }
  
  public Scope(Scope parent)
  {
    _parent = parent;
  }
  
  /**
   * Returns true for an abstract scope, e.g. an abstract class or an
   * interface.
   */
  public boolean isAbstract()
  {
    return false;
  }
  
  /*
   * Returns true if scope is global.
   */
  public boolean isGlobal()
  {
    return false;
  }
  
  /*
   * Returns true if scope is within a class.
   */
  public boolean isClass()
  {
    return false;
  }
  
  /*
   * Returns true if scope is local to a function.
   */
  public boolean isFunction()
  {
    return false;
  }
  
  /*
   * Returns true if scope is local to an IF statement.
   */
  public boolean isIf()
  {
    return false;
  }
  
  /*
   * Returns true if scope is local to a switch case statement.
   */
  public boolean isSwitchCase()
  {
    return false;
  }
  
  /*
   * Returns true if scope is local to a while statement.
   */
  public boolean isWhile()
  {
    return false;
  }
  
  /*
   * Returns true if scope is local to a try statement.
   */
  public boolean isTry()
  {
    return false;
  }
  
  /*
   * Returns the parent scope.
   */
  public Scope getParent()
  {
    return _parent;
  }
  
  /**
   * Adds a constant.
   */
  public void addConstant(String name, Expr value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Adds a function.
   */
  abstract public void addFunction(String name,
                                   Function function,
                                   boolean isTop);

  /*
   *  Adds a function defined in a conditional block.
   */
  protected void addConditionalFunction(String name,
                                        Function function)
  {
    addConditionalFunction(function);
  }

  /*
   *  Adds a function defined in a conditional block.
   */
  protected void addConditionalFunction(Function function)
  {
  }
  
  /**
   * Adds a class.
   */
  abstract public InterpretedClassDef addClass(Location location,
                                               String name,
                                               String parent,
                                               ArrayList<String> ifaceList,
                                               int index,
                                               boolean isTop);
  
  /*
   *  Adds a conditional class.
   */
  abstract protected void addConditionalClass(InterpretedClassDef def);
}

