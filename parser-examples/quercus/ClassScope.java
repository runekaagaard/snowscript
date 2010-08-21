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

import com.caucho.quercus.env.FieldVisibility;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.Location;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Class parse scope.
 */
public class ClassScope extends Scope
{
  private final static L10N L = new L10N(ClassScope.class);
  
  private final InterpretedClassDef _cl;

  public ClassScope(InterpretedClassDef cl)
  {
    _cl = cl;
  }
  
  /**
   * Returns true if scope is within a class.
   */
  public boolean isClass()
  {
    return true;
  }
  
  /**
   * Returns true for an abstract scope, e.g. an abstract class or an
   * interface.
   */
  public boolean isAbstract()
  {
    return _cl.isAbstract() || _cl.isInterface();
  }
  
  /**
   * Adds a function.
   */
  @Override
  public void addFunction(String name,
                          Function function,
                          boolean isTop)
  {
    _cl.addFunction(name, function);
  }
  
  /*
   *  Adds a function defined in a conditional block.
   */
  @Override
  public void addConditionalFunction(String name, Function function)
  {
    //addFunction(name, function);
  }
  
  /**
   * Adds a value
   */
  public void addVar(StringValue name,
                     Expr value,
                     FieldVisibility visibility,
                     String comment)
  {
    _cl.addValue(name, value, visibility, comment);
  }
  
  /**
   * Adds a static value
   */
  public void addStaticVar(StringValue name, Expr value, String comment)
  {
    _cl.addStaticValue(name, value, comment);
  }
  
  /**
   * Adds a constant value
   */
  public void addConstant(String name, Expr value)
  {
    _cl.addConstant(name, value);
  }

  /**
   * Adds a class
   */
  @Override
  public InterpretedClassDef addClass(Location location, String name,
                                      String parentName,
                                      ArrayList<String> ifaceList,
                                      int index,
                                      boolean isTop)
  {
    throw new UnsupportedOperationException();
  }
  
  /*
   *  Adds a conditional class.
   */
  protected void addConditionalClass(InterpretedClassDef def)
  {
    throw new UnsupportedOperationException();
  }
}

