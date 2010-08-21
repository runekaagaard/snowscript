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

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.*;
import com.caucho.quercus.function.*;
import com.caucho.quercus.program.*;
import com.caucho.quercus.statement.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.CharConversionException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Parses a PHP program.
 */
public class QuercusParser {
  private final static L10N L = new L10N(QuercusParser.class);

  private final static int M_STATIC = 0x1;
  private final static int M_PUBLIC = 0x2;
  private final static int M_PROTECTED = 0x4;
  private final static int M_PRIVATE = 0x8;
  private final static int M_FINAL = 0x10;
  private final static int M_ABSTRACT = 0x20;
  private final static int M_INTERFACE = 0x40;

  private final static int IDENTIFIER = 256;
  private final static int STRING = 257;
  private final static int LONG = 258;
  private final static int DOUBLE = 259;
  private final static int LSHIFT = 260;
  private final static int RSHIFT = 261;
  private final static int PHP_END = 262;
  private final static int EQ = 263;
  private final static int DEREF = 264;
  private final static int LEQ = 268;
  private final static int GEQ = 269;
  private final static int NEQ = 270;
  private final static int EQUALS = 271;
  private final static int NEQUALS = 272;
  private final static int C_AND = 273;
  private final static int C_OR = 274;

  private final static int PLUS_ASSIGN = 278;
  private final static int MINUS_ASSIGN = 279;
  private final static int APPEND_ASSIGN = 280;
  private final static int MUL_ASSIGN = 281;
  private final static int DIV_ASSIGN = 282;
  private final static int MOD_ASSIGN = 283;
  private final static int AND_ASSIGN = 284;
  private final static int OR_ASSIGN = 285;
  private final static int XOR_ASSIGN = 286;
  private final static int LSHIFT_ASSIGN = 287;
  private final static int RSHIFT_ASSIGN = 288;

  private final static int INCR = 289;
  private final static int DECR = 290;

  private final static int SCOPE = 291;
  private final static int ESCAPED_STRING = 292;
  private final static int HEREDOC = 293;
  private final static int ARRAY_RIGHT = 294;
  private final static int SIMPLE_STRING_ESCAPE = 295;
  private final static int COMPLEX_STRING_ESCAPE = 296;

  private final static int BINARY = 297;
  private final static int SIMPLE_BINARY_ESCAPE = 298;
  private final static int COMPLEX_BINARY_ESCAPE = 299;

  private final static int FIRST_IDENTIFIER_LEXEME = 512;
  private final static int ECHO = 512;
  private final static int NULL = 513;
  private final static int IF = 514;
  private final static int WHILE = 515;
  private final static int FUNCTION = 516;
  private final static int CLASS = 517;
  private final static int NEW = 518;
  private final static int RETURN = 519;
  private final static int VAR = 520;
  private final static int PRIVATE = 521;
  private final static int PROTECTED = 522;
  private final static int PUBLIC = 523;
  private final static int FOR = 524;
  private final static int DO = 525;
  private final static int BREAK = 526;
  private final static int CONTINUE = 527;
  private final static int ELSE = 528;
  private final static int EXTENDS = 529;
  private final static int STATIC = 530;
  private final static int INCLUDE = 531;
  private final static int REQUIRE = 532;
  private final static int INCLUDE_ONCE = 533;
  private final static int REQUIRE_ONCE = 534;
  private final static int UNSET = 535;
  private final static int FOREACH = 536;
  private final static int AS = 537;
  private final static int TEXT = 538;
  private final static int ISSET = 539;
  private final static int SWITCH = 540;
  private final static int CASE = 541;
  private final static int DEFAULT = 542;
  private final static int EXIT = 543;
  private final static int GLOBAL = 544;
  private final static int ELSEIF = 545;
  private final static int PRINT = 546;
  private final static int SYSTEM_STRING = 547;
  private final static int SIMPLE_SYSTEM_STRING = 548;
  private final static int COMPLEX_SYSTEM_STRING = 549;
  private final static int TEXT_ECHO = 550;
  private final static int ENDIF = 551;
  private final static int ENDWHILE = 552;
  private final static int ENDFOR = 553;
  private final static int ENDFOREACH = 554;
  private final static int ENDSWITCH = 555;

  private final static int XOR_RES = 556;
  private final static int AND_RES = 557;
  private final static int OR_RES = 558;
  private final static int LIST = 559;

  private final static int THIS = 560;
  private final static int TRUE = 561;
  private final static int FALSE = 562;
  private final static int CLONE = 563;
  private final static int INSTANCEOF = 564;
  private final static int CONST = 565;
  private final static int ABSTRACT = 566;
  private final static int FINAL = 567;
  private final static int DIE = 568;
  private final static int THROW = 569;
  private final static int TRY = 570;
  private final static int CATCH = 571;
  private final static int INTERFACE = 572;
  private final static int IMPLEMENTS = 573;

  private final static int IMPORT = 574;
  private final static int TEXT_PHP = 575;
  private final static int NAMESPACE = 576;
  private final static int USE = 577;

  private final static int LAST_IDENTIFIER_LEXEME = 1024;

  private final static IntMap _insensitiveReserved = new IntMap();
  private final static IntMap _reserved = new IntMap();

  private QuercusContext _quercus;

  private Path _sourceFile;
  private int _sourceOffset; // offset into the source file for the first line

  private ParserLocation _parserLocation = new ParserLocation();

  private ExprFactory _factory;

  private boolean _hasCr;

  private int _peek = -1;
  private ReadStream _is;
  private String _encoding;

  private CharBuffer _sb = new CharBuffer();
  
  private String _namespace = "";
  private HashMap<String,String> _namespaceUseMap
    = new HashMap<String,String>();

  private int _peekToken = -1;
  private String _lexeme = "";
  private String _heredocEnd = null;

  private GlobalScope _globalScope;

  private boolean _returnsReference = false;

  private Scope _scope;
  private InterpretedClassDef _classDef;

  private FunctionInfo _function;

  private boolean _isTop;

  private boolean _isNewExpr;
  private boolean _isIfTest;

  private int _classesParsed;
  private int _functionsParsed;

  private ArrayList<String> _loopLabelList = new ArrayList<String>();
  private int _labelsCreated;

  private String _comment;

  public QuercusParser(QuercusContext quercus)
  {
    _quercus = quercus;
    
    if (quercus == null)
      _factory = ExprFactory.create();
    else
      _factory = quercus.createExprFactory();

    _globalScope = new GlobalScope(_factory);
    _scope = _globalScope;
  }

  public QuercusParser(QuercusContext quercus,
                       Path sourceFile,
                       ReadStream is)
  {
    this(quercus);

    if (quercus == null || quercus.isUnicodeSemantics())
      init(sourceFile, is, "UTF-8");
    else
      init(sourceFile, is, "ISO-8859-1");
  }

  private void init(Path sourceFile)
    throws IOException
  {
    init(sourceFile, sourceFile.openRead(), "UTF-8");
  }

  private void init(Path sourceFile, ReadStream is, String encoding)
  {
    _is = is;
    _encoding = encoding;

    if (sourceFile != null) {
      _parserLocation.setFileName(sourceFile);
      _sourceFile = sourceFile;
    }
    else {
      _parserLocation.setFileName("eval:");

      // php/2146
      _sourceFile = new NullPath("eval:");
    }

    _parserLocation.setLineNumber(1);

    _peek = -1;
    _peekToken = -1;
  }

  public void setLocation(String fileName, int line)
  {
    _parserLocation.setFileName(fileName);
    _parserLocation.setLineNumber(line);
    
    if (line > 0)
      _sourceOffset = 1 - line;
  }

  public static QuercusProgram parse(QuercusContext quercus,
                                     Path path,
                                     String encoding)
    throws IOException
  {
    ReadStream is = path.openRead();

    try {
      is.setEncoding(encoding);

      QuercusParser parser;
      parser = new QuercusParser(quercus, path, is);

      return parser.parse();
    } finally {
      is.close();
    }
  }

  public static QuercusProgram parse(QuercusContext quercus,
                                     Path path,
                                     String encoding,
                                     String fileName,
                                     int line)
    throws IOException
  {
    ReadStream is = path.openRead();

    try {
      is.setEncoding(encoding);

      QuercusParser parser;
      parser = new QuercusParser(quercus, path, is);

      if (fileName != null && line >= 0)
        parser.setLocation(fileName, line);

      return parser.parse();
    } finally {
      is.close();
    }
  }

  public static QuercusProgram parse(QuercusContext quercus,
                                     ReadStream is)
    throws IOException
  {
    QuercusParser parser;
    parser = new QuercusParser(quercus, is.getPath(), is);

    return parser.parse();
  }

  public static QuercusProgram parse(QuercusContext quercus,
                                     Path path, ReadStream is)
    throws IOException
  {
    return new QuercusParser(quercus, path, is).parse();
  }

  public static QuercusProgram parseEval(QuercusContext quercus, String str)
    throws IOException
  {
    Path path = new StringPath(str);

    QuercusParser parser = new QuercusParser(quercus, path, path.openRead());

    return parser.parseCode();
  }

  public static QuercusProgram parseEvalExpr(QuercusContext quercus, String str)
    throws IOException
  {
    Path path = new StringPath(str);

    QuercusParser parser = new QuercusParser(quercus, path, path.openRead());

    return parser.parseCode().createExprReturn();
  }

  public static AbstractFunction parseFunction(QuercusContext quercus,
                                               String name,
                                               String args,
                                               String code)
    throws IOException
  {
    Path argPath = new StringPath(args);
    Path codePath = new StringPath(code);

    QuercusParser parser = new QuercusParser(quercus);

    Function fun = parser.parseFunction(name, argPath, codePath);

    parser.close();

    return fun;
  }
  
  public boolean isUnicodeSemantics()
  {
    return _quercus != null && _quercus.isUnicodeSemantics();
  }

  public static Expr parse(QuercusContext quercus, String str)
    throws IOException
  {
      Path path = new StringPath(str);

    return new QuercusParser(quercus, path, path.openRead()).parseExpr();
  }

  public static Expr parseDefault(String str)
  {
    try {
      Path path = new StringPath(str);
      
      return new QuercusParser(null, path, path.openRead()).parseExpr();
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  public static Expr parseDefault(ExprFactory factory, String str)
  {
    try {
      Path path = new StringPath(str);
      
      QuercusParser parser = new QuercusParser(null, path, path.openRead());
      
      parser._factory = factory;
      
      return parser.parseExpr();
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Returns the current filename.
   */
  public String getFileName()
  {
    if (_sourceFile == null)
      return null;
    else
      return _sourceFile.getPath();
  }

  /**
   * Returns the current class name
   */
  public String getClassName()
  {
    if (_classDef != null)
      return _classDef.getName();
    else
      return null;
  }
  
  /**
   * Returns the current line
   */
  public int getLine()
  {
    return _parserLocation.getLineNumber();
  }

  public ExprFactory getExprFactory()
  {
    return _factory;
  }

  public ExprFactory getFactory()
  {
    return _factory;
  }

  public QuercusProgram parse()
    throws IOException
  {
    ClassDef globalClass = null;
    
    _function = getFactory().createFunctionInfo(_quercus, globalClass, "");
    _function.setPageMain(true);

    // quercus/0b0d
    _function.setVariableVar(true);
    _function.setUsesSymbolTable(true);

    Statement stmt = parseTop();

    QuercusProgram program
      = new QuercusProgram(_quercus, _sourceFile,
                           _globalScope.getFunctionMap(),
                           _globalScope.getFunctionList(),
                           _globalScope.getClassMap(),
                           _globalScope.getClassList(),
                           _function,
                           stmt);
    return program;

    /*
    com.caucho.vfs.WriteStream out = com.caucho
        .vfs.Vfs.lookup("stdout:").openWrite();
    out.setFlushOnNewline(true);
    stmt.debug(new JavaWriter(out));
    */
  }

  QuercusProgram parseCode()
    throws IOException
  {
    ClassDef globalClass = null;
    
    _function = getFactory().createFunctionInfo(_quercus, globalClass, "eval");
    // XXX: need param or better function name for non-global?
    _function.setGlobal(false);

    Location location = getLocation();

    ArrayList<Statement> stmtList = parseStatementList();

    return new QuercusProgram(_quercus, _sourceFile,
                              _globalScope.getFunctionMap(),
                              _globalScope.getFunctionList(),
                              _globalScope.getClassMap(),
                              _globalScope.getClassList(),
                              _function,
                              _factory.createBlock(location, stmtList));
  }

  public Function parseFunction(String name, Path argPath, Path codePath)
    throws IOException
  {
    ClassDef globalClass = null;
    
    _function = getFactory().createFunctionInfo(_quercus, globalClass, name);
    _function.setGlobal(false);
    _function.setPageMain(true);

    init(argPath);

    Arg []args = parseFunctionArgDefinition();

    close();

    init(codePath);

    Statement []statements = parseStatements();

    Function fun = _factory.createFunction(Location.UNKNOWN,
                                           name,
                                           _function,
                                           args,
                                           statements);

    close();

    return fun;
  }

  /**
   * Parses the top page.
   */
  Statement parseTop()
    throws IOException
  {
    _isTop = true;

    ArrayList<Statement> statements = new ArrayList<Statement>();

    Location location = getLocation();

    int token = parsePhpText();

    if (_lexeme.length() > 0)
      statements.add(_factory.createText(location, _lexeme));

    if (token == TEXT_ECHO) {
      parseEcho(statements);
    }
    else if (token == TEXT_PHP) {
      _peekToken = parseToken();

      if (_peekToken == IDENTIFIER && _lexeme.equalsIgnoreCase("php")) {
        _peekToken = -1;
      }
    }

    statements.addAll(parseStatementList());

    return _factory.createBlock(location, statements);
  }

  /*
   * Parses a statement list.
   */
  private Statement []parseStatements()
    throws IOException
  {
    ArrayList<Statement> statementList = parseStatementList();

    Statement []statements = new Statement[statementList.size()];

    statementList.toArray(statements);

    return statements;
  }

  /**
   * Parses a statement list.
   */
  private ArrayList<Statement> parseStatementList()
    throws IOException
  {
    ArrayList<Statement> statementList = new ArrayList<Statement>();

    while (true) {
      Location location = getLocation();

      int token = parseToken();

      switch (token) {
      case -1:
        return statementList;

      case ';':
        break;

      case ECHO:
        parseEcho(statementList);
        break;

      case PRINT:
        statementList.add(parsePrint());
        break;

      case UNSET:
        parseUnset(statementList);
        break;

      case ABSTRACT:
      case FINAL:
        {
          _peekToken = token;

          int modifiers = 0;
          do {
            token = parseToken();

            switch (token) {
            case ABSTRACT:
              modifiers |= M_ABSTRACT;
              break;
            case FINAL:
              modifiers |= M_FINAL;
              break;
            case CLASS:
              statementList.add(parseClassDefinition(modifiers));
              break;
            default:
              throw error(L.l("expected 'class' at {0}",
                              tokenName(token)));
            }
          } while (token != CLASS);
        }
        break;

      case FUNCTION:
        {
          Location functionLocation = getLocation();

          Function fun = parseFunctionDefinition(M_STATIC);

          if (! _isTop) {
            statementList.add(
                _factory.createFunctionDef(functionLocation, fun));
          }
        }
        break;

      case CLASS:
        // parseClassDefinition(0);
        statementList.add(parseClassDefinition(0));
        break;

      case INTERFACE:
        // parseClassDefinition(M_INTERFACE);
        statementList.add(parseClassDefinition(M_INTERFACE));
        break;
        
      case CONST:
        statementList.addAll(parseConstDefinition());
        break;

      case IF:
        statementList.add(parseIf());
        break;

      case SWITCH:
        statementList.add(parseSwitch());
        break;

      case WHILE:
        statementList.add(parseWhile());
        break;

      case DO:
        statementList.add(parseDo());
        break;

      case FOR:
        statementList.add(parseFor());
        break;

      case FOREACH:
        statementList.add(parseForeach());
        break;

      case PHP_END:
        return statementList;

      case RETURN:
        statementList.add(parseReturn());
        break;

      case THROW:
        statementList.add(parseThrow());
        break;

      case BREAK:
        statementList.add(parseBreak());
        break;

      case CONTINUE:
        statementList.add(parseContinue());
        break;

      case GLOBAL:
        statementList.add(parseGlobal());
        break;

      case STATIC:
        statementList.add(parseStatic());
        break;

      case TRY:
        statementList.add(parseTry());
        break;
        
      case NAMESPACE:
        statementList.addAll(parseNamespace());
        break;
        
      case USE:
        parseUse();
        break;

      case '{':
        {
          ArrayList<Statement> enclosedStatementList = parseStatementList();

          expect('}');

          statementList.addAll(enclosedStatementList);
        }
        break;

      case '}':
      case CASE:
      case DEFAULT:
      case ELSE:
      case ELSEIF:
      case ENDIF:
      case ENDWHILE:
      case ENDFOR:
      case ENDFOREACH:
      case ENDSWITCH:
        _peekToken = token;
        return statementList;

      case TEXT:
        if (_lexeme.length() > 0) {
          statementList.add(_factory.createText(location, _lexeme));
        }
        break;

      case TEXT_PHP:
        if (_lexeme.length() > 0) {
          statementList.add(_factory.createText(location, _lexeme));
        }

        _peekToken = parseToken();

        if (_peekToken == IDENTIFIER && _lexeme.equalsIgnoreCase("php")) {
          _peekToken = -1;
        }
        break;

      case TEXT_ECHO:
        if (_lexeme.length() > 0)
          statementList.add(_factory.createText(location, _lexeme));

        parseEcho(statementList);

        break;

      default:
        _peekToken = token;

        statementList.add(parseExprStatement());
        break;
      }
    }
  }

  private Statement parseStatement()
    throws IOException
  {
    Location location = getLocation();

    int token = parseToken();

    switch (token) {
    case ';':
      return _factory.createNullStatement();

    case '{':
      location = getLocation();

      ArrayList<Statement> statementList = parseStatementList();

      expect('}');

      return _factory.createBlock(location, statementList);

    case IF:
      return parseIf();

    case SWITCH:
      return parseSwitch();

    case WHILE:
      return parseWhile();

    case DO:
      return parseDo();

    case FOR:
      return parseFor();

    case FOREACH:
      return parseForeach();

    case TRY:
      return parseTry();

    case TEXT:
      if (_lexeme.length() > 0) {
        return _factory.createText(location, _lexeme);
      }
      else
        return parseStatement();

    case TEXT_PHP:
      {
        Statement stmt = null;

        if (_lexeme.length() > 0) {
          stmt = _factory.createText(location, _lexeme);
        }

        _peekToken = parseToken();

        if (_peekToken == IDENTIFIER && _lexeme.equalsIgnoreCase("php")) {
          _peekToken = -1;
        }

        if (stmt == null)
          stmt = parseStatement();

        return stmt;
      }

    default:
      Statement stmt = parseStatementImpl(token);

      token  = parseToken();
      if (token != ';')
        _peekToken = token;

      return stmt;
    }
  }

  /**
   * Parses statements that expect to be terminated by ';'.
   */
  private Statement parseStatementImpl(int token)
    throws IOException
  {
    switch (token) {
    case ECHO:
      {
        Location location = getLocation();

        ArrayList<Statement> statementList = new ArrayList<Statement>();
        parseEcho(statementList);

        return _factory.createBlock(location, statementList);
      }

    case PRINT:
      return parsePrint();

    case UNSET:
      return parseUnset();

    case GLOBAL:
      return parseGlobal();

    case STATIC:
      return parseStatic();

    case BREAK:
      return parseBreak();

    case CONTINUE:
      return parseContinue();

    case RETURN:
      return parseReturn();

    case THROW:
      return parseThrow();

    case TRY:
      return parseTry();

    default:
      _peekToken = token;
      return parseExprStatement();

      /*
    default:
      throw error(L.l("unexpected token {0}.", tokenName(token)));
      */
    }
  }

  /**
   * Parses the echo statement.
   */
  private void parseEcho(ArrayList<Statement> statements)
    throws IOException
  {
    Location location = getLocation();

    while (true) {
      Expr expr = parseTopExpr();

      createEchoStatements(location, statements, expr);

      int token = parseToken();

      if (token != ',') {
        _peekToken = token;
        return;
      }
    }
  }

  /**
   * Creates echo statements from an expression.
   */
  private void createEchoStatements(Location location,
                                    ArrayList<Statement> statements,
                                    Expr expr)
  {
    if (expr == null) {
      // since AppendExpr.getNext() can be null.
    }
    else if (expr instanceof BinaryAppendExpr) {
      BinaryAppendExpr append = (BinaryAppendExpr) expr;

      // XXX: children of append print differently?

      createEchoStatements(location, statements, append.getValue());
      createEchoStatements(location, statements, append.getNext());
    }
    else if (expr instanceof LiteralStringExpr) {
      LiteralStringExpr string = (LiteralStringExpr) expr;

      Statement statement
        = _factory.createText(location, string.evalConstant().toString());

      statements.add(statement);
    }
    else {
      Statement statement = _factory.createEcho(location, expr);

      statements.add(statement);
    }
  }

  /**
   * Parses the print statement.
   */
  private Statement parsePrint()
    throws IOException
  {
    return _factory.createExpr(getLocation(), parsePrintExpr());
  }

  /**
   * Parses the print statement.
   */
  private Expr parsePrintExpr()
    throws IOException
  {
    ArrayList<Expr> args = new ArrayList<Expr>();
    args.add(parseTopExpr());

    return _factory.createCall(this, "print", args);
  }

  /**
   * Parses the global statement.
   */
  private Statement parseGlobal()
    throws IOException
  {
    ArrayList<Statement> statementList = new ArrayList<Statement>();

    Location location = getLocation();

    while (true) {
      Expr expr = parseTopExpr();

      if (expr instanceof VarExpr) {
        VarExpr var = (VarExpr) expr;

        _function.setUsesGlobal(true);

        // php/323c
        // php/3a6g, php/3a58
        //var.getVarInfo().setGlobal();

        statementList.add(_factory.createGlobal(location, var));
      }
      else if (expr instanceof VarVarExpr) {
        VarVarExpr var = (VarVarExpr) expr;

        statementList.add(_factory.createVarGlobal(location, var));
      }
      else
        throw error(L.l("unknown expr {0} to global", expr));

      // statementList.add(new ExprStatement(expr));

      int token = parseToken();

      if (token != ',') {
        _peekToken = token;
        return _factory.createBlock(location, statementList);
      }
    }
  }

  /**
   * Parses the static statement.
   */
  private Statement parseStatic()
    throws IOException
  {
    ArrayList<Statement> statementList = new ArrayList<Statement>();

    Location location = getLocation();

    while (true) {
      expect('$');

      String name = parseIdentifier();

      VarExpr var = _factory.createVar(_function.createVar(name));

      Expr init = null;

      int token = parseToken();

      if (token == '=') {
        init = parseExpr();
        token = parseToken();
      }

      // var.getVarInfo().setReference();
      
      if (_classDef != null) {
        statementList.add(_factory.createClassStatic(location,
                                                     _classDef.getName(),
                                                     var,
                                                     init));
      }
      else {
        statementList.add(_factory.createStatic(location, var, init));
      }

      if (token != ',') {
        _peekToken = token;
        return _factory.createBlock(location, statementList);
      }
    }
  }

  /**
   * Parses the unset statement.
   */
  private Statement parseUnset()
    throws IOException
  {
    Location location = getLocation();

    ArrayList<Statement> statementList = new ArrayList<Statement>();
    parseUnset(statementList);

    return _factory.createBlock(location, statementList);
  }

  /**
   * Parses the unset statement.
   */
  private void parseUnset(ArrayList<Statement> statementList)
    throws IOException
  {
    Location location = getLocation();

    int token = parseToken();

    if (token != '(') {
      _peekToken = token;

      statementList.add(parseTopExpr().createUnset(_factory, location));

      return;
    }

    do {
      // XXX: statementList.add(
      //    parseTopExpr().createUnset(_factory, getLocation()));

      Expr topExpr = parseTopExpr();

      statementList.add(topExpr.createUnset(_factory, getLocation()));
    } while ((token = parseToken()) == ',');

    _peekToken = token;
    expect(')');
  }

  /**
   * Parses the if statement
   */
  private Statement parseIf()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      Location location = getLocation();

      expect('(');

      _isIfTest = true;
      Expr test = parseExpr();
      _isIfTest = false;

      expect(')');

      int token = parseToken();

      if (token == ':')
        return parseAlternateIf(test, location);
      else
        _peekToken = token;

      Statement trueBlock = null;

      trueBlock = parseStatement();

      Statement falseBlock = null;

      token = parseToken();

      if (token == ELSEIF) {
        falseBlock = parseIf();
      }
      else if (token == ELSE) {
        falseBlock = parseStatement();
      }
      else
        _peekToken = token;

      return _factory.createIf(location, test, trueBlock, falseBlock);

    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses the if statement
   */
  private Statement parseAlternateIf(Expr test, Location location)
    throws IOException
  {
    Statement trueBlock = null;

    trueBlock = _factory.createBlock(location, parseStatementList());

    Statement falseBlock = null;

    int token = parseToken();

    if (token == ELSEIF) {
      Location subLocation = getLocation();

      Expr subTest = parseExpr();
      expect(':');

      falseBlock = parseAlternateIf(subTest, subLocation);
    }
    else if (token == ELSE) {
      expect(':');

      falseBlock = _factory.createBlock(getLocation(), parseStatementList());

      expect(ENDIF);
    }
    else {
      _peekToken = token;
      expect(ENDIF);
    }

    return _factory.createIf(location, test, trueBlock, falseBlock);
  }

  /**
   * Parses the switch statement
   */
  private Statement parseSwitch()
    throws IOException
  {
    Location location = getLocation();

    boolean oldTop = _isTop;
    _isTop = false;

    String label = pushSwitchLabel();

    try {
      expect('(');

      Expr test = parseExpr();

      expect(')');

      boolean isAlternate = false;

      int token = parseToken();

      if (token == ':')
        isAlternate = true;
      else if (token == '{')
        isAlternate = false;
      else {
        _peekToken = token;

        expect('{');
      }

      ArrayList<Expr[]> caseList = new ArrayList<Expr[]>();
      ArrayList<BlockStatement> blockList = new ArrayList<BlockStatement>();

      ArrayList<Integer> fallThroughList = new ArrayList<Integer>();
      BlockStatement defaultBlock = null;

      while ((token = parseToken()) == CASE || token == DEFAULT) {
        Location caseLocation = getLocation();

        ArrayList<Expr> valueList = new ArrayList<Expr>();
        boolean isDefault = false;

        while (token == CASE || token == DEFAULT) {
          if (token == CASE) {
            Expr value = parseExpr();

            valueList.add(value);
          }
          else
            isDefault = true;

          token = parseToken();
          if (token == ':') {
          }
          else if (token == ';') {
            // XXX: warning?
          }
          else
            throw error("expected ':' at " + tokenName(token));

          token = parseToken();
        }

        _peekToken = token;

        Expr []values = new Expr[valueList.size()];
        valueList.toArray(values);

        ArrayList<Statement> newBlockList = parseStatementList();

        for (int fallThrough : fallThroughList) {
          BlockStatement block = blockList.get(fallThrough);

          boolean isDefaultBlock = block == defaultBlock;

          block = block.append(newBlockList);

          blockList.set(fallThrough, block);

          if (isDefaultBlock)
            defaultBlock = block;
        }

        BlockStatement block
          = _factory.createBlockImpl(caseLocation, newBlockList);

        if (values.length > 0) {
          caseList.add(values);

          blockList.add(block);
        }

        if (isDefault)
          defaultBlock = block;

        if (blockList.size() > 0
            && ! fallThroughList.contains(blockList.size() - 1)) {
          fallThroughList.add(blockList.size() - 1);
        }

        if (block.fallThrough() != Statement.FALL_THROUGH)
          fallThroughList.clear();
      }

      _peekToken = token;

      if (isAlternate)
        expect(ENDSWITCH);
      else
        expect('}');

      return _factory.createSwitch(location, test,
                                   caseList, blockList,
                                   defaultBlock, label);
    } finally {
      _isTop = oldTop;

      popLoopLabel();
    }
  }

  /**
   * Parses the 'while' statement
   */
  private Statement parseWhile()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    String label = pushWhileLabel();

    try {
      Location location = getLocation();

      expect('(');

      _isIfTest = true;
      Expr test = parseExpr();
      _isIfTest = false;

      expect(')');

      Statement block;

      int token = parseToken();

      if (token == ':') {
        block = _factory.createBlock(getLocation(), parseStatementList());

        expect(ENDWHILE);
      }
      else {
        _peekToken = token;

        block = parseStatement();
      }

      return _factory.createWhile(location, test, block, label);
    } finally {
      _isTop = oldTop;

      popLoopLabel();
    }
  }

  /**
   * Parses the 'do' statement
   */
  private Statement parseDo()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    String label = pushDoLabel();

    try {
      Location location = getLocation();

      Statement block = parseStatement();

      expect(WHILE);
      expect('(');

      _isIfTest = true;
      Expr test = parseExpr();
      _isIfTest = false;

      expect(')');

      return _factory.createDo(location, test, block, label);
    } finally {
      _isTop = oldTop;

      popLoopLabel();
    }
  }

  /**
   * Parses the 'for' statement
   */
  private Statement parseFor()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    String label = pushForLabel();

    try {
      Location location = getLocation();

      expect('(');

      Expr init = null;

      int token = parseToken();
      if (token != ';') {
        _peekToken = token;
        init = parseTopCommaExpr();
        expect(';');
      }

      Expr test = null;

      token = parseToken();
      if (token != ';') {
        _peekToken = token;

        _isIfTest = true;
        test = parseTopCommaExpr();
        _isIfTest = false;

        expect(';');
      }

      Expr incr = null;

      token = parseToken();
      if (token != ')') {
        _peekToken = token;
        incr = parseTopCommaExpr();
        expect(')');
      }

      Statement block;

      token = parseToken();

      if (token == ':') {
        block = _factory.createBlock(getLocation(), parseStatementList());

        expect(ENDFOR);
      }
      else {
        _peekToken = token;

        block = parseStatement();
      }

      return _factory.createFor(location, init, test, incr, block, label);
    } finally {
      _isTop = oldTop;

      popLoopLabel();
    }
  }

  /**
   * Parses the 'foreach' statement
   */
  private Statement parseForeach()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    String label = pushForeachLabel();

    try {
      Location location = getLocation();

      expect('(');

      Expr objExpr = parseTopExpr();

      expect(AS);

      boolean isRef = false;

      int token = parseToken();
      if (token == '&')
        isRef = true;
      else
        _peekToken = token;

      AbstractVarExpr valueExpr = (AbstractVarExpr) parseLeftHandSide();

      AbstractVarExpr keyVar = null;
      AbstractVarExpr valueVar;

      token = parseToken();

      if (token == ARRAY_RIGHT) {
        if (isRef)
          throw error(L.l("key reference is forbidden in foreach"));

        keyVar = valueExpr;

        token = parseToken();

        if (token == '&')
          isRef = true;
        else
          _peekToken = token;

        valueVar = (AbstractVarExpr) parseLeftHandSide();

        token = parseToken();
      }
      else
        valueVar = valueExpr;

      if (token != ')')
        throw error(L.l("expected ')' in foreach"));

      Statement block;

      token = parseToken();

      if (token == ':') {
        block = _factory.createBlock(getLocation(), parseStatementList());

        expect(ENDFOREACH);
      }
      else {
        _peekToken = token;

        block = parseStatement();
      }

      return _factory.createForeach(location, objExpr, keyVar,
                                    valueVar, isRef, block, label);
    } finally {
      _isTop = oldTop;

      popLoopLabel();
    }
  }

  /**
   * Parses the try statement
   */
  private Statement parseTry()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    try {
      Location location = getLocation();

      Statement block = null;

      try {
        block = parseStatement();
      } finally {
        //  _scope = oldScope;
      }

      TryStatement stmt = _factory.createTry(location, block);

      int token = parseToken();

      while (token == CATCH) {
        expect('(');

        String id = parseNamespaceIdentifier();

        AbstractVarExpr lhs = parseLeftHandSide();

        expect(')');

        block = parseStatement();

        stmt.addCatch(id, lhs, block);

        token = parseToken();
      }

      _peekToken = token;

      return stmt;
    } finally {
      _isTop = oldTop;
    }
  }

  /**
   * Parses a function definition
   */
  private Function parseFunctionDefinition(int modifiers)
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    boolean oldReturnsReference = _returnsReference;
    FunctionInfo oldFunction = _function;

    boolean isAbstract = (modifiers & M_ABSTRACT) != 0;
    boolean isStatic = (modifiers & M_STATIC) != 0;

    if (_classDef != null && _classDef.isInterface())
      isAbstract = true;

    try {
      _returnsReference = false;

      int token = parseToken();

      String comment = _comment;
      _comment = null;

      if (token == '&')
        _returnsReference = true;
      else
        _peekToken = token;

      String name;
      
      name = parseIdentifier();
      
      if (_classDef == null) {
        name = resolveIdentifier(name);
      }
      
      if (isAbstract && ! _scope.isAbstract()) {
        if (_classDef != null)
          throw error(L.l(
              "'{0}' may not be abstract because class {1} is not abstract.",
                          name, _classDef.getName()));
        else
          throw error(L.l(
              "'{0}' may not be abstract. Abstract functions are only "
                  + "allowed in abstract classes.",
                          name));
      }
      
      boolean isConstructor = false;
      
      if (_classDef != null
          && (name.equals(_classDef.getName())
              || name.equals("__constructor"))) {
        if (isStatic) {
          throw error(L.l(
              "'{0}:{1}' may not be static because class constructors "
                  + "may not be static",
                          _classDef.getName(), name));
        }
        
        isConstructor = true;
      }

      _function = getFactory().createFunctionInfo(_quercus, _classDef, name);
      _function.setPageStatic(oldTop);
      _function.setConstructor(isConstructor);

      _function.setReturnsReference(_returnsReference);

      Location location = getLocation();

      expect('(');

      Arg []args = parseFunctionArgDefinition();

      expect(')');

      if (_classDef != null
          && "__call".equals(name)
          && args.length != 2)
      {
        throw error(L.l("{0}::{1} must have exactly two arguments defined",
                        _classDef.getName(), name));
      }

      Function function;

      if (isAbstract) {
        expect(';');

        function = _factory.createMethodDeclaration(location,
                                                    _classDef, name,
                                                    _function, args);
      }
      else {
        expect('{');

        Statement []statements = null;

        Scope oldScope = _scope;
        try {
          _scope = new FunctionScope(_factory, oldScope);
          statements = parseStatements();
        } finally {
          _scope = oldScope;
        }

        expect('}');

        if (_classDef != null)
          function = _factory.createObjectMethod(location,
                                                 _classDef,
                                                 name, _function,
                                                 args, statements);
        else
          function = _factory.createFunction(location, name,
                                             _function, args,
                                             statements);
      }

      function.setGlobal(oldTop);
      function.setStatic((modifiers & M_STATIC) != 0);
      function.setFinal((modifiers & M_FINAL) != 0);

      function.setParseIndex(_functionsParsed++);
      function.setComment(comment);

      if ((modifiers & M_PROTECTED) != 0)
        function.setVisibility(Visibility.PROTECTED);
      else if ((modifiers & M_PRIVATE) != 0)
        function.setVisibility(Visibility.PRIVATE);

      _scope.addFunction(name, function, oldTop);

      /*
    com.caucho.vfs.WriteStream out = com.caucho.vfs
          .Vfs.lookup("stdout:").openWrite();
    out.setFlushOnNewline(true);
    function.debug(new JavaWriter(out));
      */

      return function;
    } finally {
      _returnsReference = oldReturnsReference;
      _function = oldFunction;
      _isTop = oldTop;
    }
  }

  /**
   * Parses a function definition
   */
  private Expr parseClosure()
    throws IOException
  {
    boolean oldTop = _isTop;
    _isTop = false;

    boolean oldReturnsReference = _returnsReference;
    FunctionInfo oldFunction = _function;

    try {
      _returnsReference = false;

      int token = parseToken();

      String comment = null;

      if (token == '&')
        _returnsReference = true;
      else
        _peekToken = token;
      
      String name = "__quercus_closure_" + _functionsParsed;

      ClassDef classDef = null;
      _function = getFactory().createFunctionInfo(_quercus, classDef, name);
      _function.setReturnsReference(_returnsReference);
      _function.setClosure(true);

      Location location = getLocation();

      expect('(');

      Arg []args = parseFunctionArgDefinition();

      expect(')');

      Arg []useArgs;
      ArrayList<VarExpr> useVars = new ArrayList<VarExpr>();
      
      token = parseToken();
      
      if (token == USE) {
        expect('(');

        useArgs = parseFunctionArgDefinition();
        
        for (Arg arg : useArgs) {
          VarExpr var = _factory.createVar(
              oldFunction.createVar(arg.getName()));
          
          useVars.add(var);
        }

        expect(')');
      }
      else {
        useArgs = new Arg[0];
      
        _peekToken = token;
      }
      
      expect('{');

      Statement []statements = null;

      Scope oldScope = _scope;
      try {
        _scope = new FunctionScope(_factory, oldScope);
        statements = parseStatements();
      } finally {
        _scope = oldScope;
      }

      expect('}');

      Function function = _factory.createFunction(location, name,
                                                  _function, args,
                                                  statements);

      function.setParseIndex(_functionsParsed++);
      function.setComment(comment);
      function.setClosure(true);
      function.setClosureUseArgs(useArgs);
      
      _globalScope.addFunction(name, function, oldTop);

      return _factory.createClosure(location, function, useVars);
    } finally {
      _returnsReference = oldReturnsReference;
      _function = oldFunction;
      _isTop = oldTop;
    }
  }


  private Arg []parseFunctionArgDefinition()
    throws IOException
  {
    LinkedHashMap<String, Arg> argMap = new LinkedHashMap<String, Arg>();

    while (true) {
      int token = parseToken();
      boolean isReference = false;

      // php/076b, php/1c02
      // XXX: save arg type for type checking upon function call
      String expectedClass = null;
      if (token != ')'
          && token != '&'
          && token != '$'
          && token != -1) {
        _peekToken = token;
        expectedClass = parseIdentifier();
        token = parseToken();
      }

      if (token == '&') {
        isReference = true;
        token = parseToken();
      }

      if (token != '$') {
        _peekToken = token;
        break;
      }

      String argName = parseIdentifier();
      Expr defaultExpr = _factory.createRequired();

      token = parseToken();
      if (token == '=') {
        // XXX: actually needs to be primitive
        defaultExpr = parseUnary(); // parseTerm(false);

        token = parseToken();
      }

      Arg arg = new Arg(argName, defaultExpr, isReference, expectedClass);

      if (argMap.get(argName) != null && _quercus.isStrict()) {
        throw error(L.l("aliasing of function argument '{0}'", argName));
      }

      argMap.put(argName, arg);

      VarInfo var = _function.createVar(argName);

      if (token != ',') {
        _peekToken = token;
        break;
      }
    }

    Arg [] args = new Arg[argMap.size()];

    argMap.values().toArray(args);

    return args;
  }

  /**
   * Parses the 'return' statement
   */
  private Statement parseBreak()
    throws IOException
  {
    // commented out for adodb (used by Moodle and others)
    // XXX: should only throw fatal error if break statement is reached
    //      during execution

    if (! _isTop && _loopLabelList.size() == 0 && ! _quercus.isLooseParse())
      throw error(L.l("cannot 'break' inside a function"));

    Location location = getLocation();

    int token = parseToken();

    switch (token) {
    case ';':
      _peekToken = token;

      return _factory.createBreak(location,
                                  null,
                                  (ArrayList<String>) _loopLabelList.clone());

    default:
      _peekToken = token;

      Expr expr = parseTopExpr();

      return _factory.createBreak(location,
                                  expr,
                                  (ArrayList<String>) _loopLabelList.clone());
    }
  }

  /**
   * Parses the 'return' statement
   */
  private Statement parseContinue()
    throws IOException
  {
    if (! _isTop && _loopLabelList.size() == 0 && ! _quercus.isLooseParse())
      throw error(L.l("cannot 'continue' inside a function"));

    Location location = getLocation();

    int token = parseToken();

    switch (token) {
    case TEXT_PHP:
    case ';':
      _peekToken = token;

      return _factory
          .createContinue(location,
              null,
              (ArrayList<String>) _loopLabelList.clone());

    default:
      _peekToken = token;

      Expr expr = parseTopExpr();

      return _factory
          .createContinue(location,
              expr,
              (ArrayList<String>) _loopLabelList.clone());
    }
  }

  /**
   * Parses the 'return' statement
   */
  private Statement parseReturn()
    throws IOException
  {
    Location location = getLocation();

    int token = parseToken();

    switch (token) {
    case ';':
      _peekToken = token;

      return _factory.createReturn(location, _factory.createNull());

    default:
      _peekToken = token;

      Expr expr = parseTopExpr();

      /*
      if (_returnsReference)
        expr = expr.createRef();
      else
        expr = expr.createCopy();
      */

      if (_returnsReference)
        return _factory.createReturnRef(location, expr);
      else
        return _factory.createReturn(location, expr);
    }
  }

  /**
   * Parses the 'throw' statement
   */
  private Statement parseThrow()
    throws IOException
  {
    Location location = getLocation();

    Expr expr = parseExpr();

    return _factory.createThrow(location, expr);
  }

  /**
   * Parses a class definition
   */
  private Statement parseClassDefinition(int modifiers)
    throws IOException
  {
    String name = parseIdentifier();
    
    name = resolveIdentifier(name);

    String comment = _comment;

    String parentName = null;

    ArrayList<String> ifaceList = new ArrayList<String>();

    int token = parseToken();
    if (token == EXTENDS) {
      if ((modifiers & M_INTERFACE) != 0) {
        do {
          ifaceList.add(parseNamespaceIdentifier());

          token = parseToken();
        } while (token == ',');
      }
      else {
        parentName = parseNamespaceIdentifier();

        token = parseToken();
      }
    }

    if ((modifiers & M_INTERFACE) == 0 && token == IMPLEMENTS) {
      do {
        ifaceList.add(parseNamespaceIdentifier());

        token = parseToken();
      } while (token == ',');
    }

    _peekToken = token;

    InterpretedClassDef oldClass = _classDef;
    Scope oldScope = _scope;

    try {
      _classDef = oldScope.addClass(getLocation(),
                                    name, parentName, ifaceList,
                                    _classesParsed++,
                                    _isTop);

      _classDef.setComment(comment);

      if ((modifiers & M_ABSTRACT) != 0)
        _classDef.setAbstract(true);
      if ((modifiers & M_INTERFACE) != 0)
        _classDef.setInterface(true);
      if ((modifiers & M_FINAL) != 0)
        _classDef.setFinal(true);

      _scope = new ClassScope(_classDef);

      expect('{');

      parseClassContents();

      expect('}');

      return _factory.createClassDef(getLocation(), _classDef);
    } finally {
      _classDef = oldClass;
      _scope = oldScope;
    }
  }

  /**
   * Parses a statement list.
   */
  private void parseClassContents()
    throws IOException
  {
    while (true) {
      _comment = null;

      int token = parseToken();

      switch (token) {
        case ';':
          break;

        case FUNCTION:
        {
          Function fun = parseFunctionDefinition(0);
          fun.setStatic(false);
          break;
        }

        case CLASS:
          parseClassDefinition(0);
          break;

            /* quercus/0260
        case VAR:
              parseClassVarDefinition(false);
              break;
        */

        case CONST:
          parseClassConstDefinition();
          break;

        case PUBLIC:
        case PRIVATE:
        case PROTECTED:
        case STATIC:
        case FINAL:
        case ABSTRACT:
        {
          _peekToken = token;
          int modifiers = parseModifiers();

          int token2 = parseToken();

          if (token2 == FUNCTION) {
            Function fun = parseFunctionDefinition(modifiers);
          }
          else {
            _peekToken = token2;

            parseClassVarDefinition(modifiers);
          }
        }
        break;

        case IDENTIFIER:
          if (_lexeme.equals("var")) {
            parseClassVarDefinition(0);
          }
          else {
            _peekToken = token;
            return;
          }
          break;

        case -1:
        case '}':
        default:
          _peekToken = token;
          return;
      }
    }
  }

  /**
   * Parses a function definition
   */
  private void parseClassVarDefinition(int modifiers)
    throws IOException
  {
    int token;

    do {
      expect('$');

      String comment = _comment;

      String name = parseIdentifier();

      token = parseToken();

      Expr expr = null;

      if (token == '=') {
        expr = parseExpr();
      }
      else {
        _peekToken = token;
        expr = _factory.createNull();
      }

      StringValue nameV = createStringValue(name);

      if ((modifiers & M_STATIC) != 0) {
        ((ClassScope) _scope).addStaticVar(nameV, expr, _comment);
      }
      else if ((modifiers & M_PRIVATE) != 0) {
        ((ClassScope) _scope).addVar(nameV,
                                     expr,
                                     FieldVisibility.PRIVATE,
                                     comment);
      }
      else if ((modifiers & M_PROTECTED) != 0) {
        ((ClassScope) _scope).addVar(nameV,
                                     expr,
                                     FieldVisibility.PROTECTED,
                                     comment);
      }
      else {
        ((ClassScope) _scope).addVar(nameV,
                                     expr,
                                     FieldVisibility.PUBLIC,
                                     comment);
      }

      token = parseToken();
    } while (token == ',');

    _peekToken = token;
  }

  /**
   * Parses a const definition
   */
  private ArrayList<Statement> parseConstDefinition()
    throws IOException
  {
    ArrayList<Statement> constList = new ArrayList<Statement>();
    
    int token;

    do {
      String name = parseNamespaceIdentifier();

      expect('=');

      Expr expr = parseExpr();

      ArrayList<Expr> args = new ArrayList<Expr>();
      args.add(_factory.createString(name));
      args.add(expr);
      
      Expr fun = _factory.createCall(this, "define", args);
      
      constList.add(_factory.createExpr(getLocation(), fun));
      // _scope.addConstant(name, expr);

      token = parseToken();
    } while (token == ',');

    _peekToken = token;
    
    return constList;
  }

  /**
   * Parses a const definition
   */
  private void parseClassConstDefinition()
    throws IOException
  {
    int token;

    do {
      String name = parseIdentifier();

      expect('=');

      Expr expr = parseExpr();

      ((ClassScope) _scope).addConstant(name, expr);

      token = parseToken();
    } while (token == ',');

    _peekToken = token;
  }

  private int parseModifiers()
    throws IOException
  {
    int token;
    int modifiers = 0;

    while (true) {
      token = parseToken();

      switch (token) {
      case PUBLIC:
        modifiers |= M_PUBLIC;
        break;

      case PRIVATE:
        modifiers |= M_PRIVATE;
        break;

      case PROTECTED:
        modifiers |= M_PROTECTED;
        break;

      case FINAL:
        modifiers |= M_FINAL;
        break;

      case STATIC:
        modifiers |= M_STATIC;
        break;

      case ABSTRACT:
        modifiers |= M_ABSTRACT;
        break;

      default:
        _peekToken = token;
        return modifiers;
      }
    }
  }
  
  private ArrayList<Statement> parseNamespace()
    throws IOException
  {
    int token = parseToken();
    
    String var = "";
    
    if (token == IDENTIFIER) {
      var = _lexeme;
      
      token = parseToken();
    }
    
    if (var.startsWith("\\"))
      var = var.substring(1);
    
    String oldNamespace = _namespace;
    
    _namespace = var;
    
    if (token == '{') {
      ArrayList<Statement> statementList = parseStatementList();
      
      expect('}');
      
      _namespace = oldNamespace;
      
      return statementList;
    }
    else if (token == ';') {
      return new ArrayList<Statement>();
    }
    else {
      throw error(L.l("namespace must be followed by '{' or ';'"));
    }
  }
  
  private void parseUse()
    throws IOException
  {
    int token = parseNamespaceIdentifier(read());
    
    String name = _lexeme;
    
    int ns = name.lastIndexOf('\\');
    
    String tail;
    if (ns >= 0)
      tail = name.substring(ns + 1);
    else
      tail = name;
    
    if (name.startsWith("\\"))
      name = name.substring(1);
    
    token = parseToken();
    
    if (token == ';') {
      _namespaceUseMap.put(tail, name);
      return;
    }
    else if (token == AS) {
      do {
        tail = parseIdentifier();
        
        _namespaceUseMap.put(tail, name);
      } while ((token = parseToken()) == ',');
    }
    
    _peekToken = token;
    
    expect(';');
  }

  /**
   * Parses an expression statement.
   */
  private Statement parseExprStatement()
    throws IOException
  {
    Location location = getLocation();

    Expr expr = parseTopExpr();

    Statement statement = _factory.createExpr(location, expr);

    int token = parseToken();
    _peekToken = token;

    switch (token) {
    case -1:
    case ';':
    case '}':
    case PHP_END:
    case TEXT:
    case TEXT_PHP:
    case TEXT_ECHO:
      break;

    default:
      expect(';');
      break;
    }

    return statement;
  }

  /**
   * Parses a top expression.
   */
  private Expr parseTopExpr()
    throws IOException
  {
    return parseExpr();
  }

  /**
   * Parses a top expression.
   */
  private Expr parseTopCommaExpr()
    throws IOException
  {
    return parseCommaExpr();
  }

  /**
   * Parses a comma expression.
   */
  private Expr parseCommaExpr()
    throws IOException
  {
    Expr expr = parseExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case ',':
        expr = _factory.createComma(expr, parseExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses an expression with optional '&'.
   */
  private Expr parseRefExpr()
    throws IOException
  {
    int token = parseToken();

    boolean isRef = token == '&';

    if (! isRef)
      _peekToken = token;

    Expr expr = parseExpr();

    if (isRef)
      expr = _factory.createRef(expr);

    return expr;
  }

  /**
   * Parses an expression.
   */
  private Expr parseExpr()
    throws IOException
  {
    return parseWeakOrExpr();
  }

  /**
   * Parses a logical xor expression.
   */
  private Expr parseWeakOrExpr()
    throws IOException
  {
    Expr expr = parseWeakXorExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case OR_RES:
        expr = _factory.createOr(expr, parseWeakXorExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a logical xor expression.
   */
  private Expr parseWeakXorExpr()
    throws IOException
  {
    Expr expr = parseWeakAndExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case XOR_RES:
        expr = _factory.createXor(expr, parseWeakAndExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a logical and expression.
   */
  private Expr parseWeakAndExpr()
    throws IOException
  {
    Expr expr = parseConditionalExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case AND_RES:
        expr = _factory.createAnd(expr, parseConditionalExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a conditional expression.
   */
  private Expr parseConditionalExpr()
    throws IOException
  {
    Expr expr = parseOrExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '?':
        token = parseToken();
        
        if (token == ':') {
          expr = _factory.createShortConditional(expr, parseOrExpr());
        }
        else {
          _peekToken = token;
          
          Expr trueExpr = parseExpr();
          expect(':');
          // php/33c1
          expr = _factory.createConditional(expr, trueExpr, parseOrExpr());
        }
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a logical or expression.
   */
  private Expr parseOrExpr()
    throws IOException
  {
    Expr expr = parseAndExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case C_OR:
        expr = _factory.createOr(expr, parseAndExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a logical and expression.
   */
  private Expr parseAndExpr()
    throws IOException
  {
    Expr expr = parseBitOrExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case C_AND:
        expr = _factory.createAnd(expr, parseBitOrExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a bit or expression.
   */
  private Expr parseBitOrExpr()
    throws IOException
  {
    Expr expr = parseBitXorExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '|':
        expr = _factory.createBitOr(expr, parseBitXorExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a bit xor expression.
   */
  private Expr parseBitXorExpr()
    throws IOException
  {
    Expr expr = parseBitAndExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '^':
        expr = _factory.createBitXor(expr, parseBitAndExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a bit and expression.
   */
  private Expr parseBitAndExpr()
    throws IOException
  {
    Expr expr = parseEqExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '&':
        expr = _factory.createBitAnd(expr, parseEqExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a comparison expression.
   */
  private Expr parseEqExpr()
    throws IOException
  {
    Expr expr = parseCmpExpr();

    int token = parseToken();

    switch (token) {
    case EQ:
      return _factory.createEq(expr, parseCmpExpr());

    case NEQ:
      return _factory.createNeq(expr, parseCmpExpr());

    case EQUALS:
      return _factory.createEquals(expr, parseCmpExpr());

    case NEQUALS:
      return _factory.createNot(_factory.createEquals(expr, parseCmpExpr()));

    default:
      _peekToken = token;
      return expr;
    }
  }

  /**
   * Parses a comparison expression.
   */
  private Expr parseCmpExpr()
    throws IOException
  {
    Expr expr = parseShiftExpr();

    int token = parseToken();

    switch (token) {
    case '<':
      return _factory.createLt(expr, parseShiftExpr());

    case '>':
      return _factory.createGt(expr, parseShiftExpr());

    case LEQ:
      return _factory.createLeq(expr, parseShiftExpr());

    case GEQ:
      return _factory.createGeq(expr, parseShiftExpr());

    case INSTANCEOF:
      Location location = getLocation();

      Expr classNameExpr = parseShiftExpr();

      if (classNameExpr instanceof ConstExpr)
        return _factory.createInstanceOf(expr, classNameExpr.toString());
      else
        return _factory.createInstanceOfVar(expr, classNameExpr);

    default:
      _peekToken = token;
      return expr;
    }
  }

  /**
   * Parses a left/right shift expression.
   */
  private Expr parseShiftExpr()
    throws IOException
  {
    Expr expr = parseAddExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case LSHIFT:
        expr = _factory.createLeftShift(expr, parseAddExpr());
        break;
      case RSHIFT:
        expr = _factory.createRightShift(expr, parseAddExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses an add/substract expression.
   */
  private Expr parseAddExpr()
    throws IOException
  {
    Expr expr = parseMulExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '+':
        expr = _factory.createAdd(expr, parseMulExpr());
        break;
      case '-':
        expr = _factory.createSub(expr, parseMulExpr());
        break;
      case '.':
        expr = _factory.createAppend(expr, parseMulExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses a multiplication/division expression.
   */
  private Expr parseMulExpr()
    throws IOException
  {
    Expr expr = parseAssignExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '*':
        expr = _factory.createMul(expr, parseAssignExpr());
        break;
      case '/':
        expr = _factory.createDiv(expr, parseAssignExpr());
        break;
      case '%':
        expr = _factory.createMod(expr, parseAssignExpr());
        break;
      default:
        _peekToken = token;
        return expr;
      }
    }
  }

  /**
   * Parses an assignment expression.
   */
  private Expr parseAssignExpr()
    throws IOException
  {
    Expr expr = parseUnary();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '=':
        token = parseToken();

        try {
          if (token == '&') {
            // php/03d6
            expr = expr.createAssignRef(this, parseBitOrExpr());
          }
          else {
            _peekToken = token;

            if (_isIfTest && _quercus.isStrict()) {
              throw error(
                  "assignment without parentheses inside If/While/For "
                      + "test statement; please make sure whether equality "
                      + "was intended instead");
            }

            expr = expr.createAssign(this, parseConditionalExpr());
          }
        } catch (QuercusParseException e) {
          throw e;
        } catch (IOException e) {
          throw error(e.getMessage());
        }
        break;

      case PLUS_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
                                   _factory.createAdd(expr,
                                                      parseConditionalExpr()));
        else // php/03d4
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case MINUS_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
                                   _factory.createSub(expr,
                                                      parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case APPEND_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
              _factory.createAppend(expr,
                  parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case MUL_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
                                   _factory.createMul(expr,
                                                      parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case DIV_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
                                   _factory.createDiv(expr,
                                                      parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case MOD_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
                                   _factory.createMod(expr,
                                                      parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case LSHIFT_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
              _factory.createLeftShift(expr,
                  parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case RSHIFT_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
              _factory.createRightShift(expr,
                  parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case AND_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
              _factory.createBitAnd(expr,
                  parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case OR_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
              _factory.createBitOr(expr,
                  parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case XOR_ASSIGN:
        if (expr.canRead())
          expr = expr.createAssign(this,
              _factory.createBitXor(expr,
                  parseConditionalExpr()));
        else
          expr = expr.createAssign(this, parseConditionalExpr());
        break;

      case INSTANCEOF:
        Expr classNameExpr = parseShiftExpr();

        if (classNameExpr instanceof ConstExpr)
          return _factory.createInstanceOf(expr, classNameExpr.toString());
        else
          return _factory.createInstanceOfVar(expr, classNameExpr);

      default:
        _peekToken = token;
        return expr;
      }
    }
  }


  /**
   * Parses unary term.
   *
   * <pre>
   * unary ::= term
   *       ::= '&' unary
   *       ::= '-' unary
   *       ::= '+' unary
   *       ::= '!' unary
   *       ::= '~' unary
   *       ::= '@' unary
   * </pre>
   */
  private Expr parseUnary()
    throws IOException
  {
    int token = parseToken();
    
    switch (token) {

    case '+':
      {
        Expr expr = parseAssignExpr();

        return _factory.createPlus(expr);
      }

    case '-':
      {
        Expr expr = parseAssignExpr();

        return _factory.createMinus(expr);
      }

    case '!':
      {
        Expr expr = parseAssignExpr();

        return _factory.createNot(expr);
      }

    case '~':
      {
        Expr expr = parseAssignExpr();

        return _factory.createBitNot(expr);
      }

    case '@':
      {
        Expr expr = parseAssignExpr();

        return _factory.createSuppress(expr);
      }

    case CLONE:
      {
        Expr expr = parseAssignExpr();

        return _factory.createClone(expr);
      }

    case INCR:
      {
        Expr expr = parseUnary();

        return _factory.createPreIncrement(expr, 1);
      }

    case DECR:
      {
        Expr expr = parseUnary();

        return _factory.createPreIncrement(expr, -1);
      }
      
    default:
      _peekToken = token;
      
      return parseTerm(true);
    }
  }


  /**
   * Parses a basic term.
   *
   * <pre>
   * term ::= termBase
   *      ::= term '[' index ']'
   *      ::= term '{' index '}'
   *      ::= term '->' name
   *      ::= term '::' name
   *      ::= term '(' a1, ..., an ')'
   * </pre>
   */
  private Expr parseTerm(boolean isParseCall)
    throws IOException
  {
    Expr term = parseTermBase();
    
    while (true) {
      int token = parseToken();

      switch (token) {
      case '[':
        {
          token = parseToken();

          if (token == ']') {
            term = _factory.createArrayTail(getLocation(), term);
          }
          else {
            _peekToken = token;
            Expr index = parseExpr();
            token = parseToken();

            term = _factory.createArrayGet(getLocation(), term, index);
          }

          if (token != ']')
            throw expect("']'", token);
        }
        break;

      case '{':
        {
          Expr index = parseExpr();

          expect('}');

          term = _factory.createCharAt(term, index);
        }
        break;

      case INCR:
        term = _factory.createPostIncrement(term, 1);
        break;

      case DECR:
        term = _factory.createPostIncrement(term, -1);
        break;

      case DEREF:
        term = parseDeref(term);
        break;
        
      case SCOPE:
        term = parseScope(term);
        break;
        

      case '(':
        _peek = token;
        
        if (isParseCall)
          term = parseCall(term);
        else
          return term;
        break;

      default:
        _peekToken = token;
        return term;
      }
    }
  }

  /**
   * Parses a basic term.
   *
   * <pre>
   * term ::= termBase
   *      ::= term '[' index ']'
   *      ::= term '{' index '}'
   * </pre>
   */
  private Expr parseTermArray()
    throws IOException
  {
    Expr term = parseTermBase();

    while (true) {
      int token = parseToken();

      switch (token) {
      case '[':
      {
        token = parseToken();

        if (token == ']') {
          term = _factory.createArrayTail(getLocation(), term);
        }
        else {
          _peekToken = token;
          Expr index = parseExpr();
          token = parseToken();

          term = _factory.createArrayGet(getLocation(), term, index);
        }

          if (token != ']')
            throw expect("']'", token);
        }
        break;

      case '{':
        {
          Expr index = parseExpr();

          expect('}');

          term = _factory.createCharAt(term, index);
        }
        break;

      case INCR:
        term = _factory.createPostIncrement(term, 1);
        break;

      case DECR:
        term = _factory.createPostIncrement(term, -1);
        break;

      default:
        _peekToken = token;
        return term;
      }
    }
  }

  /**
   * Parses a deref
   *
   * <pre>
   * deref ::= term -> IDENTIFIER
   *       ::= term -> IDENTIFIER '(' args ')'
   * </pre>
   */
  private Expr parseDeref(Expr term)
    throws IOException
  {
    String name = null;
    Expr nameExpr = null;

    int token = parseToken();

    if (token == '$') {
      // php/09e0
      _peekToken = token;
      nameExpr = parseTerm(false);
      
      return term.createFieldGet(_factory, nameExpr);
    }
    else if (token == '{') {
      nameExpr = parseExpr();
      expect('}');
      
      return term.createFieldGet(_factory, nameExpr);
    }
    else {
      _peekToken = token;
      name = parseIdentifier();
      
      return term.createFieldGet(_factory, createStringValue(name));
    }
  }

  /**
   * Parses a basic term.
   *
   * <pre>
   * term ::= STRING
   *      ::= LONG
   *      ::= DOUBLE
   * </pre>
   */
  private Expr parseTermBase()
    throws IOException
  {
    int token = parseToken();

    switch (token) {
    case STRING:
      return createString(_lexeme);

    case SYSTEM_STRING:
      {
        ArrayList<Expr> args = new ArrayList<Expr>();
        args.add(createString(_lexeme));
        return _factory.createCall(this, "shell_exec", args);
      }

    case SIMPLE_SYSTEM_STRING:
      {
        ArrayList<Expr> args = new ArrayList<Expr>();
        args.add(parseEscapedString(_lexeme, SIMPLE_STRING_ESCAPE, true));
        return _factory.createCall(this, "shell_exec", args);
      }

    case COMPLEX_SYSTEM_STRING:
      {
        ArrayList<Expr> args = new ArrayList<Expr>();
        args.add(parseEscapedString(_lexeme, COMPLEX_STRING_ESCAPE, true));
        return _factory.createCall(this, "shell_exec", args);
      }

    case SIMPLE_STRING_ESCAPE:
    case COMPLEX_STRING_ESCAPE:
      return parseEscapedString(_lexeme, token, false);

    case BINARY:
      try {
        return createBinary(_lexeme.getBytes("iso-8859-1"));
      } catch (Exception e) {
        throw new QuercusParseException(e);
      }

    case SIMPLE_BINARY_ESCAPE:
    case COMPLEX_BINARY_ESCAPE:
      return parseEscapedString(_lexeme, token, false, false);

    case LONG:
    {
      long value = 0;
      double doubleValue = 0;
      long sign = 1;
      boolean isOverflow = false;
      
      char ch = _lexeme.charAt(0);
      
      int i = 0;
      if (ch == '+') {
        i++;
      } else if (ch == '-') {
        sign = -1;
        i++;
      }
      
      int len = _lexeme.length();
      for (; i < len; i++) {
        int digit = _lexeme.charAt(i) - '0';
        long oldValue = value;
        
        value = value * 10 + digit;
        doubleValue = doubleValue * 10 + digit;
        
        if (value < oldValue)
          isOverflow = true;           
      }
      
      if (! isOverflow)
        return _factory.createLiteral(LongValue.create(value * sign));
      else
        return _factory.createLiteral(new DoubleValue(doubleValue * sign));
    }
    case DOUBLE:
      return _factory.createLiteral(
          new DoubleValue(Double.parseDouble(_lexeme)));

    case NULL:
      return _factory.createNull();

    case TRUE:
      return _factory.createLiteral(BooleanValue.TRUE);

    case FALSE:
      return _factory.createLiteral(BooleanValue.FALSE);

    case '$':
      return parseVariable();

    case NEW:
      return parseNew();
      
    case FUNCTION:
      return parseClosure();

    case INCLUDE:
      return _factory.createInclude(getLocation(), _sourceFile, parseExpr());
    case REQUIRE:
      return _factory.createRequire(getLocation(), _sourceFile, parseExpr());
    case INCLUDE_ONCE:
      return _factory.createIncludeOnce(getLocation(),
          _sourceFile, parseExpr());
    case REQUIRE_ONCE:
      return _factory.createRequireOnce(getLocation(),
          _sourceFile, parseExpr());

    case LIST:
      return parseList();

    case PRINT:
      return parsePrintExpr();

    case EXIT:
      return parseExit();

    case DIE:
      return parseDie();

    case IDENTIFIER:
    case NAMESPACE:
      {
        if (_lexeme.equals("new"))
          return parseNew();

        String name = _lexeme;

        token = parseToken();
        _peekToken = token;

        if (token == '(' && ! _isNewExpr) {
          // shortcut for common case of static function
          
          return parseCall(name);
        }
        else
          return parseConstant(name);
      }

    case '(':
      {
        _isIfTest = false;

        Expr expr = parseExpr();

        expect(')');

        if (expr instanceof ConstExpr) {
          String type = ((ConstExpr) expr).getVar();
          
          int ns = type.lastIndexOf('\\');
          if (ns >= 0)
            type = type.substring(ns + 1);

          if ("bool".equalsIgnoreCase(type)
              || "boolean".equalsIgnoreCase(type))
            return _factory.createToBoolean(parseAssignExpr());
          else if ("int".equalsIgnoreCase(type)
                   || "integer".equalsIgnoreCase(type))
            return _factory.createToLong(parseAssignExpr());
          else if ("float".equalsIgnoreCase(type)
                   || "double".equalsIgnoreCase(type)
                   || "real".equalsIgnoreCase(type))
            return _factory.createToDouble(parseAssignExpr());
          else if ("string".equalsIgnoreCase(type))
            return _factory.createToString(parseAssignExpr());
          else if ("binary".equalsIgnoreCase(type))
            return _factory.createToBinary(parseAssignExpr());
          else if ("unicode".equalsIgnoreCase(type))
            return _factory.createToUnicode(parseAssignExpr());
          else if ("object".equalsIgnoreCase(type))
            return _factory.createToObject(parseAssignExpr());
          else if ("array".equalsIgnoreCase(type))
            return _factory.createToArray(parseAssignExpr());
        }

        return expr;
      }

    case IMPORT:
      {
        String importTokenString = _lexeme;

        token = parseToken();

        if (token == '(') {
          _peekToken = token;

          return parseCall(importTokenString);
        }
        else {
          _peekToken = token;

          return parseImport();
        }
      }

    default:
      throw error(L.l("{0} is an unexpected token, expected an expression.",
                      tokenName(token)));
    }
  }

  /**
   * Parses a basic term.
   *
   * <pre>
   * lhs ::= VARIABLE
   *     ::= lhs '[' expr ']'
   *     ::= lhs -> FIELD
   * </pre>
   */
  private AbstractVarExpr parseLeftHandSide()
    throws IOException
  {
    int token = parseToken();
    AbstractVarExpr lhs = null;

    if (token == '$')
      lhs = parseVariable();
    else
      throw error(L.l("expected variable at {0} as left-hand-side",
                      tokenName(token)));

    while (true) {
      token = parseToken();

      switch (token) {
      case '[':
        {
          token = parseToken();

          if (token == ']') {
            lhs = _factory.createArrayTail(getLocation(), lhs);
          }
          else {
            _peekToken = token;
            Expr index = parseExpr();
            token = parseToken();

            lhs = _factory.createArrayGet(getLocation(), lhs, index);
          }

          if (token != ']')
            throw expect("']'", token);
        }
        break;

      case '{':
        {
          Expr index = parseExpr();

          expect('}');

          lhs = _factory.createCharAt(lhs, index);
        }
        break;

      case DEREF:
        lhs = (AbstractVarExpr) parseDeref(lhs);
        break;

      default:
        _peekToken = token;
        return lhs;
      }
    }
  }

  private Expr parseScope(Expr classNameExpr)
    throws IOException
  {
    int token = parseToken();

    if (isIdentifier(token)) {
      return classNameExpr.createClassConst(this, _lexeme);
    }
    else if (token == '$') {
      token = parseToken();
      
      if (isIdentifier(token)) {
        return classNameExpr.createClassField(this, _lexeme);
      }
      else if (token == '{') {
        Expr expr = parseExpr();
        
        expect('}');
        
        return classNameExpr.createClassField(this, expr);
      }
      else {
        _peekToken = token;
        
        return classNameExpr.createClassField(this, parseTermBase());
      }
    }
    
    throw error(L.l("unexpected token '{0}' in class scope expression",
                    tokenName(token)));
  }
  
  private boolean isIdentifier(int token)
  {
    return token == IDENTIFIER || FIRST_IDENTIFIER_LEXEME <= token;
  }

  /**
   * Parses the next variable
   */
  private AbstractVarExpr parseVariable()
    throws IOException
  {
    int token = parseToken();

    if (token == THIS) {
      return _factory.createThis(_classDef);
    }
    else if (token == '$') {
      _peekToken = token;

      // php/0d6c, php/0d6f
      return _factory.createVarVar(parseTermArray());
    }
    else if (token == '{') {
      AbstractVarExpr expr = _factory.createVarVar(parseExpr());

      expect('}');

      return expr;
    }
    else if (_lexeme == null)
      throw error(L.l("Expected identifier at '{0}'", tokenName(token)));
    
    if (_lexeme.indexOf('\\') >= 0) {
      throw error(L.l("Namespace is not allowed for variable ${0}", _lexeme));
    }

    return _factory.createVar(_function.createVar(_lexeme));
  }
  
  public Expr createVar(String name)
  {
    return _factory.createVar(_function.createVar(name));    
  }
  
  /**
   * Parses the next function
   */
  private Expr parseCall(String name)
    throws IOException
  {
    if (name.equalsIgnoreCase("array"))
      return parseArrayFunction();

    ArrayList<Expr> args = parseArgs();
    
    name = resolveIdentifier(name);

    return _factory.createCall(this, name, args);

    /*
     if (name.equals("each")) {
        if (args.size() != 1)
          throw error(L.l("each requires a single expression"));

        // php/1721
        // we should let ArrayModule.each() handle it
        //return _factory.createEach(args.get(0));
      }
      */
  }

  /**
   * Parses the next constant
   */
  private Expr parseConstant(String name)
  {
    if (name.equals("__FILE__")) {
      return _factory.createFileNameExpr(_parserLocation.getFileName());
    }
    else if (name.equals("__DIR__")) {
      Path parent = Vfs.lookup(_parserLocation.getFileName()).getParent();
      
      return _factory.createDirExpr(parent.getNativePath());
    }
    else if (name.equals("__LINE__"))
      return _factory.createLong(_parserLocation.getLineNumber());
    else if (name.equals("__CLASS__") && _classDef != null)
      return createString(_classDef.getName());
    else if (name.equals("__FUNCTION__")) {
      return createString(_function.getName());
    }
    else if (name.equals("__METHOD__")) {
      if (_classDef != null) {
        if (_function.getName().length() != 0)
          return createString(_classDef.getName() + "::" + _function.getName());
        else
          return createString(_classDef.getName());
      }
      else
        return createString(_function.getName());
    }
    else if (name.equals("__NAMESPACE__")) {
      return createString(_namespace);
    }
    
    name = resolveIdentifier(name);
    
    if (name.startsWith("\\"))
      name = name.substring(1);
    
    return _factory.createConst(name);
  }

  /**
   * Parses the next function
   */
  private Expr parseCall(Expr name)
    throws IOException
  {
    return name.createCall(this, getLocation(), parseArgs());
  }

  private ArrayList<Expr> parseArgs()
    throws IOException
  {
    expect('(');
    
    ArrayList<Expr> args = new ArrayList<Expr>();
    
    int token;

    while ((token = parseToken()) > 0 && token != ')') {
      boolean isRef = false;

      if (token == '&')
        isRef = true;
      else
        _peekToken = token;

      Expr expr = parseExpr();

      if (isRef)
        expr = expr.createRef(this);

      args.add(expr);

      token = parseToken();
      if (token == ')')
        break;
      else if (token != ',')
        throw expect("','", token);
    }
    
    return args;
  }

  public String getSelfClassName()
  {
    if (_classDef == null)
      throw error(L.l("'self' is not valid because there is no active class."));
    
    return _classDef.getName();
  }

  public String getParentClassName()
  {
    if (_classDef == null)
      throw error(L.l(
          "'parent' is not valid because there is no active class."));
    
    return _classDef.getParentName();
  }
  
  /**
   * Parses the new expression
   */
  private Expr parseNew()
    throws IOException
  {
    String name = null;
    Expr nameExpr = null;

    boolean isNewExpr = _isNewExpr;
    _isNewExpr = true;

    //nameExpr = parseTermBase();
    nameExpr = parseTerm(false);

    _isNewExpr = isNewExpr;

    // XX: unicode issues?
    if (nameExpr.isLiteral() || nameExpr instanceof ConstExpr) {
      name = nameExpr.evalConstant().toString();

      // php/0957
      if ("self".equals(name) && _classDef != null)
        name = _classDef.getName();
      else if ("parent".equals(name) && getParentClassName() != null)
        name = getParentClassName().toString();
      else {
        // name = resolveIdentifier(name);
      }
    }

    int token = parseToken();

    ArrayList<Expr> args = new ArrayList<Expr>();

    if (token != '(')
      _peekToken = token;
    else {
      while ((token = parseToken()) > 0 && token != ')') {
        _peekToken = token;

        args.add(parseExpr());

        token = parseToken();
        if (token == ')')
          break;
        else if (token != ',')
          throw error(L.l("expected ','"));
      }
    }

    Expr expr;

    if (name != null)
      expr =  _factory.createNew(getLocation(), name, args);
    else
      expr = _factory.createVarNew(getLocation(), nameExpr, args);

    return expr;
  }

  /**
   * Parses the include expression
   */
  private Expr parseInclude()
    throws IOException
  {
    Expr name = parseExpr();

    return _factory.createInclude(getLocation(), _sourceFile, name);
  }

  /**
   * Parses the list(...) = value expression
   */
  private Expr parseList()
    throws IOException
  {
    ListHeadExpr leftVars = parseListHead();

    expect('=');

    Expr value = parseConditionalExpr();

    return _factory.createList(this, leftVars, value);
  }

  /**
   * Parses the list(...) expression
   */
  private ListHeadExpr parseListHead()
    throws IOException
  {
    expect('(');

    int peek = parseToken();

    ArrayList<Expr> leftVars = new ArrayList<Expr>();

    while (peek > 0 && peek != ')') {
      if (peek == LIST) {
        leftVars.add(parseListHead());

        peek = parseToken();
      }
      else if (peek != ',') {
        _peekToken = peek;

        Expr left = parseTerm(true);

        leftVars.add(left);

        left.assign(this);

        peek = parseToken();
      }
      else {
        leftVars.add(null);
      }

      if (peek == ',')
        peek = parseToken();
      else
        break;
    }

    if (peek != ')')
      throw error(L.l("expected ')'"));

    return _factory.createListHead(leftVars);
  }

  /**
   * Parses the exit/die expression
   */
  private Expr parseExit()
    throws IOException
  {
    int token = parseToken();
    _peekToken = token;

    if (token == '(') {
      ArrayList<Expr> args = parseArgs();

      if (args.size() > 0)
        return _factory.createExit(args.get(0));
      else
        return _factory.createExit(null);
    }
    else {
      return _factory.createExit(null);
    }
  }

  /**
   * Parses the exit/die expression
   */
  private Expr parseDie()
    throws IOException
  {
    int token = parseToken();
    _peekToken = token;

    if (token == '(') {
      ArrayList<Expr> args = parseArgs();

      if (args.size() > 0)
        return _factory.createDie(args.get(0));
      else
        return _factory.createDie(null);
    }
    else {
      return _factory.createDie(null);
    }
  }

  /**
   * Parses the array() expression
   */
  private Expr parseArrayFunction()
    throws IOException
  {
    String name = _lexeme;

    int token = parseToken();

    if (token != '(')
      throw error(L.l("Expected '('"));

    ArrayList<Expr> keys = new ArrayList<Expr>();
    ArrayList<Expr> values = new ArrayList<Expr>();

    while ((token = parseToken()) > 0 && token != ')') {
      _peekToken = token;

      Expr value = parseRefExpr();

      token = parseToken();

      if (token == ARRAY_RIGHT) {
        Expr key = value;

        value = parseRefExpr();

        keys.add(key);
        values.add(value);

        token = parseToken();
      }
      else {
        keys.add(null);
        values.add(value);
      }

      if (token == ')')
        break;
      else if (token != ',')
        throw error(L.l("expected ','"));
    }

    return _factory.createArrayFun(keys, values);
  }

  /**
   * Parses a Quercus import.
   */
  private Expr parseImport()
    throws IOException
  {
    boolean isWildcard = false;
    boolean isIdentifierStart = true;

    StringBuilder sb = new StringBuilder();

    while (true) {
      int token = parseToken();

      if (token == IDENTIFIER) {
        sb.append(_lexeme);

        token = parseToken();

        if (token == '.') {
          sb.append('.');
        }
        else {
          _peekToken = token;
          break;
        }
      }
      else if (token == '*') {
        if (sb.length() > 0)
          sb.setLength(sb.length() - 1);

        isWildcard = true;
        break;
      }
      else {
        throw error(L.l("'{0}' is an unexpected token in import",
                        tokenName(token)));
      }
    }

    //expect(';');

    return _factory.createImport(getLocation(), sb.toString(), isWildcard);
  }

  /**
   * Parses the next token.
   */
  private int parseToken()
    throws IOException
  {
    int peekToken = _peekToken;
    if (peekToken > 0) {
      _peekToken = 0;
      return peekToken;
    }

    while (true) {
      int ch = read();

      switch (ch) {
      case -1:
        return -1;

      case ' ': case '\t': case '\n': case '\r':
        break;

      case '#':
        while ((ch = readByte()) != '\n' && ch != '\r' && ch >= 0) {
          if (ch != '?') {
          }
          else if ((ch = readByte()) != '>') {
            _peek = ch;
          }
          else {
            ch = readByte();
            if (ch == '\r')
              ch = readByte();
            if (ch != '\n')
              _peek = ch;

            return parsePhpText();
          }
        }
        break;

      case '"':
      {
        String heredocEnd = _heredocEnd;
        _heredocEnd = null;

        int result = parseEscapedString('"');
        _heredocEnd = heredocEnd;

        return result;
      }
      case '`':
        {
          int token = parseEscapedString('`');

          switch (token) {
          case STRING:
            return SYSTEM_STRING;
          case SIMPLE_STRING_ESCAPE:
            return SIMPLE_SYSTEM_STRING;
          case COMPLEX_STRING_ESCAPE:
            return COMPLEX_SYSTEM_STRING;
          default:
            throw new IllegalStateException();
          }
        }

      case '\'':
        parseStringToken('\'');
        return STRING;

      case ';': case '$': case '(': case ')': case '@':
      case '[': case ']': case ',': case '{': case '}':
      case '~':
        return ch;

      case '+':
        ch = read();
        if (ch == '=')
          return PLUS_ASSIGN;
        else if (ch == '+')
          return INCR;
        else
          _peek = ch;

        return '+';

      case '-':
        ch = read();
        if (ch == '>')
          return DEREF;
        else if (ch == '=')
          return MINUS_ASSIGN;
        else if (ch == '-')
          return DECR;
        else
          _peek = ch;

        return '-';

      case '*':
        ch = read();
        if (ch == '=')
          return MUL_ASSIGN;
        else
          _peek = ch;

        return '*';

      case '/':
        ch = read();
        if (ch == '=')
          return DIV_ASSIGN;
        else if (ch == '/') {
          while (ch >= 0) {
            if (ch == '\n' || ch == '\r') {
              break;
            }
            else if (ch == '?') {
              ch = readByte();

              if (ch == '>') {
                ch = readByte();

                if (ch == '\r')
                  ch = readByte();
                if (ch != '\n')
                  _peek = ch;

                return parsePhpText();
              }
            }
            else
              ch = readByte();
          }
          break;
        }
        else if (ch == '*') {
          parseMultilineComment();
          break;
        }
        else
          _peek = ch;

        return '/';

      case '%':
        ch = read();
        if (ch == '=')
          return MOD_ASSIGN;
        else if (ch == '>') {
          ch = read();
          if (ch == '\r')
            ch = read();
          if (ch != '\n')
            _peek = ch;

          return parsePhpText();
        }
        else
          _peek = ch;

        return '%';

      case ':':
        ch = read();
        if (ch == ':')
          return SCOPE;
        else
          _peek = ch;

        return ':';

      case '=':
        ch = read();
        if (ch == '=') {
          ch = read();
          if (ch == '=')
            return EQUALS;
          else {
            _peek = ch;
            return EQ;
          }
        }
        else if (ch == '>')
          return ARRAY_RIGHT;
        else {
          _peek = ch;
          return '=';
        }

      case '!':
        ch = read();
        if (ch == '=') {
          ch = read();
          if (ch == '=')
            return NEQUALS;
          else {
            _peek = ch;
            return NEQ;
          }
        }
        else {
          _peek = ch;
          return '!';
        }

      case '&':
        ch = read();
        if (ch == '&')
          return C_AND;
        else if (ch == '=')
          return AND_ASSIGN;
        else {
          _peek = ch;
          return '&';
        }

      case '^':
        ch = read();
        if (ch == '=')
          return XOR_ASSIGN;
        else
          _peek = ch;

        return '^';

      case '|':
        ch = read();
        if (ch == '|')
          return C_OR;
        else if (ch == '=')
          return OR_ASSIGN;
        else {
          _peek = ch;
          return '|';
        }

      case '<':
        ch = read();
        if (ch == '<') {
          ch = read();

          if (ch == '=')
            return LSHIFT_ASSIGN;
          else if (ch == '<') {
            return parseHeredocToken();
          }
          else
            _peek = ch;

          return LSHIFT;
        }
        else if (ch == '=')
          return LEQ;
        else if (ch == '>')
          return NEQ;
        else if (ch == '/') {
          StringBuilder sb = new StringBuilder();

          if (! parseTextMatch(sb, "script"))
            throw error(L.l("expected 'script' at '{0}'", sb));

          expect('>');

          return parsePhpText();
        }
        else
          _peek = ch;

        return '<';

      case '>':
        ch = read();
        if (ch == '>') {
          ch = read();

          if (ch == '=')
            return RSHIFT_ASSIGN;
          else
            _peek = ch;

          return RSHIFT;
        }
        else if (ch == '=')
          return GEQ;
        else
          _peek = ch;

        return '>';

      case '?':
        ch = read();
        if (ch == '>') {
          ch = read();
          if (ch == '\r')
            ch = read();
          if (ch != '\n')
            _peek = ch;

          return parsePhpText();
        }
        else
          _peek = ch;

        return '?';

      case '.':
        ch = read();

        if (ch == '=')
          return APPEND_ASSIGN;

        _peek = ch;

        if ('0' <= ch && ch <= '9')
          return parseNumberToken('.');
        else
          return '.';

      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        return parseNumberToken(ch);

      default:

        if (ch == 'b') {
          int ch2 = read();

          if (ch2 == '\'') {
            parseStringToken('\'', false);
            return BINARY;
          }
          else if (ch2 == '"') {

            int token = parseEscapedString('"', false);
            switch (token) {
              case STRING:
                return BINARY;
              case SIMPLE_STRING_ESCAPE:
                return SIMPLE_BINARY_ESCAPE;
              case COMPLEX_STRING_ESCAPE:
                return COMPLEX_BINARY_ESCAPE;
              default:
                return token;
            }
          }
          else
            _peek = ch2;
        }
        
        return parseNamespaceIdentifier(ch);
      }
    }
  }

  private String parseIdentifier()
    throws IOException
  {
    int token = _peekToken;
    _peekToken = -1;
    
    if (token <= 0)
      token = parseIdentifier(read());

    if (token != IDENTIFIER && token < FIRST_IDENTIFIER_LEXEME)
      throw error(L.l("expected identifier at {0}.", tokenName(token)));
    
    if (_lexeme.indexOf('\\') >= 0) {
      throw error(L.l("namespace identifier is not allowed at '{0}'",
                      _lexeme));
    }
    else if (_peek == '\\') {
        throw error(L.l("namespace identifier is not allowed at '{0}\\'",
                        _lexeme));
    }
    
    return _lexeme;
  }
  

  private String parseNamespaceIdentifier()
    throws IOException
  {
    int token = _peekToken;
    _peekToken = -1;
    
    if (token <= 0)
      token = parseNamespaceIdentifier(read());

    if (token == IDENTIFIER)
      return resolveIdentifier(_lexeme);
    else if (FIRST_IDENTIFIER_LEXEME <= token)
      return resolveIdentifier(_lexeme);
    else
      throw error(L.l("expected identifier at {0}.", tokenName(token)));
  }

  public String getSystemFunctionName(String name)
  {
    int p = name.lastIndexOf('\\');
    
    if (p < 0)
      return name;
    
    String systemName = name.substring(p + 1);
    
    if (_quercus.findFunction(systemName) != null)
      return systemName;
    else
      return null;
  }
  private String resolveIdentifier(String id)
  {
    if (id.startsWith("\\"))
      return id.substring(1);
    
    int ns = id.indexOf('\\');
    
    if (ns > 0) {
      String prefix = id.substring(0, ns);
      
      String use = _namespaceUseMap.get(prefix);
      
      if (use != null)
        return use + id.substring(ns);
      else if (_namespace.equals(""))
        return id;
      else
        return _namespace + "\\" + id;
    }
    else {
      String use = _namespaceUseMap.get(id);
      
      if (use != null)
        return use;
      else if (_namespace.equals(""))
        return id;
      else
        return _namespace + '\\' + id;
    }
  }

  private int parseIdentifier(int ch)
    throws IOException
  {
    for (; Character.isWhitespace(ch); ch = read()) {
    }
    
    if (isIdentifierStart(ch)) {
      _sb.setLength(0);
      _sb.append((char) ch);

      for (ch = read(); isIdentifierPart(ch); ch = read()) {
        _sb.append((char) ch);
      }

      _peek = ch;
      
      return lexemeToToken();
    }

    throw error("expected identifier at " + (char) ch);
  }

  private int parseNamespaceIdentifier(int ch)
    throws IOException
  {
    for (; Character.isWhitespace(ch); ch = read()) {
    }
    
    if (isNamespaceIdentifierStart(ch)) {
      _sb.setLength(0);
      _sb.append((char) ch);

      for (ch = read(); isNamespaceIdentifierPart(ch); ch = read()) {
        _sb.append((char) ch);
      }

      _peek = ch;
        
      return lexemeToToken();
    }

    throw error("unknown lexeme:" + (char) ch);
  }

  private int lexemeToToken()
    throws IOException
  {
    _lexeme = _sb.toString();

    // the 'static' reserved keyword vs late static binding (static::$a)
    if (_peek == ':' && "static".equals(_lexeme))
      return IDENTIFIER;

    int reserved = _reserved.get(_lexeme);

    if (reserved > 0)
      return reserved;

    reserved = _insensitiveReserved.get(_lexeme.toLowerCase());
    if (reserved > 0)
      return reserved;
    else
      return IDENTIFIER;
  }

  /**
   * Parses a multiline comment.
   */
  private void parseMultilineComment()
    throws IOException
  {
    int ch = readByte();

    if (ch == '*') {
      _sb.setLength(0);
      _sb.append('/');
      _sb.append('*');

      do {
        if (ch != '*') {
          _sb.append((char) ch);
        }
        else if ((ch = readByte()) == '/') {
          _sb.append('*');
          _sb.append('/');

          _comment = _sb.toString();

          return;
        }
        else {
          _sb.append('*');
          _peek = ch;
        }
      } while ((ch = readByte()) >= 0);

      _comment = _sb.toString();
    }
    else if (ch >= 0) {
      do {
        if (ch != '*') {
        }
        else if ((ch = readByte()) == '/')
          return;
        else
          _peek = ch;
      } while ((ch = readByte()) >= 0);
    }
  }

  /**
   * Parses quercus text
   */
  private int parsePhpText()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    int ch = read();
    while (ch > 0) {
      if (ch == '<') {
        int ch2;
        int ch3;

        if ((ch = read()) == 's' || ch == 'S') {
          _peek = ch;
          if (parseScriptBegin(sb)) {
            _lexeme = sb.toString();
            return TEXT;
          }
          ch = read();
        }
        else if (ch == '%') {
          if ((ch = read()) == '=') {
            _lexeme = sb.toString();

            return TEXT_ECHO;
          }
          else if (Character.isWhitespace(ch)) {
            _lexeme = sb.toString();

            return TEXT;
          }
        }
        else if (ch != '?') {
          sb.append('<');
        }
        else if ((ch = read()) == '=') {
          _lexeme = sb.toString();

          return TEXT_ECHO;
        }
        else {
          _lexeme = sb.toString();
          _peek = ch;

          if (ch == 'p' || ch == 'P')
            return TEXT_PHP;
          else
            return TEXT;
        }
      }
      else {
        sb.append((char) ch);

        ch = read();
      }
    }

    _lexeme = sb.toString();

    return TEXT;
  }

  /**
   * Parses the <script language="quercus"> opening
   */
  private boolean parseScriptBegin(StringBuilder sb)
    throws IOException
  {
    int begin = sb.length();

    sb.append('<');

    if (! parseTextMatch(sb, "script"))
      return false;

    parseWhitespace(sb);

    if (! parseTextMatch(sb, "language="))
      return false;

    int openingParentheses = read();

    if(openingParentheses == '\'' || openingParentheses == '"'){
      if (! parseTextMatch(sb, "php")){
        sb.append((char) openingParentheses);
        return false;
      }

      int closingParentheses = read();
      if(openingParentheses != closingParentheses){
        sb.append((char) closingParentheses);
        return false;
      }
    }


    parseWhitespace(sb);

    int ch = read();

    if (ch == '>') {
      sb.setLength(begin);
      return true;
    }
    else {
      _peek = ch;
      return false;
    }
  }

  private boolean parseTextMatch(StringBuilder sb, String text)
    throws IOException
  {
    int len = text.length();

    for (int i = 0; i < len; i++) {
      int ch = read();

      if (ch < 0)
        return false;

      if (Character.toLowerCase(ch) != text.charAt(i)) {
        _peek = ch;
        return false;
      }
      else
        sb.append((char) ch);
    }

    return true;
  }

  private void parseWhitespace(StringBuilder sb)
    throws IOException
  {
    int ch;

    while (Character.isWhitespace((ch = read()))) {
      sb.append((char) ch);
    }

    _peek = ch;
  }

  private void parseStringToken(int end)
    throws IOException
  {
    parseStringToken(end, isUnicodeSemantics());
  }

  /**
   * Parses the next string token.
   */
  private void parseStringToken(int end, boolean isUnicode)
    throws IOException
  {
    _sb.setLength(0);

    int ch;

    for (ch = read(); ch >= 0 && ch != end; ch = read()) {
      if (ch == '\\') {
        ch = read();

        if (isUnicode) {
          if (ch == 'u') {
            int value = parseUnicodeEscape(false);
            
            if (value < 0) {
              _sb.append('\\');
              _sb.append('u');
            }
            else
              _sb.append(Character.toChars(value));
            
            continue;
          }
          else if (ch == 'U') {
            int value = parseUnicodeEscape(true);
            
            if (value < 0) {
              _sb.append('\\');
              _sb.append('U');
            }
            else
              _sb.append(Character.toChars(value));
            
            continue;
          }
        }

        if (end == '"') {
          _sb.append('\\');

          if (ch >= 0)
            _sb.append((char) ch);
        }
        else {
          switch (ch) {
          case '\'': case '\\':
            _sb.append((char) ch);
            break;
          default:
            _sb.append('\\');
            _sb.append((char) ch);
            break;
          }
        }
      }
      else
        _sb.append((char) ch);
    }

    _lexeme = _sb.toString();
  }

  /**
   * Parses the next heredoc token.
   */
  private int parseHeredocToken()
    throws IOException
  {
    _sb.setLength(0);

    int ch;

    // eat whitespace
    while ((ch = read()) >= 0 && (ch == ' ' || ch == '\t')) {
    }
    _peek = ch;

    while ((ch = read()) >= 0 && ch != '\r' && ch != '\n') {
      _sb.append((char) ch);
    }

    _heredocEnd = _sb.toString();

    if (ch == '\n') {
    }
    else if (ch == '\r') {
      ch = read();
      if (ch != '\n')
        _peek = ch;
    }
    else
      _peek = ch;

    return parseEscapedString('"');
  }

  /**
   * Parses the next string
   * XXX: parse as Unicode if and only if unicode.semantics is on.
   */
  private Expr parseEscapedString(String prefix, int token, boolean isSystem)
    throws IOException
  {
    return parseEscapedString(prefix, token, isSystem, true);
  }

  /**
   * Parses the next string
   */
  private Expr parseEscapedString(String prefix,
                                   int token,
                                   boolean isSystem,
                                   boolean isUnicode)
    throws IOException
  {
    Expr expr;

    if (isUnicode)
      expr = createString(prefix);
    else {
      // XXX: getBytes isn't correct
      expr = createBinary(prefix.getBytes("iso-8859-1"));
    }

    while (true) {
      Expr tail;

      if (token == COMPLEX_STRING_ESCAPE
          || token == COMPLEX_BINARY_ESCAPE) {
        tail = parseExpr();

        expect('}');
      }
      else if (token == SIMPLE_STRING_ESCAPE
               || token == SIMPLE_BINARY_ESCAPE) {
        int ch = read();

        _sb.setLength(0);

        for (; isIdentifierPart(ch); ch = read()) {
          _sb.append((char) ch);
        }

        _peek = ch;

        String varName = _sb.toString();

        if (varName.equals("this"))
          tail = _factory.createThis(_classDef);
        else
          tail = _factory.createVar(_function.createVar(varName));

        // php/013n
        if (((ch = read()) == '[' || ch == '-')) {
          if (ch == '[') {
            tail = parseSimpleArrayTail(tail);

            ch = read();
          }
          else {
            if ((ch = read()) != '>') {
              tail = _factory.createAppend(tail, createString("-"));
            }
            else if (isIdentifierPart(ch = read())) {
              _sb.clear();
              for (; isIdentifierPart(ch); ch = read()) {
                _sb.append((char) ch);
              }

              tail = tail.createFieldGet(_factory,
                  createStringValue(_sb.toString()));
            }
            else {
              tail = _factory.createAppend(tail, createString("->"));
            }

            _peek = ch;
          }
        }

        _peek = ch;
      }
      else
        throw error("unexpected token");

      expr = _factory.createAppend(expr, tail);

      if (isSystem)
        token = parseEscapedString('`');
      else
        token = parseEscapedString('"');

      if (_sb.length() > 0) {
        Expr string;

        if (isUnicode)
          string = createString(_sb.toString());
        else
          string = createBinary(_sb.toString().getBytes("iso-8859-1"));

        expr = _factory.createAppend(expr, string);
      }

      if (token == STRING)
        return expr;
    }
  }

  /**
   * Parses the next string
   */
  private Expr parseSimpleArrayTail(Expr tail)
    throws IOException
  {
    int ch = read();

    _sb.clear();

    if (ch == '$') {
      for (ch = read(); isIdentifierPart(ch); ch = read()) {
        _sb.append((char) ch);
      }

      VarExpr var = _factory.createVar(_function.createVar(_sb.toString()));

      tail = _factory.createArrayGet(getLocation(), tail, var);
    }
    else if ('0' <= ch && ch <= '9') {
      long index = ch - '0';

      for (ch = read();
           '0' <= ch && ch <= '9';
           ch = read()) {
        index = 10 * index + ch - '0';
      }

      tail = _factory.createArrayGet(getLocation(),
          tail, _factory.createLong(index));
    }
    else if (isIdentifierPart(ch)) {
      for (; isIdentifierPart(ch); ch = read()) {
        _sb.append((char) ch);
      }

      Expr constExpr = _factory.createConst(_sb.toString());

      tail = _factory.createArrayGet(getLocation(), tail, constExpr);
    }
    else
      throw error(L.l("Unexpected character at {0}",
                      String.valueOf((char) ch)));

    if (ch != ']')
      throw error(L.l("Expected ']' at {0}",
                      String.valueOf((char) ch)));

    return tail;
  }

  private Expr createString(String lexeme)
  {
    // XXX: see QuercusParser.parseDefault for _quercus == null
    if (isUnicodeSemantics())
      return _factory.createUnicode(lexeme);
    else
      return _factory.createString(lexeme);
  }

  private StringValue createStringValue(String lexeme)
  {
    // XXX: see QuercusParser.parseDefault for _quercus == null
    if (isUnicodeSemantics())
      return new UnicodeBuilderValue(lexeme);
    else
      return new ConstStringValue(lexeme);
  }

  private Expr createBinary(byte []bytes)
    throws IOException
  {
    // XXX: see QuercusParser.parseDefault for _quercus == null
    // php/0ch1, php/0350
    // return _factory.createBinary(bytes);

    if (isUnicodeSemantics())
      return _factory.createBinary(bytes);
    else {
      try {
        return _factory.createString(
            new String(bytes, 0, bytes.length, "iso-8859-1"));
      } catch (UnsupportedEncodingException e) {
        throw new QuercusParseException(e);
      }
    }
  }

  /**
   * XXX: parse as Unicode if and only if unicode.semantics is on.
   */
  private int parseEscapedString(char end)
    throws IOException
  {
    return parseEscapedString(end, isUnicodeSemantics());
  }

  /**
   * Parses the next string
   */
  private int parseEscapedString(char end, boolean isUnicode)
    throws IOException
  {
    _sb.setLength(0);

    int ch;

    while ((ch = read()) > 0) {
      if (_heredocEnd == null && ch == end) {
        _lexeme = _sb.toString();
        return STRING;
      }
      else if (ch == '\\') {
        ch = read();

        switch (ch) {
        case '0': case '1': case '2': case '3':
          _sb.append((char) parseOctalEscape(ch));
          break;
        case 't':
          _sb.append('\t');
          break;
        case 'r':
          _sb.append('\r');
          break;
        case 'n':
          _sb.append('\n');
          break;
        case '"':
        case '`':
          if (_heredocEnd != null)
            _sb.append('\\');

          _sb.append((char) ch);
          break;
        case '$':
        case '\\':
          _sb.append((char) ch);
          break;
        case 'x': {
          int value = parseHexEscape();
          
          if (value >= 0)
            _sb.append((char) value);
          else {
            _sb.append('\\');
            _sb.append('x');
          }
            
          break;
        }
        case 'u':
          if (isUnicode) {
            int result = parseUnicodeEscape(false);
            
            if (result < 0) {
              _sb.append('\\');
              _sb.append('u');
            }
            else
              _sb.append(Character.toChars(result));
          }
          else {
            _sb.append('\\');
            _sb.append((char) ch);
          }
          break;
        case 'U':
          if (isUnicode) {
            int result = parseUnicodeEscape(true);
            
            if (result < 0) {
              _sb.append('\\');
              _sb.append('U');
            }
            else
              _sb.append(Character.toChars(result));
          }
          else {
            _sb.append('\\');
            _sb.append((char) ch);
          }
          break;
        case '{':
          ch = read();
          _peek = ch;
          if (ch == '$' && _heredocEnd == null)
            _sb.append('{');
          else
            _sb.append("\\{");
          break;
        default:
          _sb.append('\\');
          _sb.append((char) ch);
          break;
        }
      }
      else if (ch == '$') {
        ch = read();

        if (ch == '{') {
          _peek = '$';
          _lexeme = _sb.toString();
          return COMPLEX_STRING_ESCAPE;
        }
        else if (isIdentifierStart(ch)) {
          _peek = ch;
          _lexeme = _sb.toString();
          return SIMPLE_STRING_ESCAPE;
        }
        else {
          _sb.append('$');
          _peek = ch;
        }
      }
      else if (ch == '{') {
        ch = read();

        if (ch == '$') {
          _peek = ch;
          _lexeme = _sb.toString();
          return COMPLEX_STRING_ESCAPE;
        }
        else {
          _peek = ch;
          _sb.append('{');
        }
      }
      /* quercus/013c
      else if ((ch == '\r' || ch == '\n') && _heredocEnd == null)
        throw error(L.l("unexpected newline in string."));
      */
      else {
        _sb.append((char) ch);

        if (_heredocEnd == null || ! _sb.endsWith(_heredocEnd)) {
        }
        else if (
            _sb.length() == _heredocEnd.length()
            || _sb.charAt(_sb.length() - _heredocEnd.length() - 1) == '\n'
            || _sb.charAt(_sb.length() - _heredocEnd.length() - 1) == '\r'
            ) {
          _sb.setLength(_sb.length() - _heredocEnd.length());

          if (_sb.length() > 0 && _sb.charAt(_sb.length() - 1) == '\n')
            _sb.setLength(_sb.length() - 1);
          if (_sb.length() > 0 && _sb.charAt(_sb.length() - 1) == '\r')
            _sb.setLength(_sb.length() - 1);

          _heredocEnd = null;
          _lexeme = _sb.toString();
          return STRING;
        }
      }
    }

    _lexeme = _sb.toString();

    return STRING;
  }
 
  private boolean isNamespaceIdentifierStart(int ch)
  {
    return isIdentifierStart(ch) || ch == '\\';
  }
  
  private boolean isIdentifierStart(int ch)
  {
    if (ch < 0)
      return false;
    else
      return (ch >= 'a' && ch <= 'z' 
              || ch >= 'A' && ch <= 'Z'
              || ch == '_'
              || Character.isLetter(ch));
  }
  
  private boolean isNamespaceIdentifierPart(int ch)
  {
    return isIdentifierPart(ch) || ch == '\\';
  }
  
  private boolean isIdentifierPart(int ch)
  {
    if (ch < 0)
      return false;
    else
      return (ch >= 'a' && ch <= 'z'
              || ch >= 'A' && ch <= 'Z'
              || ch >= '0' && ch <= '9'
              || ch == '_'
              || Character.isLetterOrDigit(ch));
  }

  private int parseOctalEscape(int ch)
    throws IOException
  {
    int value = ch - '0';

    ch = read();
    if (ch < '0' || ch > '7') {
      _peek = ch;
      return value;
    }

    value = 8 * value + ch - '0';

    ch = read();
    if (ch < '0' || ch > '7') {
      _peek = ch;
      return value;
    }

    value = 8 * value + ch - '0';

    return value;
  }

  private int parseHexEscape()
    throws IOException
  {
    int value = 0;

    int ch = read();

    if ('0' <= ch && ch <= '9')
      value = 16 * value + ch - '0';
    else if ('a' <= ch && ch <= 'f')
      value = 16 * value + 10 + ch - 'a';
    else if ('A' <= ch && ch <= 'F')
      value = 16 * value + 10 + ch - 'A';
    else {
      _peek = ch;
      return -1;
    }

    ch = read();

    if ('0' <= ch && ch <= '9')
      value = 16 * value + ch - '0';
    else if ('a' <= ch && ch <= 'f')
      value = 16 * value + 10 + ch - 'a';
    else if ('A' <= ch && ch <= 'F')
      value = 16 * value + 10 + ch - 'A';
    else {
      _peek = ch;
      return value;
    }

    return value;
  }

  private int parseUnicodeEscape(boolean isLongForm)
    throws IOException
  {
    int codePoint = parseHexEscape();

    if (codePoint < 0)
      return -1;
    
    int low = parseHexEscape();
    
    if (low < 0)
      return codePoint;
    
    codePoint = codePoint * 256 + low;

    if (isLongForm) {
      low = parseHexEscape();
      
      if (low < 0)
        return codePoint;
      
      codePoint = codePoint * 256 + low;
    }

    return codePoint;
  }

  /**
   * Parses the next number.
   */
  private int parseNumberToken(int ch)
    throws IOException
  {
    int ch0 = ch;

    if (ch == '0') {
      ch = read();
      if (ch == 'x' || ch == 'X')
        return parseHex();
      else if (ch == '0')
        return parseNumberToken(ch);
      else {
        _peek = ch;
        ch = '0';
      }
    }

    _sb.setLength(0);

    int token = LONG;

    for (; '0' <= ch && ch <= '9'; ch = read()) {
      _sb.append((char) ch);
    }

    if (ch == '.') {
      token = DOUBLE;

      _sb.append((char) ch);

      for (ch = read(); '0' <= ch && ch <= '9'; ch = read()) {
        _sb.append((char) ch);
      }
    }

    if (ch == 'e' || ch == 'E') {
      token = DOUBLE;

      _sb.append((char) ch);

      ch = read();
      if (ch == '+' || ch == '-') {
        _sb.append((char) ch);
        ch = read();
      }

      if ('0' <= ch && ch <= '9') {
        for (; '0' <= ch && ch <= '9'; ch = read()) {
          _sb.append((char) ch);
        }
      }
      else
        throw error(L.l("illegal exponent"));
    }

    _peek = ch;

    if (ch0 == '0' && token == LONG) {
      int len = _sb.length();
      int value = 0;

      for (int i = 0; i < len; i++) {
        ch = _sb.charAt(i);
        if ('0' <= ch && ch <= '7')
          value = value * 8 + ch - '0';
        else
          break;
      }

      _lexeme = String.valueOf(value);
    }
    else {
      _lexeme = _sb.toString();
    }

    return token;
  }

  /**
   * Parses the next as hex
   */
  private int parseHex()
    throws IOException
  {
    long value = 0;
    double dValue = 0;

    while (true) {
      int ch = read();

      if ('0' <= ch && ch <= '9') {
        value = 16 * value + ch - '0';
        dValue = 16 * dValue + ch - '0';
      }
      else if ('a' <= ch && ch <= 'f') {
        value = 16 * value + ch - 'a' + 10;
        dValue = 16 * dValue + ch - 'a' + 10;
      }
      else if ('A' <= ch && ch <= 'F') {
        value = 16 * value + ch - 'A' + 10;
        dValue = 16 * dValue + ch - 'A' + 10;
      }
      else {
        _peek = ch;
        break;
      }
    }

    if (value == dValue) {
      _lexeme = String.valueOf(value);
      return LONG;
    }
    else {
      _lexeme = String.valueOf(dValue);

      return DOUBLE;
    }
  }

  /**
   * Parses the next as octal
   */
  private int parseOctal(int ch)
    throws IOException
  {
    long value = 0;
    double dValue = 0;

    while (true) {
      if ('0' <= ch && ch <= '7') {
        value = 8 * value + ch - '0';
        dValue = 8 * dValue + ch - '0';
      }
      else {
        while ('0' <= ch && ch <= '9') {
          ch = read();
        }

        _peek = ch;
        break;
      }

      ch = read();
    }

    if (value == dValue) {
      _lexeme = String.valueOf(value);

      return LONG;
    }
    else {
      _lexeme = String.valueOf(dValue);

      return DOUBLE;
    }
  }

  private void expect(int expect)
    throws IOException
  {
    int token = parseToken();

    if (token != expect)
      throw error(L.l("expected {0} at {1}",
                      tokenName(expect),
                      tokenName(token)));
  }

  /**
   * Reads the next character.
   */
  private int read()
    throws IOException
  {
    int peek = _peek;

    if (peek >= 0) {
      _peek = -1;
      return peek;
    }

    try {
      int ch = _is.readChar();

      if (ch == '\r') {
        _parserLocation.incrementLineNumber();
        _hasCr = true;
      }
      else if (ch == '\n' && ! _hasCr) {
        _parserLocation.incrementLineNumber();
      }
      else
        _hasCr = false;

      return ch;
    } catch (CharConversionException e) {
      throw new QuercusParseException(getFileName() + ":" + getLine()
          + ": " + e
          + "\nCheck that the script-encoding setting matches the "
          + "source file's encoding", e);
    } catch (IOException e) {
      throw new IOExceptionWrapper(
          getFileName() + ":" + getLine() + ":" + e, e);
    }
  }

  /*
   * Reads the next byte.
   */
  private int readByte()
    throws IOException
  {
    int peek = _peek;

    if (peek >= 0) {
      _peek = -1;
      return peek;
    }

    try {
      int ch;

      // XXX: should really be handled by ReadStream
      // php/001b
      if (_encoding == null)
        ch = _is.read();
      else
        ch = _is.readChar();

      if (ch == '\r') {
        _parserLocation.incrementLineNumber();
        _hasCr = true;
      }
      else if (ch == '\n' && ! _hasCr)
        _parserLocation.incrementLineNumber();
      else
        _hasCr = false;

      return ch;
    } catch (IOException e) {
      throw new IOExceptionWrapper(
          getFileName() + ":" + getLine() + ":" + e, e);
    }
  }

  /**
   * Returns an error.
   */
  private QuercusParseException expect(String expected, int token)
  {
    return error(L.l("expected {0} at {1}", expected, tokenName(token)));
  }

  /**
   * Returns an error.
   */
  public QuercusParseException error(String msg)
  {
    int lineNumber = _parserLocation.getLineNumber();
    int lines = 5;
    int first = lines / 2;

    String []sourceLines = Env.getSourceLine(_sourceFile, 
                                             lineNumber - first + _sourceOffset,
                                             lines);

    if (sourceLines != null
        && sourceLines.length > 0) {
      StringBuilder sb = new StringBuilder();

      String shortFile = _parserLocation.getFileName();
      int p = shortFile.lastIndexOf('/');
      if (p > 0)
        shortFile = shortFile.substring(p + 1);

      sb.append(_parserLocation.toString())
        .append(msg)
        .append(" in");

      for (int i = 0; i < sourceLines.length; i++) {
        if (sourceLines[i] == null)
          continue;
        
        sb.append("\n");
        sb.append(shortFile)
          .append(":")
          .append(lineNumber - first + i)
          .append(": ")
          .append(sourceLines[i]);
      }

      return new QuercusParseException(sb.toString());
    }
    else
      return new QuercusParseException(_parserLocation.toString() + msg);
  }

  /**
   * Returns the token name.
   */
  private String tokenName(int token)
  {
    switch (token) {
    case -1:
      return "end of file";

    case '\'':
      return "'";

    case AS: return "'as'";

    case TRUE: return "true";
    case FALSE: return "false";

    case AND_RES: return "'and'";
    case OR_RES: return "'or'";
    case XOR_RES: return "'xor'";

    case C_AND: return "'&&'";
    case C_OR: return "'||'";

    case IF: return "'if'";
    case ELSE: return "'else'";
    case ELSEIF: return "'elseif'";
    case ENDIF: return "'endif'";

    case WHILE: return "'while'";
    case ENDWHILE: return "'endwhile'";
    case DO: return "'do'";

    case FOR: return "'for'";
    case ENDFOR: return "'endfor'";

    case FOREACH: return "'foreach'";
    case ENDFOREACH: return "'endforeach'";

    case SWITCH: return "'switch'";
    case ENDSWITCH: return "'endswitch'";

    case ECHO: return "'echo'";
    case PRINT: return "'print'";

    case LIST: return "'list'";
    case CASE: return "'case'";

    case DEFAULT: return "'default'";
    case CLASS: return "'class'";
    case INTERFACE: return "'interface'";
    case EXTENDS: return "'extends'";
    case IMPLEMENTS: return "'implements'";
    case RETURN: return "'return'";

    case DIE: return "'die'";
    case EXIT: return "'exit'";
    case THROW: return "'throw'";

    case CLONE: return "'clone'";
    case INSTANCEOF: return "'instanceof'";

    case SIMPLE_STRING_ESCAPE: return "string";
    case COMPLEX_STRING_ESCAPE: return "string";

    case REQUIRE: return "'require'";
    case REQUIRE_ONCE: return "'require_once'";

    case PRIVATE: return "'private'";
    case PROTECTED: return "'protected'";
    case PUBLIC: return "'public'";
    case STATIC: return "'static'";
    case FINAL: return "'final'";
    case ABSTRACT: return "'abstract'";
    case CONST: return "'const'";

    case GLOBAL: return "'global'";

    case FUNCTION: return "'function'";

    case THIS: return "'this'";

    case ARRAY_RIGHT: return "'=>'";
    case LSHIFT: return "'<<'";

    case IDENTIFIER:
      return "'" + _lexeme + "'";

    case LONG:
      return "integer (" + _lexeme + ")";

    case DOUBLE:
      return "double (" + _lexeme + ")";

    case TEXT:
      return "TEXT (token " + token + ")";

    case STRING:
      return "string(" + _lexeme + ")";

    case TEXT_ECHO:
      return "<?=";

    case SCOPE:
      return "SCOPE (" + _lexeme +  ")";
      
    case NAMESPACE:
      return "NAMESPACE";
      
    case USE:
      return "USE";

    default:
      if (32 <= token && token < 127)
        return "'" + (char) token + "'";
      else
        return "(token " + token + ")";
    }
  }

  /**
   * The location from which the last token was read.
   * @return
   */
  public Location getLocation()
  {
    return _parserLocation.getLocation();
  }

  private String pushWhileLabel()
  {
    return pushLoopLabel(createWhileLabel());
  }

  private String pushDoLabel()
  {
    return pushLoopLabel(createDoLabel());
  }

  private String pushForLabel()
  {
    return pushLoopLabel(createForLabel());
  }

  private String pushForeachLabel()
  {
    return pushLoopLabel(createForeachLabel());
  }

  private String pushSwitchLabel()
  {
    return pushLoopLabel(createSwitchLabel());
  }

  private String pushLoopLabel(String label)
  {
    _loopLabelList.add(label);

    return label;
  }

  private String popLoopLabel()
  {
    int size = _loopLabelList.size();

    if (size == 0)
      return null;
    else
      return _loopLabelList.remove(size - 1);
  }

  private String createWhileLabel()
  {
    return "while_" + _labelsCreated++;
  }

  private String createDoLabel()
  {
    return "do_" + _labelsCreated++;
  }

  private String createForLabel()
  {
    return "for_" + _labelsCreated++;
  }

  private String createForeachLabel()
  {
    return "foreach_" + _labelsCreated++;
  }

  private String createSwitchLabel()
  {
    return "switch_" + _labelsCreated++;
  }

  /*
   * Returns true if this is a switch label.
   */
  public static boolean isSwitchLabel(String label)
  {
    return label != null && label.startsWith("switch");
  }

  public void close()
  {
    ReadStream is = _is;
    _is = null;

    if (is != null)
      is.close();
  }

  private class ParserLocation {
    private int _lineNumber = 1;
    private String _fileName;
    private String _userPath;

    private String _lastClassName;
    private String _lastFunctionName;

    private Location _location;

    public int getLineNumber()
    {
      return _lineNumber;
    }

    public void setLineNumber(int lineNumber)
    {
      _lineNumber = lineNumber;
      _location = null;
    }

    public void incrementLineNumber()
    {
      _lineNumber++;
      _location = null;
    }

    public String getFileName()
    {
      return _fileName;
    }

    public void setFileName(String fileName)
    {
      _fileName = fileName;
      _userPath = fileName;

      _location = null;
    }

    public void setFileName(Path path)
    {
      // php/600a
      // need to return proper Windows paths (for joomla)
      _fileName = path.getNativePath();
      _userPath = path.getUserPath();
    }

    public String getUserPath()
    {
      return _userPath;
    }

    public Location getLocation()
    {
      String currentFunctionName
        = (_function == null || _function.isPageMain()
           ? null
           : _function.getName());

      String currentClassName = _classDef == null ? null : _classDef.getName();

      if (_location != null) {
        if (!equals(currentFunctionName, _lastFunctionName))
          _location = null;
        else if (!equals(currentClassName, _lastClassName))
          _location = null;
      }

      if (_location == null)
        _location = new Location(_fileName, _lineNumber,
            currentClassName, currentFunctionName);

      _lastFunctionName = currentFunctionName;
      _lastClassName = currentClassName;

      return _location;
    }

    private boolean equals(String s1, String s2)
    {
      return (s1 == null || s2 == null) ?  s1 == s2 : s1.equals(s2);
    }

    @Override
    public String toString()
    {
      return _fileName + ":" + _lineNumber + ": ";
    }
  }

  static {
    _insensitiveReserved.put("echo", ECHO);
    _insensitiveReserved.put("print", PRINT);
    _insensitiveReserved.put("if", IF);
    _insensitiveReserved.put("else", ELSE);
    _insensitiveReserved.put("elseif", ELSEIF);
    _insensitiveReserved.put("do", DO);
    _insensitiveReserved.put("while", WHILE);
    _insensitiveReserved.put("for", FOR);
    _insensitiveReserved.put("function", FUNCTION);
    _insensitiveReserved.put("class", CLASS);
    _insensitiveReserved.put("new", NEW);
    _insensitiveReserved.put("return", RETURN);
    _insensitiveReserved.put("break", BREAK);
    _insensitiveReserved.put("continue", CONTINUE);
    // quercus/0260
    //    _insensitiveReserved.put("var", VAR);
    _insensitiveReserved.put("this", THIS);
    _insensitiveReserved.put("private", PRIVATE);
    _insensitiveReserved.put("protected", PROTECTED);
    _insensitiveReserved.put("public", PUBLIC);
    _insensitiveReserved.put("and", AND_RES);
    _insensitiveReserved.put("xor", XOR_RES);
    _insensitiveReserved.put("or", OR_RES);
    _insensitiveReserved.put("extends", EXTENDS);
    _insensitiveReserved.put("static", STATIC);
    _insensitiveReserved.put("include", INCLUDE);
    _insensitiveReserved.put("require", REQUIRE);
    _insensitiveReserved.put("include_once", INCLUDE_ONCE);
    _insensitiveReserved.put("require_once", REQUIRE_ONCE);
    _insensitiveReserved.put("unset", UNSET);
    _insensitiveReserved.put("foreach", FOREACH);
    _insensitiveReserved.put("as", AS);
    _insensitiveReserved.put("switch", SWITCH);
    _insensitiveReserved.put("case", CASE);
    _insensitiveReserved.put("default", DEFAULT);
    _insensitiveReserved.put("die", DIE);
    _insensitiveReserved.put("exit", EXIT);
    _insensitiveReserved.put("global", GLOBAL);
    _insensitiveReserved.put("list", LIST);
    _insensitiveReserved.put("endif", ENDIF);
    _insensitiveReserved.put("endwhile", ENDWHILE);
    _insensitiveReserved.put("endfor", ENDFOR);
    _insensitiveReserved.put("endforeach", ENDFOREACH);
    _insensitiveReserved.put("endswitch", ENDSWITCH);

    _insensitiveReserved.put("true", TRUE);
    _insensitiveReserved.put("false", FALSE);
    _insensitiveReserved.put("null", NULL);
    _insensitiveReserved.put("clone", CLONE);
    _insensitiveReserved.put("instanceof", INSTANCEOF);
    _insensitiveReserved.put("const", CONST);
    _insensitiveReserved.put("final", FINAL);
    _insensitiveReserved.put("abstract", ABSTRACT);
    _insensitiveReserved.put("throw", THROW);
    _insensitiveReserved.put("try", TRY);
    _insensitiveReserved.put("catch", CATCH);
    _insensitiveReserved.put("interface", INTERFACE);
    _insensitiveReserved.put("implements", IMPLEMENTS);

    _insensitiveReserved.put("import", IMPORT);
    // backward compatibility issues
    _insensitiveReserved.put("namespace", NAMESPACE);
    _insensitiveReserved.put("use", USE);
  }
}
