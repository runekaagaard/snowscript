#!/usr/bin/env python
"PLY grammar for parsing Python"

# This only contains code for parsing files, not eval or single_line

# Written by Andrew Dalke
# Copyright (c) 2008 by Dalke Scientific, AB
# 
# See LICENSE for details.


from ply import yacc
import compiler
from compiler import ast, misc, syntax, pycodegen

import python_lex
import validate

tokens = python_lex.tokens

# I cross-compare my code with Python's compiler module, which has
# bugs and decisions I disagree with.  Enable this to make the line
# numbers and generated AST more closely match Python's.

BACKWARDS_COMPATIBLE = False

def raise_syntax_error(message, node=None, lineno=None):
    if node is not None:
        assert lineno is None, "don't use both 'node' and 'lineno'"
        lineno = node.lineno
    else:
        assert lineno is not None, "must use either 'node' or 'lineno'"
        
    # Once I have the span working I can get the byte position.
    # I don't have access to the lexdata at this point nor the
    # line_offsets so I can't generate the full error message.
    # Then again, Python doesn't either.
    raise SyntaxError(message, (None, lineno, None, None))


# I'm experimenting with tracking the bound of each term.
# I've not tested the code so I've commented it out for this release.
# To enable it, remove the 3 occurances of "#, " and all of "#)"
# And uncomment the "#node.span" line 4 lines down.
def locate(node, lineno):#, (span_start, span_end)):
    assert isinstance(lineno, int), (node, lineno)
    node.lineno = lineno
    #node.span = (span_start, span_end)

def bounds(x, y):
    if isinstance(x, tuple):
        if isinstance(y, tuple):
            return (x[0], y[1])
        else:
            return (x[0], y.span[1])
    else:
        if isinstance(y, tuple):
            return (x.span[0], y[1])
        else:
            return (x.span[0], y.span[1])

def text_bounds(p, arg1, arg2=None):
    start = p.lexpos(arg1)
    if arg2 is None:
        end = start + len(p[arg1])
    else:
        end = p.lexpos(arg2) + len(p[arg2])
    return start, end
                   

def extract_docstring(stmt):
    if not isinstance(stmt, ast.Stmt):
        raise AssertionError("not a Stmt: %r" % (stmt,))
    nodes = stmt.nodes
    if nodes:
        if isinstance(nodes[0], ast.Discard):
            expr = nodes[0].expr
            if isinstance(expr, ast.Const):
                if isinstance(expr.value, basestring):
                    new_stmt = ast.Stmt(nodes[1:])
                    locate(new_stmt, nodes[0].lineno)#, nodes[0].span)
                    return expr.value, new_stmt
    return None, stmt

# "varargslist" defines the parameters in a function definition.
# "arglist" defines the parameters in a function call.
class VarargsList(object):
    def __init__(self, argnames, defaults, flags):
        self.argnames = argnames
        self.defaults = defaults
        self.flags = flags

# XXX I don't like this.  I'm making a new Arglist each time and doing
# the check for has_keyword as an O(N**2) operation.  Though N is
# small.
class Arglist(object):
    def __init__(self, args=None, star_args=None, dstar_args=None):
        self.args = args or []
        self.star_args = star_args
        self.dstar_args = dstar_args
        has_keyword = False
        for arg in self.args:
            if isinstance(arg, ast.Keyword):
                has_keyword = True
            else:
                if has_keyword:
                    raise_syntax_error("non-keyword after keyword args",
                                       arg)

    def add_arg(self, arg):
        return Arglist(self.args + [arg], self.star_args, self.dstar_args)
    def add_star_arg(self, star_args, dstar_args):
        return Arglist(self.args, star_args, dstar_args)

####################################

def p_file_input_1(p):
    "file_input : ENDMARKER"
    # Empty file
    stmt = ast.Stmt([])
    locate(stmt, 1)#, (None, None))
    p[0] = ast.Module(None, stmt)
    locate(p[0], 1)#, (None, None))
def p_file_input_2(p):
    "file_input : file_input_star ENDMARKER"
    stmt = ast.Stmt(p[1])
    locate(stmt, p[1][0].lineno)#, bounds(p[1][0], p[1][-1]))
    docstring, stmt = extract_docstring(stmt)
    p[0] = ast.Module(docstring, stmt)
    locate(p[0], 1)#, (None, None))

# file_input: (NEWLINE | stmt)* ENDMARKER
def p_file_input_star_1(p):
    "file_input_star : NEWLINE"
    p[0] = []
    # My tokenizer does not return top-level newlines
    raise AssertionError("tokenizer should not have returned NEWLINE")
def p_file_input_star_2(p):
    "file_input_star : file_input_star NEWLINE"
    p[0] = p[1]
    raise AssertionError("tokenizer should not have returned NEWLINE")
def p_file_input_star_3(p):
    "file_input_star : stmt"
    p[0] = p[1]
def p_file_input_star_4(p):
    "file_input_star : file_input_star stmt"
    p[0] = p[1] + p[2]


def dotted_name_list_to_expr(name_list):
    # Each is a 3-tuple containing: (name, lineno, span)
    name, lineno, span = name_list[0]
    expr = ast.Name(name)
    locate(expr, lineno)#, span)
    for (name, lineno, span) in name_list[1:]:
        expr = ast.Getattr(expr, name)
        locate(expr, lineno)#, span)
    return expr

# When BACKWARDS_COMPATIBLE, ast.CallFunc gets the line number from
# the first argument in the argument list rather than the '@', which I
# think it should.  This function gets that line number.
_not_given = object()
def get_callfunc_lineno(arglist, default_lineno = _not_given):
    node = None
    if arglist.args:
        node = arglist.args[0]
    elif arglist.star_args:
        node = arglist.star_args
    elif arglist.dstar_args:
        node = arglist.dstar_args
    elif default_lineno is not _not_given:
        return default_lineno
    else:
        raise AssertionError("should not get this")
    # I have the line number for the root element in the 
    # first expression, but that isn't necessarily the first term
    # Consider:
    # f(a
    #    +b)
    # The top term is line #2 but Python uses line #1 because
    # that's where the first token comes from.
    smallest_lineno = node.lineno
    nodes = [node]
    while nodes:
        node = nodes.pop()
        if node.lineno < smallest_lineno:
            smallest_lineno = node.lineno
        nodes.extend(node.getChildNodes())
    return smallest_lineno

# decorator: '@' dotted_name [ '(' [arglist] ')' ] NEWLINE
def p_decorator_1(p):
    'decorator : AT dotted_name NEWLINE'
    p[0] = dotted_name_list_to_expr(p[2])
def p_decorator_2(p):
    'decorator : AT dotted_name LPAR RPAR NEWLINE'
    expr = dotted_name_list_to_expr(p[2])
    p[0] = ast.CallFunc(expr, [], None, None)
    if BACKWARDS_COMPATIBLE:
        # Huh?  It uses the line number from the ')'
        locate(p[0], p.lineno(4))#, text_bounds(p, 1, 4))
    else:
        # I think it should be the '@'
        locate(p[0], p.lineno(1))#, text_bounds(p, 1, 4))
def p_decorator_3(p):
    'decorator : AT dotted_name LPAR arglist RPAR NEWLINE'
    expr = dotted_name_list_to_expr(p[2])
    p[0] = ast.CallFunc(expr, p[4].args, p[4].star_args, p[4].dstar_args)
    if BACKWARDS_COMPATIBLE:
        # Huh?  It uses the line number from the first term?
        lineno = get_callfunc_lineno(p[4])
        locate(p[0], lineno)#, text_bounds(p, 1, 5))
    else:
        # I think it should be the '@'
        locate(p[0], p.lineno(1))#, text_bounds(p, 1, 5))

# decorators: decorator+
def p_decorators(p):
    'decorators : decorators_plus'
    p[0] = ast.Decorators(p[1])
    locate(p[0], p[1][0].lineno)#, bounds(p[1][0].span, p[1][-1].span))

def p_decorators_plus_1(p):
    'decorators_plus : decorator'
    p[0] = [p[1]]

def p_decorators_plus_2(p):
    'decorators_plus : decorators_plus decorator'
    p[0] = p[1] + [p[2]]


# funcdef: [decorators] 'def' NAME parameters ':' suite
def p_funcdef_1(p):
    'funcdef : DEF NAME parameters COLON suite'
    docstring, stmt = extract_docstring(p[5])
    node = ast.Function(None, p[2], p[3].argnames, p[3].defaults,
                        p[3].flags, docstring, stmt)
    lineno = p.lineno(1)
    if BACKWARDS_COMPATIBLE:
        lineno = p.lineno(2)  # XXX which is right?  I like using the "def"
    locate(node, lineno)#, bounds(text_bounds(p, 1), p[5]))
    p[0] = [node]

def p_funcdef_2(p):
    'funcdef : decorators DEF NAME parameters COLON suite'
    docstring, stmt = extract_docstring(p[6])
    node = ast.Function(p[1], p[3], p[4].argnames, p[4].defaults,
                        p[4].flags, docstring, stmt)
    # Using the def for the function definition line number
    # Should the span be the entire span, or the span for the function?
    locate(node, p.lineno(2))#, bounds(p[1].span, p[6].span))
    p[0] = [node]

# "parameters" are the () terms used in a function definition
def p_parameters_1(p):
    'parameters : LPAR RPAR'
    p[0] = VarargsList(argnames=(), defaults=(), flags=0)
    locate(p[0], p.lineno(1))#, text_bounds(p, 1, 2))

def p_parameters_2(p):
    'parameters : LPAR varargslist RPAR'
    p[0] = p[2]
    
## Making the function definition
# varargslist: ((fpdef ['=' test] ',')*
#               ('*' NAME [',' '**' NAME] | '**' NAME) |
#               fpdef ['=' test] (',' fpdef ['=' test])* [','])
def make_varargslist(args, star_args, dstar_args):
    has_value = False
    seen = set()
    argnames = []
    defaults = []
    prev_lineno = None

    # This is complicated because:
    #   def f(a,(b,c,(d,e),x),y)
    # is legal.  Easiest solution is recursive
    def check_args(lhs):
        arg_type, name, lineno = lhs
        if arg_type == "name":
            if name in seen:
                raise_syntax_error(
                  "duplicate argument %r in function definition" % (name,),
                  lineno=lineno)
                  
            seen.add(name)
            return name

        assert arg_type == "tuple"
        arg_tuple = name
        results = []
        for arg in arg_tuple:
            results.append(check_args(arg))
        return tuple(results)

    prev_lineno = None
    for (lhs, rhs) in args:
        lineno = lhs[2]
        argnames.append(check_args(lhs))
        if rhs is not None:
            has_value = True
            defaults.append(rhs)
        elif has_value:
            # non-default argument follows default argument
            # XXX use "prev_lineno" for error reporting to match compiler
            # But I think it should be the current lineno.
            raise_syntax_error(
                "non-default argument follows default argument",
                lineno=lineno)
        # This is the line number Python reports on errors
        prev_lineno = lineno
    # Should I move this logic into the VarargsList class? XXX
    flags = 0
    if star_args:
        flags |= 4 # MAGIC
        argnames.append(star_args)
    if dstar_args:
        flags |= 8 # MAGIC
        argnames.append(dstar_args)
    return VarargsList(argnames = argnames, defaults = defaults, flags=flags)

# def f(varargslist): ...
def p_varargslist_1(p):
    'varargslist : fpdef COMMA STAR NAME'
    # def f(a, *args): pass
    # def f((a, b), *args): pass
    p[0] = make_varargslist([(p[1], None)], p[4], None)
def p_varargslist_2(p):
    'varargslist : fpdef COMMA STAR NAME COMMA DOUBLESTAR NAME'
    # def f(a, *args, **kwargs): pass
    # def f((a, b), *args, **kwargs): pass
    p[0] = make_varargslist([(p[1], None)], p[4], p[7])
def p_varargslist_3(p):
    'varargslist : fpdef COMMA DOUBLESTAR NAME'
    # def f(a, **kwargs): pass
    # def f((a, b), **kwargs): pass
    p[0] = make_varargslist([(p[1], None)], None, p[4])
def p_varargslist_4(p):
    'varargslist : fpdef'
    # def f(a): pass
    # def f((a, b)): pass
    p[0] = make_varargslist([(p[1], None)], None, None)
def p_varargslist_5(p):
    'varargslist : fpdef COMMA'
    # def f(a,): pass
    # def f((a,b)): pass
    p[0] = make_varargslist([(p[1], None)], None, None)
def p_varargslist_6(p):
    'varargslist : fpdef varargslist_star COMMA STAR NAME'
    # def f((a, b), c, *args): pass
    # def f((a, b), c, d=4, *args): pass
    p[0] = make_varargslist([(p[1], None)] + p[2], p[5], None)
def p_varargslist_7(p):
    'varargslist : fpdef varargslist_star COMMA STAR NAME COMMA DOUBLESTAR NAME'
    # def f((a, b), c, *args, **kwargs): pass
    # def f((a, b), c, d=4, *args, **kwargs): pass
    p[0] = make_varargslist([(p[1], None)] + p[2], p[5], p[8])
def p_varargslist_8(p):
    'varargslist : fpdef varargslist_star COMMA DOUBLESTAR NAME'
    # def f((a, b), c, **kwargs): pass
    # def f((a, b), c, d=4, **kwargs): pass
    p[0] = make_varargslist([(p[1], None)] + p[2], None, p[5])
def p_varargslist_9(p):
    'varargslist : fpdef varargslist_star'
    # def f((a, b), c): pass
    # def f((a, b), c, d=4): pass
    p[0] = make_varargslist([(p[1], None)] + p[2], None, None)
def p_varargslist_10(p):
    'varargslist : fpdef varargslist_star COMMA'
    # def f((a, b), c,): pass
    # def f((a, b), c, d=4): pass
    p[0] = make_varargslist([(p[1], None)] + p[2], None, None)

# These take a default argument
def p_varargslist_11(p):
    'varargslist : fpdef EQUAL test COMMA STAR NAME'
    # def f(a, *args): pass
    # def f((a,b)=(1,2), *args): pass
    p[0] = make_varargslist([(p[1], p[3])], p[6], None)
def p_varargslist_12(p):
    'varargslist : fpdef EQUAL test COMMA STAR NAME COMMA DOUBLESTAR NAME'
    # def f(a=1, *args, **kwargs): pass
    # def f((a,b)=(1,2), *args, **kwargs): pass
    p[0] = make_varargslist([(p[1], p[3])], p[6], p[9])
def p_varargslist_13(p):
    'varargslist : fpdef EQUAL test COMMA DOUBLESTAR NAME'
    # def f(a=1, **kwargs): pass
    # def f((a,b)=(1,2), **kwargs): pass
    p[0] = make_varargslist([(p[1], p[3])], None, p[6])
def p_varargslist_14(p):
    'varargslist : fpdef EQUAL test'
    # def f(a=1): pass
    # def f((a,b)=(1,2)): pass
    p[0] = make_varargslist([(p[1], p[3])], None, None)
def p_varargslist_15(p):
    'varargslist : fpdef EQUAL test COMMA'
    # def f(a=1,): pass
    # def f((a,b)=(1,2),): pass
    p[0] = make_varargslist([(p[1], p[3])], None, None)

def p_varargslist_16(p):
    'varargslist : fpdef EQUAL test varargslist_star COMMA STAR NAME'
    # def f(a=1, b=2, *args): pass
    p[0] = make_varargslist([(p[1], p[3])] + p[4], p[7], None)
def p_varargslist_17(p):
    'varargslist : fpdef EQUAL test varargslist_star COMMA STAR NAME COMMA DOUBLESTAR NAME'
    # def f(a=1, b=2, *args, **kwargs)
    p[0] = make_varargslist([(p[1], p[3])] + p[4], p[7], p[10])
def p_varargslist_18(p):
    'varargslist : fpdef EQUAL test varargslist_star COMMA DOUBLESTAR NAME'
    # def f(a=1, b=2, **kw): pass
    p[0] = make_varargslist([(p[1], p[3])] + p[4], None, p[7])
def p_varargslist_19(p):
    'varargslist : fpdef EQUAL test varargslist_star'
    # def f(a, *args): pass
    # def f(a=1, *args): pass
    p[0] = make_varargslist([(p[1], p[3])] + p[4], None, None)
def p_varargslist_20(p):
    'varargslist : fpdef EQUAL test varargslist_star COMMA'
    # def f(a, *args): pass
    # def f(a=1, *args): pass
    p[0] = make_varargslist([(p[1], p[3])] + p[4], None, None)
def p_varargslist_21(p):
    'varargslist : STAR NAME'
    # def f(a, *args): pass
    # def f(a=1, *args): pass
    p[0] = make_varargslist([], p[2], None)
def p_varargslist_22(p):
    'varargslist : STAR NAME COMMA DOUBLESTAR NAME'
    # def f(a, *args): pass
    # def f(a=1, *args): pass
    p[0] = make_varargslist([], p[2], p[5])
def p_varargslist_23(p):
    'varargslist : DOUBLESTAR NAME'
    # def f(a, *args): pass
    # def f(a=1, *args): pass
    p[0] = make_varargslist([], None, p[2])

# These are the "key" and "key=value" (default value) fields in the
# function definition.  For example, in
#  def f(a, b, c)
# the "varargslist_star" is the ", b, c" portion.
# This returns a list of 2-ples:
#  [0] = the NAME (as a string) or fplist (list of NAME and fplist)
#  [1] = the default value expression, or None if there isn't one
def p_varargslist_star_1(p):
    'varargslist_star : COMMA fpdef'
    # The 2nd keyword parameter in the formal definition, with no default
    # def f(a, b):
    p[0] = [(p[2], None)]
def p_varargslist_star_2(p):
    'varargslist_star : COMMA fpdef EQUAL test'
    # The 2nd keyword parameter in the formal definition, with a default
    # def f(a, b=2):
    p[0] = [(p[2], p[4])]
def p_varargslist_star_3(p):
    'varargslist_star : varargslist_star COMMA fpdef'
    # The 3rd and onwards formal parameter definition; with no default
    p[0] = p[1] + [(p[3], None)]
def p_varargslist_star_4(p):
    'varargslist_star : varargslist_star COMMA fpdef EQUAL test'
    # The 3rd and onwards formal parameter definition; with a default
    p[0] = p[1] + [(p[3], p[5])]

# This is the parameter name, or tuple, in the function definition
# Keep track of the line number so I can use it for error reporting.
# fpdef: NAME | '(' fplist ')'
def p_fpdef_1(p):
    'fpdef : NAME'
    # def f(a, b):  - 'a' and 'b' are each an fpdef of a NAME
    p[0] = ("name", p[1], p.lineno(1))
def p_fpdef_2(p):
    'fpdef : LPAR fplist RPAR'
    # def f((a)):    - not a tuple
    # def f((a,b)):  - '(a,b)' is an fpdef
    #                   'a,b' is an fplist (returning a tuple)
    #                      which containing the NAME fpdefs 'a' and 'b'
    if p[2][0] == "tuple":
        assert len(p[2]) == 2
        p[0] = p[2] + (p.lineno(1),)
    else:
        # This handles:  f((a)) 
        p[0] = p[2]

# This is used for the "a, b" portion of
#    def f((a, b)):
# fplist: fpdef (',' fpdef)* [',']
def p_fplist_1(p):
    'fplist : fpdef'
    # def f((a)=x):
    # This is the same as f(a=x)
    p[0] = p[1]
def p_fplist_2(p):
    'fplist : fpdef COMMA'
    # def f((a,)=x):
    p[0] = "tuple", (p[1],)
def p_fplist_3(p):
    'fplist : fpdef fplist_star'
    # def f((a,b)=x):
    p[0] = "tuple", (p[1],) + p[2]
def p_fplist_4(p):
    'fplist : fpdef fplist_star COMMA'
    # def f((a,b,)=x):
    p[0] = "tuple", (p[1],) + p[2]

def p_fplist_star_1(p):
    'fplist_star : COMMA fpdef'
    p[0] = (p[2],)
  
def p_fplist_star_2(p):
    'fplist_star : fplist_star COMMA fpdef'
    p[0] = p[1] + (p[3],)


# This returns a list of statements
# stmt: simple_stmt | compound_stmt
def p_stmt(p):
    '''stmt : simple_stmt
            | compound_stmt'''
    p[0] = p[1]

# simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
def p_simple_stmt_1(p):
    'simple_stmt : small_stmt NEWLINE'
    p[0] = p[1]
def p_simple_stmt_2(p):
    'simple_stmt : small_stmt SEMI NEWLINE'
    result = p[1]
    if BACKWARDS_COMPATIBLE:
        # Grr.  The compiler module inserts a useless term
        useless_term = ast.Discard(ast.Const(None))
        locate(useless_term.expr, p.lineno(2))#, text_bounds(p, 2))
        locate(useless_term, p.lineno(2))#, text_bounds(p, 2))
        result.append(useless_term)
    p[0] = result
def p_simple_stmt_3(p):
    'simple_stmt : small_stmt simple_stmt_star NEWLINE'
    p[0] = p[1] + p[2]
def p_simple_stmt_4(p):
    'simple_stmt : small_stmt simple_stmt_star SEMI NEWLINE'
    result = p[1] + p[2]
    if BACKWARDS_COMPATIBLE:
        # Grr.  The compiler module inserts a useless term
        useless_term = ast.Discard(ast.Const(None))
        locate(useless_term.expr, p.lineno(2))#, text_bounds(p, 2))
        locate(useless_term, p.lineno(2))#, text_bounds(p, 2))
        result.append(useless_term)
    p[0] = result

# Return a list of small_stmt elements
def p_simple_stmt_star_1(p):
    'simple_stmt_star : SEMI small_stmt'
    p[0] = p[2]
def p_simple_stmt_star_2(p):
    'simple_stmt_star : simple_stmt_star SEMI small_stmt'
    p[0] = p[1] + p[3]


# small_stmt: (expr_stmt | print_stmt  | del_stmt | pass_stmt | flow_stmt |
#              import_stmt | global_stmt | exec_stmt | assert_stmt)
def p_small_stmt(p):
    """
    small_stmt : expr_stmt
    | print_stmt
    | del_stmt
    | pass_stmt
    | flow_stmt
    | import_stmt
    | global_stmt
    | exec_stmt
    | assert_stmt
    """
    p[0] = p[1]

# An expr_statment is either:
#   an expression without assignment:  a+3
#   an expression with assignment:  a = x
#   an expression with augmented assignment:  a += 3

# expr_stmt: testlist (augassign (yield_expr|testlist) |
#                      ('=' (yield_expr|testlist))*)
def p_expr_stmt_1(p):
    '''expr_stmt : testlist augassign yield_expr
                 | testlist augassign testlist'''
    # a += 3
    symbol, lineno = p[2]
    node = ast.AugAssign(p[1], symbol, p[3])
    locate(node, lineno)#, bounds(p[1], p[3]))
    p[0] = [node]

# "testlist" is an expression.  This says that any expression is also
# statement.  The conversion is to evalute the expression then discard
# the result.
def p_expr_stmt_2(p):
    '''expr_stmt : testlist'''
    # a+3
    # f()
    node = ast.Discard(p[1])
    locate(node, p[1].lineno)#, p[1].span)
    p[0] = [node]

_shorthand_name = dict.fromkeys(
    (ast.Add, ast.And, ast.Bitand, ast.Bitor, ast.Bitxor,
     ast.Div, ast.FloorDiv, ast.Invert, ast.LeftShift,
     ast.Mod, ast.Mul, ast.Not, ast.Or, ast.Power,
     ast.RightShift, ast.Sub, ast.UnaryAdd, ast.UnarySub), "operator")
_shorthand_name.update({
        ast.Const: "literal",
        ast.Dict: "literal",
        ast.Backquote: "repr",
        ast.Compare: "comparison",
        ast.CallFunc: "function call",
        ast.GenExpr: "generator expression",
        ast.IfExp: "conditional expression",
        ast.Lambda: "lambda",
        ast.ListComp: "list comprehension",
        ast.Yield: "yield expression",
        })
def expr_to_assign(term):
    if isinstance(term, ast.Name):
        x = ast.AssName(term.name, 'OP_ASSIGN')
        if term.name == "None":
            raise_syntax_error("assignment to None", term)
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Tuple):
        x = ast.AssTuple([expr_to_assign(child) for child in term])
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.List):
        x = ast.AssList([expr_to_assign(child) for child in term])
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Subscript):
        x = ast.Subscript(term.expr, 'OP_ASSIGN', term.subs)
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Getattr):
        x = ast.AssAttr(term.expr, term.attrname, 'OP_ASSIGN')
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Slice):
        x = ast.Slice(term.expr, 'OP_ASSIGN', term.lower, term.upper)
        locate(x, term.lineno)#, term.span)
        return x

    else:
        # XXX These need the right line number, and a unittest
        name = _shorthand_name.get(term.__class__, None)
        if name is not None:
            raise_syntax_error("can't assign to %s" % (name,), term)
        raise AssertionError("Unknown assign node: %r" % (term,))

def p_expr_stmt_3(p):
    '''expr_stmt : testlist expr_stmt_star'''
    # a = b,c = f()
    terms = [p[1]]
    for (term, lineno) in p[2]:
        terms.append(term)
    rhs = terms.pop()
    assign_nodes = [expr_to_assign(term) for term in terms]
    lineno = p[2][0][1]
    node = ast.Assign(assign_nodes, rhs)
    locate(node, lineno)#, bounds(p[1], rhs))
    p[0] = [node]

# In the expression
#   a = b,c = f()
# the "expr_stmt_star" terms are "= b,c" and "= f()"
# This returns a list of items which are {yield_expr, testlist}
def p_expr_stmt_star_1(p):
    '''expr_stmt_star : EQUAL yield_expr
                      | EQUAL testlist'''
    # The first "=" term in the assignment chain
    p[0] = [(p[2], p.lineno(1))]

def p_expr_stmt_star_2(p):
    '''expr_stmt_star : expr_stmt_star EQUAL yield_expr
                      | expr_stmt_star EQUAL testlist'''
    # The next "=" terms in the assignment chain
    p[0] = p[1] + [(p[3], p.lineno(2))]

# augassign: ('+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' |
#             '<<=' | '>>=' | '**=' | '//=')
def p_augassign(p):
    '''
    augassign : PLUSEQUAL
      | MINEQUAL
      | STAREQUAL
      | SLASHEQUAL
      | PERCENTEQUAL
      | AMPEREQUAL
      | VBAREQUAL
      | CIRCUMFLEXEQUAL
      | LEFTSHIFTEQUAL
      | RIGHTSHIFTEQUAL
      | DOUBLESTAREQUAL
      | DOUBLESLASHEQUAL
    '''
    p[0] = (p[1], p.lineno(1))
  
def p_print_stmt_1(p):
    'print_stmt : PRINT'
    node = ast.Printnl([], None)
    locate(node, p.lineno(1))#, text_bounds(p, 1))
    p[0] = [node]
def p_print_stmt_2(p):
    'print_stmt : PRINT test'
    node = ast.Printnl([p[2]], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = [node]
def p_print_stmt_3(p):
    'print_stmt : PRINT test COMMA'
    node = ast.Print([p[2]], None)
    locate(node, p.lineno(1))#, text_bounds(p, 1, 3))
    p[0] = [node]
def p_print_stmt_4(p):
    'print_stmt : PRINT test print_stmt_plus'
    node = ast.Printnl([p[2]] + p[3], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[3][-1]))
    p[0] = [node]
def p_print_stmt_5(p):
    'print_stmt : PRINT test print_stmt_plus COMMA'
    node = ast.Print([p[2]] + p[3], None)
    locate(node, p.lineno(1))#, text_bounds(p, 1, 4))
    p[0] = [node]
def p_print_stmt_6(p):
    'print_stmt : PRINT RIGHTSHIFT test'
    node = ast.Printnl([], p[3])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[3]))
    p[0] = [node]
def p_print_stmt_7(p):
    'print_stmt : PRINT RIGHTSHIFT test print_stmt_plus'
    node = ast.Printnl(p[4], p[3])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4][-1]))
    p[0] = [node]
def p_print_stmt_8(p):
    'print_stmt : PRINT RIGHTSHIFT test print_stmt_plus COMMA'
    node = ast.Print(p[4], p[3])
    locate(node, p.lineno(1))#, text_bounds(p, 1, 5))
    p[0] = [node]

# In the statement
#   print a, b, c, d
# these are for the ", b, c, d" portion
def p_print_stmt_plus_1(p):
    'print_stmt_plus : COMMA test'
    p[0] = [p[2]]
def p_print_stmt_plus_2(p):
    'print_stmt_plus : print_stmt_plus COMMA test'
    p[0] = p[1] + [p[3]]


# The grammar definition allows complex expressions in the del statement
#  (not legal)  del a+2, 9
#  (legal)      del a[0].b, c[::2]
# This function converts the general expression into a form for deletion,
# or raises an exception if there's a problem.
def expr_to_delete(term):
    if isinstance(term, ast.Name):
        # del a
        x = ast.AssName(term.name, 'OP_DELETE')
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Tuple):
        x = ast.AssTuple([expr_to_delete(child) for child in term.nodes])
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.List):
        x = ast.AssList([expr_to_delete(child) for child in term.nodes])
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Subscript):
        # del a[1]
        x = ast.Subscript(term.expr, 'OP_DELETE', term.subs)
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Getattr):
        x = ast.AssAttr(term.expr, term.attrname, 'OP_DELETE')
        locate(x, term.lineno)#, term.span)
        return x
    elif isinstance(term, ast.Slice):
        x = ast.Slice(term.expr, 'OP_DELETE', term.lower, term.upper)
        locate(x, term.lineno)#, term.span)
        return x
    else:
        # XXX These need the right line number, and a unittest
        name = _shorthand_name.get(term.__class__, None)
        if name is not None:
            raise_syntax_error("can't delete %s" % (name,), term)
        # I don't think it's possible to get here, but I'm not certain.
        raise NotImplementedError(term)

# del_stmt: 'del' exprlist
def p_del_stmt(p):
    'del_stmt : DEL exprlist'
    node = expr_to_delete(p[2])
    # already have the 'exprlist' terms; fix it so it include the "del"
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[0]))
    p[0] = [node]

# pass_stmt: 'pass'
def p_pass_stmt(p):
    'pass_stmt : PASS'
    node = ast.Pass()
    locate(node, p.lineno(1))#, text_bounds(p, 1))
    p[0] = [node]

# flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
def p_flow_stmt(p):
    '''
    flow_stmt : break_stmt
      | continue_stmt
      | return_stmt
      | raise_stmt
      | yield_stmt
    '''
    p[0] = p[1]

# break_stmt: 'break'
def p_break_stmt(p):
    'break_stmt : BREAK'
    node = ast.Break()
    locate(node, p.lineno(1))#, text_bounds(p, 1))
    p[0] = [node]

# continue_stmt: 'continue'
def p_continue_stmt(p):
    'continue_stmt : CONTINUE'
    node = ast.Continue()
    locate(node, p.lineno(1))#, text_bounds(p, 1))
    p[0] = [node]

# return_stmt: 'return' [testlist]
def p_return_stmt_1(p):
    'return_stmt : RETURN'
    none_obj = ast.Const(None)
    locate(none_obj, p.lineno(1))#, text_bounds(p, 1))
    node = ast.Return(none_obj)
    locate(node, p.lineno(1))#, text_bounds(p, 1))
    p[0] = [node]
def p_return_stmt_2(p):
    'return_stmt : RETURN testlist'
    node = ast.Return(p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = [node]

# yield_stmt: yield_expr
def p_yield_stmt(p):
    'yield_stmt : yield_expr'
    node = ast.Discard(p[1])
    locate(node, p[1].lineno)#, p[1].span)
    p[0] = [node]

# raise_stmt: 'raise' [test [',' test [',' test]]]
def p_raise_stmt_1(p):
    'raise_stmt : RAISE'
    node = ast.Raise(None, None, None)
    locate(node, p.lineno(1))#, text_bounds(p, 1))
    p[0] = [node]
def p_raise_stmt_2(p):
    'raise_stmt : RAISE test'
    node = ast.Raise(p[2], None, None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = [node]
def p_raise_stmt_3(p):
    'raise_stmt : RAISE test COMMA test'
    node = ast.Raise(p[2], p[4], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]
def p_raise_stmt_4(p):
    'raise_stmt : RAISE test COMMA test COMMA test'
    node = ast.Raise(p[2], p[4], p[6])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[6]))
    p[0] = [node]


# import_stmt: import_name | import_from
def p_import_stmt(p):
    '''
    import_stmt : import_name
                | import_from
    '''
    p[0] = p[1]


# import_name: 'import' dotted_as_names
# examples:
#   import x, StringIO.StringIO, xml.sax.saxutils
#   import cStringIO.StringIO as SIO, cElementTree as ET
def p_import_name(p):
    'import_name : IMPORT dotted_as_names'
    # p[2] is a list of 3-ples:  [(name1, as_name1, end_byte), ...]
    # The "as_name" can be None if there is no "as" name
    # The Import node only takes the name as as_name fields
    dotted_as_names = [(name, as_name) for (name, as_name, span) in p[2]]
    end_span = p[2][-1][2]
    node = ast.Import(dotted_as_names)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), end_span))
    p[0] = [node]

def make_dotted_name(dotted_name):
    return ".".join([name for (name, lineno, span) in dotted_name])

# import_from: ('from' ('.'* dotted_name | '.'+)
#               'import' ('*' | '(' import_as_names ')' | import_as_names))
# dotted_name is a list of (name, lineno, span) 3-tuples
# import_as_names is a 2-ple of (has_trailing_comma, [(name1, as_name1), ...])

def no_trailing_comma( (names, has_trailing_comma, comma_lineno, as_span) ):
    if has_trailing_comma:
        raise_syntax_error(
            "trailing comma not allowed without surrounding parentheses",
            lineno = comma_lineno)
    
def p_import_from_1(p):
    'import_from : FROM dotted_name IMPORT STAR'
    node = ast.From(make_dotted_name(p[2]), [("*", None)], 0)
    locate(node, p.lineno(1))#, text_bounds(p, 1, 3))
    p[0] = [node]
def p_import_from_2(p):
    'import_from : FROM dotted_name IMPORT LPAR import_as_names RPAR'
    names, has_trailing_comma, comma_lineno, as_span = p[5]
    node = ast.From(make_dotted_name(p[2]), names, 0)
    locate(node, p.lineno(1))#, text_bounds(p, 1, 6))
    p[0] = [node]
def p_import_from_3(p):
    'import_from : FROM dotted_name IMPORT import_as_names'
    no_trailing_comma(p[4])
    names, has_trailing_comma, comma_lineno, as_span = p[4]
    node = ast.From(make_dotted_name(p[2]), names, 0)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), as_span))
    p[0] = [node]
def p_import_from_4(p):
    'import_from : FROM import_from_plus dotted_name IMPORT STAR'
    # XXX this isn't allowed at all under Python 2.6?
    raise_syntax_error("'import *' not allowed with 'from .'",
                       lineno = p.lineno(1))
    # This works for 2.5 (and for the compiler module in 2.6)
    node = ast.From(make_dotted_name(p[3]), [("*", None)], p[2])
    locate(node, p.lineno(1))#, text_bounds(p, 1, 5))
    p[0] = [node]
def p_import_from_5(p):
    'import_from : FROM import_from_plus dotted_name IMPORT LPAR import_as_names RPAR'
    names, has_trailing_comma, comma_lineno, as_span = p[6]
    node = ast.From(make_dotted_name(p[3]), names, p[2])
    locate(node, p.lineno(1))#, text_bounds(p, 1, 7))
    p[0] = [node]
def p_import_from_6(p):
    'import_from : FROM import_from_plus dotted_name IMPORT import_as_names'
    no_trailing_comma(p[5])
    names, has_trailing_comma, comma_lineno, as_span = p[5]
    node = ast.From(make_dotted_name(p[3]), names, p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), as_span))
    p[0] = [node]
def p_import_from_7(p):
    'import_from : FROM import_from_plus IMPORT STAR'
    node = ast.From("", [("*", None)], p[2])
    locate(node, p.lineno(1))#, text_bounds(p, 1, 4))
    p[0] = [node]
def p_import_from_8(p):
    'import_from : FROM import_from_plus IMPORT LPAR import_as_names RPAR'
    names, has_trailing_comma, comma_lineno, as_span = p[5]
    node = ast.From("", names, p[2])
    locate(node, p.lineno(1))#, text_bounds(p, 1, 6))
    p[0] = [node]
def p_import_from_9(p):
    'import_from : FROM import_from_plus IMPORT import_as_names'
    no_trailing_comma(p[4])
    names, has_trailing_comma, comma_lineno, as_span = p[4]
    node = ast.From("", names, p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), as_span))
    p[0] = [node]

# This is the number of dots used in relative imports
#   from ...a import y
# returns 3
def p_import_from_plus_1(p):
    'import_from_plus : DOT'
    p[0] = 1
def p_import_from_plus_2(p):
    'import_from_plus : import_from_plus DOT'
    p[0] = p[1] + 1
  

# import_as_name: NAME ['as' NAME]
def p_import_as_name_1(p):
    'import_as_name : NAME'
    p[0] = (p[1], None, text_bounds(p, 1))
def p_import_as_name_2(p):
    'import_as_name : NAME AS NAME'
    p[0] = (p[1], p[3], text_bounds(p, 1, 3))

# dotted_as_name: dotted_name ['as' NAME]
# Only used by the "import" statment, through dotted_as_names
def p_dotted_as_name_1(p):
    'dotted_as_name : dotted_name'
    # import x.y  ; returns ("x.y", None, (7, 10))
    # dotted_name returns a list of 3-tuples:
    #    [(name1, lineno1, span1), (name2, lineno2, span2), ...
    #  Eg: [("x", 1, (1,2)), ("y", 1, (3,4)), ...]
    # Need to convert that into:
    #   ("x.y", 1, (1,4))
    full_name = ".".join([name for (name, lineno, span) in p[1]])
    span = bounds(p[1][0][2], p[1][-1][2])
    p[0] = (full_name, None, span)
def p_dotted_as_name_2(p):
    'dotted_as_name : dotted_name AS NAME'
    # import x.y as b  ; returns ("x.y", "b", (7, 15))
    full_name = ".".join([name for (name, lineno, span) in p[1]])
    span = bounds(p[1][0][2], p[1][-1][2])
    p[0] = (full_name, p[3], span)

# import_as_names: import_as_name (',' import_as_name)* [',']
# Terminal commas are only legal if the import_as_names is inside ().
# This is legal:
#   from x import (y,)
# This is not legal:
#   from x import y,
# Rather than handle this as the syntax level, the Python grammar
# accepts the illegal grammer and the parser rejects it.
# This return a 2-tuple of
#   [0]: True if the expession ended with a ",", else False
#   [1]: The list of names 2-tuples containing (name, as_name))
def p_import_as_names_1(p):
    'import_as_names : import_as_name'
    (name, as_name, span) = p[1]
    p[0] = ([(name, as_name)], False, None, span)
def p_import_as_names_2(p):
    'import_as_names : import_as_name COMMA'
    (name, as_name, span) = p[1]
    p[0] = ([p[1]], True, p.lineno(2), bounds(span, text_bounds(p, 2)))
def p_import_as_names_3(p):
    'import_as_names : import_as_name import_as_names_star'
    (name, as_name, left_span) = p[1]
    (names, right_span) = p[2]
    p[0] = ([(name, as_name)] + names, False, 0, bounds(left_span, right_span))
def p_import_as_names_4(p):
    'import_as_names : import_as_name import_as_names_star COMMA'
    name, as_name, left_span = p[1]
    names, right_span = p[2]
    p[0] = ([(name, as_name)] + names, True, p.lineno(3), bounds(left_span, right_span))

def p_import_as_names_star_1(p):
    'import_as_names_star : COMMA import_as_name'
    # the 2nd term after the "import"; the ", b" in
    #  from x import a, b, c, d
    name, as_name, span = p[2]
    p[0] = ([(name, as_name)], bounds(text_bounds(p, 1), span))
def p_import_as_names_star_2(p):
    'import_as_names_star : import_as_names_star COMMA import_as_name'
    # The 3rd and onwards term after the "import"; the ", c" and ", d" in
    #  from x import a, b, c, d
    names, left_span = p[1]
    name, as_name, right_span = p[3]
    p[0] = (names + [(name, as_name)], bounds(left_span, right_span))

# Only used by the import statement:
# NOT used by the "from ... import ..." statement
# Will return a list of 3-element tuples
#   [(import name, as name, byte after end), ...]
# For example,
#    import x.y.z as a, b
#    012345678901234567890
#    0         1         2
# returns
#    [("x.y.z", "a", 17), ("b", None, 20)]

# dotted_as_names: dotted_as_name (',' dotted_as_name)*
# Returns a list of 3-tuples
#   name, lineno of first word in the name, span of whole name
def p_dotted_as_names_1(p):
    'dotted_as_names : dotted_as_name'
    p[0] = [p[1]]
def p_dotted_as_names_2(p):
    'dotted_as_names : dotted_as_name dotted_as_names_star'
    p[0] = [p[1]] + p[2]

def p_dotted_as_names_star_1(p):
    'dotted_as_names_star : COMMA dotted_as_name'
    p[0] = [p[2]]
def p_dotted_as_names_star_2(p):
    'dotted_as_names_star : dotted_as_names_star COMMA dotted_as_name'
    p[0] = p[1] + [p[3]]

# dotted_name: NAME ('.' NAME)*
# Used by decorators, the import statement, and the "from/import" statement
# This returns a list of 3-element tuples containing:
#   (the name, line number, bounds for the name)
# I did it this way because the decorator uses the names differently than
# the other two.
# 
# >>> parse("from a.b import z")
# Module(None, Stmt([From('a.b', [('z', None)], 0)]))
# >>> parse("import  a.b")
# Module(None, Stmt([Import([('a.b', None)])]))
# >>> parse("@a.b\ndef f(): pass")
# Module(None, Stmt([Function(Decorators([Getattr(Name('a'), 'b')]),
#                             'f', (), (), 0, None, Stmt([Pass()]))]))
def p_dotted_name_1(p):
    "dotted_name : NAME"
    # @x()
    # import y
    # from z import a
    p[0] = [(p[1], p.lineno(1), text_bounds(p, 1))]
def p_dotted_name_2(p):
    "dotted_name : NAME dotted_name_star"
    # @x.y()
    # import a.b.c
    # from i.j.k import f
    p[0] = [(p[1], p.lineno(1), text_bounds(p, 1))] + p[2]

# the 2nd and onwards terms ("y" and "z") in
#   @x.y.z
#   import x.y.z
#   from x.y.z import a
def p_dotted_name_star_1(p):
    "dotted_name_star : DOT NAME"
    p[0] = [(p[2], p.lineno(2), text_bounds(p, 2))]
def p_dotted_name_star_2(p):
    "dotted_name_star : dotted_name_star DOT NAME"
    p[0] = p[1] + [(p[3], p.lineno(3), text_bounds(p, 3))]

# global_stmt: 'global' NAME (',' NAME)*
def p_global_stmt_1(p):
    "global_stmt : GLOBAL NAME"
    # Example: global _cached_value
    node = ast.Global([p[2]])
    locate(node, p.lineno(1))#, text_bounds(p, 1, 2))
    p[0] = [node]
def p_global_stmt_2(p):
    "global_stmt : GLOBAL NAME global_stmt_star"
    # Example: global a, b, c
    other_names, right_bounds = p[3]
    node = ast.Global([p[2]] + other_names)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), right_bounds))
    p[0] = [node]

# the ", y" in "global x, y"
# Returns a two element tuple containing:
#   [0] - list of names
#   [1] - location of right-most NAME
def p_global_stmt_star_1(p):
    'global_stmt_star : COMMA NAME'
    p[0] = ([p[2]], text_bounds(p, 2))
def p_global_stmt_star_2(p):
    'global_stmt_star : global_stmt_star COMMA NAME'
    p[0] = (p[1][0] + [p[3]], text_bounds(p, 3))

# exec_stmt: 'exec' expr ['in' test [',' test]]
def p_exec_stmt_1(p):
    'exec_stmt : EXEC expr'
    node = ast.Exec(p[2], None, None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = [node]
def p_exec_stmt_2(p):
    'exec_stmt : EXEC expr IN test'
    node = ast.Exec(p[2], p[4], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]
def p_exec_stmt_3(p):
    'exec_stmt : EXEC expr IN test COMMA test'
    node = ast.Exec(p[2], p[4], p[6])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[6]))
    p[0] = [node]

# assert_stmt: 'assert' test [',' test]
def p_assert_stmt_1(p):
    'assert_stmt : ASSERT test'
    node = ast.Assert(p[2], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = [node]
def p_assert_stmt_2(p):
    'assert_stmt : ASSERT test COMMA test'
    node = ast.Assert(p[2], p[4])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]

# compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | 
#                  with_stmt | funcdef | classdef
def p_compound_stmt(p):
    '''
    compound_stmt : if_stmt
      | while_stmt
      | for_stmt
      | try_stmt
      | with_stmt
      | funcdef
      | classdef
    '''
    p[0] = p[1]

# if_stmt: 'if' test ':' suite ('elif' test ':' suite)* ['else' ':' suite]
def p_if_stmt_1(p):
    'if_stmt : IF test COLON suite'
    node = ast.If( [(p[2], p[4]) ], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]
def p_if_stmt_2(p):
    'if_stmt : IF test COLON suite ELSE COLON suite'
    node = ast.If( [(p[2], p[4]) ], p[7])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[7]))
    p[0] = [node]
def p_if_stmt_3(p):
    'if_stmt : IF test COLON suite if_stmt_star'
    node = ast.If( [(p[2], p[4]) ] + p[5], None)
    # p[5] is list of "elif" clause
    # p[5][-1] is the last elif clause
    # p[5][-1][1] is the suite expression for the last elif clause
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[5][-1][1]))
    p[0] = [node]
def p_if_stmt_4(p):
    'if_stmt : IF test COLON suite if_stmt_star ELSE COLON suite'
    node = ast.If( [(p[2], p[4]) ] + p[5], p[8])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[8]))
    p[0] = [node]

# These are for the list of "elif" clauses in an if statement
# Returns a list of 2-tuples: (text, suite)
def p_if_stmt_star_1(p):
    'if_stmt_star : ELIF test COLON suite'
    p[0] = [ (p[2], p[4]) ]
def p_if_stmt_star_2(p):
    'if_stmt_star : if_stmt_star ELIF test COLON suite'
    p[0] = p[1] + [ (p[3], p[5]) ]

# while_stmt: 'while' test ':' suite ['else' ':' suite]
def p_while_stmt_1(p):
    'while_stmt : WHILE test COLON suite'
    node = ast.While(p[2], p[4], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]
def p_while_stmt_2(p):
    'while_stmt : WHILE test COLON suite ELSE COLON suite'
    node = ast.While(p[2], p[4], p[7])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[7]))
    p[0] = [node]

# for_stmt: 'for' exprlist 'in' testlist ':' suite ['else' ':' suite]
def p_for_stmt_1(p):
    'for_stmt : FOR exprlist IN testlist COLON suite'
    node = ast.For(expr_to_assign(p[2]), p[4], p[6], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[6]))
    p[0] = [node]
def p_for_stmt_2(p):
    'for_stmt : FOR exprlist IN testlist COLON suite ELSE COLON suite'
    node = ast.For(expr_to_assign(p[2]), p[4], p[6], p[9])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[9]))
    p[0] = [node]

# try_stmt: ('try' ':' suite
#            ((except_clause ':' suite)+
# 	    ['else' ':' suite]
# 	    ['finally' ':' suite] |
# 	   'finally' ':' suite))
def p_try_stmt_1(p):
    'try_stmt : TRY COLON suite try_stmt_plus'
    node = ast.TryExcept(p[3], p[4], None)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4][-1]))
    p[0] = [node]
def p_try_stmt_2(p):
    'try_stmt : TRY COLON suite try_stmt_plus FINALLY COLON suite'
    # This is implemented as a try/finally containing the try/except
    inner = ast.TryExcept(p[3], p[4], None)
    # XXX should this only span the inner block?
    locate(inner, p.lineno(1))#, bounds(text_bounds(p, 1), p[7]))
    node = ast.TryFinally(inner, p[7])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[7]))
    p[0] = [node]
def p_try_stmt_3(p):
    'try_stmt : TRY COLON suite try_stmt_plus ELSE COLON suite'
    node = ast.TryExcept(p[3], p[4], p[7])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[7]))
    p[0] = [node]
def p_try_stmt_4(p):
    'try_stmt : TRY COLON suite try_stmt_plus ELSE COLON suite FINALLY COLON suite'
    inner = ast.TryExcept(p[3], p[4], p[7])
    locate(inner, p.lineno(1))#, bounds(text_bounds(p, 1), p[10]))
    node = ast.TryFinally(inner, p[10])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[10]))
    p[0] = [node]
def p_try_stmt_5(p):
    'try_stmt : TRY COLON suite FINALLY COLON suite'
    node = ast.TryFinally(p[3], p[6])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[6]))
    p[0] = [node]

# Handle multiple "except" clauses.
# Returns a list of X ... is this right? XXX
def p_try_stmt_plus_1(p):
    'try_stmt_plus : except_clause COLON suite'
    p[0] = [p[1] + (p[3],)]
def p_try_stmt_plus_2(p):
    'try_stmt_plus : try_stmt_plus except_clause COLON suite'
    p[0] = p[1] + [p[2] + (p[4],)]

# The with statement allows assignments like
#    with x as y:
#    with x as a[2]:
#    with x as a.b:
#    with x as (a, b):
#    with x as (a+b):
# with_stmt: 'with' test [ with_var ] ':' suite

# There is a bug in Python2.5's compiler module.  It omitted the
# conversion of the vars argument via something like expr_to_assign.
# In backwards compatible mode I use the behaviour of whatever this
# version of Python uses.
_expr_to_with = expr_to_assign
_expr = compiler.parse("from __future__ import with_statement\n"
                       "with x as y: pass\n")
# Check if it uses the buggy version, in which case set things
# up to do nothing
if _expr.node.nodes[1].vars.__class__.__name__ == "Name":
    def _expr_to_with(x):
        return x
del _expr

def p_with_stmt_1(p):
    'with_stmt : WITH test COLON suite'
    node = ast.With(p[2], None, p[4])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]
def p_with_stmt_2(p):
    'with_stmt : WITH test with_var COLON suite'
    if BACKWARDS_COMPATIBLE:
        expr_to_with = _expr_to_with
    else:
        expr_to_with = expr_to_assign
    node = ast.With(p[2], expr_to_with(p[3]), p[5])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[5]))
    p[0] = [node]

# with_var: 'as' expr
def p_with_var(p):
    'with_var : AS expr'
    p[0] = p[2]

# except_clause: 'except' [test [('as' | ',') test]]
#     (the "AS test" is Python 2.6)
def p_except_clause_1(p):
    'except_clause : EXCEPT'
    p[0] = (None, None)
def p_except_clause_2(p):
    'except_clause : EXCEPT test'
    p[0] = (p[2], None)
def p_except_clause_3(p):
    'except_clause : EXCEPT test AS test'
    p[0] = (p[2], expr_to_assign(p[4]))
def p_except_clause_4(p):
    'except_clause : EXCEPT test COMMA test'
    p[0] = (p[2], expr_to_assign(p[4]))

# suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
def p_suite_1(p):
    'suite : simple_stmt'
    # if x: pass      # the suite is on the same line
    # while a: a-=1
    p[0] = ast.Stmt(p[1])
    locate(p[0], p[1][0].lineno)#, bounds(p[1][0], p[1][-1]))
def p_suite_2(p):
    'suite : NEWLINE INDENT suite_plus DEDENT'
    # if x:       # the suite indented
    #   pass
    p[0] = ast.Stmt(p[3])
    locate(p[0], p[3][0].lineno)#, bounds(p[3][0], p[3][-1]))

# a "stmt" is already a list (is that a problem? XXX)
# This implements stmt+
def p_suite_plus_1(p):
    'suite_plus : stmt'
    p[0] = p[1]
def p_suite_plus_2(p):
    'suite_plus : suite_plus stmt'
    p[0] = p[1] + p[2]

# This is used in the "in" clause of a "for ... in ..." expression.
# This handles a problem with XXX statements
# testlist_safe: old_test [(',' old_test)+ [',']]
def p_testlist_safe_1(p):
    'testlist_safe : old_test'
    p[0] = p[1]
# Python's Grammar doesn't allow this, but I don't know why not.
# This would allow [x for x in 1,]
#def p_testlist_safe_1b(p):
#    'testlist_safe : old_test COMMA'
#    p[0] = p[1]
def p_testlist_safe_2(p):
    'testlist_safe : old_test testlist_safe_plus'
    # the ",2" and ",3" in [x for x in 1,2,3]
    p[0] = ast.Tuple([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))
def p_testlist_safe_3(p):
    'testlist_safe : old_test testlist_safe_plus COMMA'
    # the ",2" and ",3" in [x for x in 1,2,3,]
    p[0] = ast.Tuple([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], text_bounds(p, 3)))

def p_testlist_safe_plus_1(p):
    'testlist_safe_plus : COMMA old_test'
    # The ",2" in "[1,2,3,4]"
    p[0] = [p[2]]
def p_testlist_safe_plus_2(p):
    'testlist_safe_plus : testlist_safe_plus COMMA old_test'
    # The ",3" and ",4" in "[1,2,3,4]"
    p[0] = p[1] + [p[3]]

# old_test: or_test | old_lambdef
# Used in:
#   "a" of "(x for x in y if a)"
# "old" is because this was the code before .. list comprehensions?
def p_old_test(p):
    '''old_test : or_test
                | old_lambdef'''
    p[0] = p[1]

# Use in:
#   "lambda :2" of "[x for x in lambda:2]"
# old_lambdef: 'lambda' [varargslist] ':' old_test
def p_old_lambdef_1(p):
    'old_lambdef : LAMBDA COLON old_test'
    # Example: [x for x in lambda :"BLAH"]
    p[0] = ast.Lambda((), (), 0, p[3])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[3]))
def p_old_lambdef_2(p):
    'old_lambdef : LAMBDA varargslist COLON old_test'
    # Example: [x for x in lambda x:2*x]
    p[0] = ast.Lambda(p[2].argnames, p[2].defaults, p[2].flags, p[4])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    # XXXX  BUG in compiler.parse    

# test: or_test ['if' or_test 'else' test] | lambdef
def p_test_1(p):
    'test : or_test'
    p[0] = p[1]
def p_test_2(p):
    'test : or_test IF or_test ELSE test'
    p[0] = ast.IfExp(p[3], p[1], p[5])
    locate(p[0], p.lineno(2))#, bounds(p[1], p[3]))
def p_test_3(p):
    'test : lambdef'
    p[0] = p[1]

# or_test: and_test ('or' and_test)*
def p_or_test_1(p):
    "or_test : and_test"
    p[0] = p[1]
def p_or_test_2(p):
    "or_test : and_test or_test_star"
    p[0] = ast.Or([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

def p_or_test_star_1(p):
    'or_test_star : OR and_test'
    p[0] = [p[2]]
def p_or_test_star_2(p):
    'or_test_star : or_test_star OR and_test'
    p[0] = p[1] + [p[3]]

# and_test: not_test ('and' not_test)*
def p_and_test_1(p):
    'and_test : not_test'
    p[0] = p[1]
def p_and_test_2(p):
    'and_test : not_test and_test_star'
    p[0] = ast.And([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

def p_and_test_star_1(p):
    'and_test_star : AND not_test'
    p[0] = [p[2]]
def p_and_test_star_2(p):
    'and_test_star : and_test_star AND not_test'
    p[0] = p[1] + [p[3]]

# not_test: 'not' not_test | comparison
def p_not_test_1(p):
    'not_test : NOT not_test'
    p[0] = ast.Not(p[2])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
def p_not_test_2(p):
    'not_test : comparison'
    # used when there isn't a "not"
    p[0] = p[1]


# comparison: expr (comp_op expr)*
def p_comparison_1(p):
    'comparison : expr'
    p[0] = p[1]

#  "a == b < c is d"
# Compare(Name('a'),
#         [('==', Name('b')), ('<', Name('c')), ('is', Name('d'))])
def p_comparison_2(p):
    'comparison : expr comparison_star'
    # "a<b", "1!=2", "x is not None", "animal in zoo"
    p[0] = ast.Compare(p[1], p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

def p_comparison_star_1(p):
    'comparison_star : comp_op expr'
    # The "< c" in "a < b < c < d < e"
    p[0] = [ (p[1], p[2]) ]
def p_comparison_star_2(p):
    'comparison_star : comparison_star comp_op expr'
    # The "< d" and "< e" in "a < b < c < d < e"
    p[0] = p[1] + [ (p[2], p[3]) ]

# comp_op: '<'|'>'|'=='|'>='|'<='|'!='|'in'|'not' 'in'|'is'|'is' 'not'
def p_comp_op_1(p):
    '''comp_op : LESS
        | GREATER
        | EQEQUAL
        | GREATEREQUAL
        | LESSEQUAL
        | NOTEQUAL
        | IN
        | IS
    '''
    p[0] = p[1]
def p_comp_op_2(p):
    'comp_op : NOT IN'
    p[0] = "not in"
def p_comp_op_3(p):
    'comp_op : IS NOT'
    p[0] = "is not"
    

# Helper function to combine left-to-right operators like a + b - c
def merge_chained_funcs(term, successors, func_table):
    for (op, lineno, rhs) in successors:
        lhs = term
        term = func_table[op]((lhs, rhs))
        locate(term, lineno)#, bounds(lhs, rhs))
    return term

# expr: xor_expr ('|' xor_expr)*
def p_expr_1(p):
    'expr : xor_expr'
    p[0] = p[1]
def p_expr_2(p):
    'expr : xor_expr expr_star'
    # Bitor is not a binary op; BitOr([Name('a'), Const(1), Const(4)])
    p[0] = ast.Bitor([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

def p_expr_star_1(p):
    'expr_star : VBAR xor_expr'
    p[0] = [p[2]]
def p_expr_star_2(p):
    'expr_star : expr_star VBAR xor_expr'
    p[0] = p[1] + [p[3]]

# xor_expr: and_expr ('^' and_expr)*
def p_xor_expr_1(p):
    'xor_expr : and_expr'
    p[0] = p[1]
def p_xor_expr_2(p):
    'xor_expr : and_expr xor_expr_star'
    # Bitxor is not a binary op; Bitxor([Name('a'), Const(1), Const(4)])
    p[0] = ast.Bitxor([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

def p_xor_expr_star_1(p):
    'xor_expr_star : CIRCUMFLEX and_expr'
    p[0] = [p[2]]
def p_xor_expr_star_2(p):
    'xor_expr_star : xor_expr_star CIRCUMFLEX and_expr'
    p[0] = p[1] + [p[3]]

# and_expr: shift_expr ('&' shift_expr)*
def p_and_expr_1(p):
    'and_expr : shift_expr'
    p[0] = p[1]
def p_and_expr_2(p):
    'and_expr : shift_expr and_expr_star'
    # Bitxor is not a binary op
    p[0] = ast.Bitand([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

def p_and_expr_star_1(p):
    'and_expr_star : AMPER shift_expr'
    p[0] = [p[2]]
def p_and_expr_star_2(p):
    'and_expr_star : and_expr_star AMPER shift_expr'
    p[0] = p[1] + [p[3]]
            
# shift_expr: arith_expr (('<<'|'>>') arith_expr)*
_shift_funcs = {"<<": ast.LeftShift,
                ">>": ast.RightShift}
def p_shift_expr_1(p):
    'shift_expr : arith_expr'
    p[0] = p[1]
def p_shift_expr_2(p):
    'shift_expr : arith_expr shift_expr_star'
    p[0] = merge_chained_funcs(p[1], p[2], _shift_funcs)

def p_shift_expr_star_1(p):
    '''shift_expr_star : LEFTSHIFT arith_expr
                       | RIGHTSHIFT arith_expr'''
    p[0] = [(p[1], p.lineno(1), p[2])]

def p_shift_expr_star_2(p):
    '''shift_expr_star : shift_expr_star LEFTSHIFT arith_expr
                       | shift_expr_star RIGHTSHIFT arith_expr'''
    p[0] = p[1] + [(p[2], p.lineno(2), p[3])]

# arith_expr: term (('+'|'-') term)*
_arith_funcs = {"+": ast.Add, "-": ast.Sub}
def p_arith_expr_1(p):
    'arith_expr : term'
    p[0] = p[1]
def p_arith_expr_2(p):
    'arith_expr : term arith_expr_star'
    p[0] = merge_chained_funcs(p[1], p[2], _arith_funcs)

def p_arith_expr_star_1(p):
    '''arith_expr_star : PLUS term
           | MINUS term'''
    p[0] = [(p[1], p.lineno(1), p[2])]

def p_arith_expr_star_2(p):
    '''arith_expr_star : arith_expr_star PLUS term
            | arith_expr_star MINUS term'''
    p[0] = p[1] + [(p[2], p.lineno(2), p[3])]

# term: factor (('*'|'/'|'%'|'//') factor)*
_factor_funcs = {"*": ast.Mul, "/": ast.Div,
                 "%": ast.Mod, "//": ast.FloorDiv}
def p_term_1(p):
    'term : factor'
    p[0] = p[1]
def p_term_2(p):
    'term : factor term_star'
    p[0] = merge_chained_funcs(p[1], p[2], _factor_funcs)

def p_term_star_1(p):
    '''term_star : STAR factor
                 | SLASH factor
                 | PERCENT factor
                 | DOUBLESLASH factor'''
    p[0] = [(p[1], p.lineno(1), p[2])]
def p_term_star_5(p):
    '''term_star : term_star STAR factor
                 | term_star SLASH factor
                 | term_star PERCENT factor
                 | term_star DOUBLESLASH factor'''
    p[0] = p[1] + [(p[2], p.lineno(2), p[3])]

# factor: ('+'|'-'|'~') factor | power
def p_factor_1(p):
    'factor : PLUS factor'
    p[0] = ast.UnaryAdd(p[2])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
def p_factor_2(p):
    'factor : MINUS factor'
    p[0] = ast.UnarySub(p[2])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
def p_factor_3(p):
    'factor : TILDE factor'
    p[0] = ast.Invert(p[2])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
def p_factor_4(p):
    'factor : power'
    p[0] = p[1]

# power: atom trailer* ['**' factor]
def make_power_subscript(left, right):
    if isinstance(right, SliceInfo):
        # This is the only way to generate an ast.Slice
        term = right
        return ast.Slice(left, 'OP_APPLY', term.left, term.right)
    # Otherwise it's an ast.Subscript
    subscripts = []
    for term in right:
        if isinstance(term, SliceInfo):
            term_left = term.left
            if term_left is None:
                term_left = ast.Const(None)
                locate(term_left, term.lineno)#, term.span)
            term_right = term.right
            if term_right is None:
                term_right = ast.Const(None)
                locate(term_right, term.lineno)#, term.span)
            x = ast.Sliceobj([term_left, term_right])
            locate(x, term.lineno)#, bounds(term_left, term_right))
            subscripts.append(x)
        else:
            subscripts.append(term)
    return ast.Subscript(left, 'OP_APPLY', subscripts)
     
def merge_power_functions(expr, terms):
    for (op, node, lineno, span) in terms:
        if op == "()":
            expr = ast.CallFunc(expr, node.args,
                                node.star_args, node.dstar_args)
            if BACKWARDS_COMPATIBLE:
                # Grr - uses a strange line number
                lineno = get_callfunc_lineno(node, lineno)
        elif op == ".":
            expr = ast.Getattr(expr, node)
        elif op == "[]":
            expr = make_power_subscript(expr, node)
        else:
            raise AssertionError("Unknown op: %r" % (op,))
        locate(expr, lineno)#, span)
    return expr

def p_power_1(p):
    'power : atom'
    p[0] = p[1]
def p_power_2(p):
    'power : atom DOUBLESTAR factor'
    # a ** 2
    # exponentiation is right to left, so I don't use merge_chained_funcs
    p[0] = ast.Power((p[1], p[3]))
    locate(p[0], p[1].lineno)#, bounds(p[1], p[3]))
def p_power_3(p):
    'power : atom power_star'
    # Apply the given operations to the atom
    # a.b() -> 'a'  [('.', 'b'), ('()', ([], None, None))
    p[0] = merge_power_functions(p[1], p[2])
def p_power_4(p):
    'power : atom power_star DOUBLESTAR factor'
    # a[0]**2
    p[0] = ast.Power((merge_power_functions(p[1], p[2]), p[4]))
    locate(p[0], p[1].lineno)#, bounds(p[1], p[4]))

# This is the trailer+ term.
# It's what allows a()() and x.y.z[0]
def p_power_star_1(p):
    'power_star : trailer'
    p[0] = [p[1]]
def p_power_star_2(p):
    'power_star : power_star trailer'
    p[0] = p[1] + [p[2]]

# atom: ('(' [yield_expr|testlist_gexp] ')' |
#        '[' [listmaker] ']' |
#        '{' [dictmaker] '}' |
#        '`' testlist1 '`' |
#        NAME | NUMBER | STRING+)
def p_atom_1(p):
    'atom : LPAR RPAR'
    p[0] = ast.Tuple(())
    locate(p[0], p.lineno(1))#, text_bounds(p, 1, 2))
def p_atom_2(p):
    'atom : LPAR yield_expr RPAR'
    p[0] = p[2]
    # Changes the existing values; I prefer including the ()s
    if not BACKWARDS_COMPATIBLE:
        locate(p[0], p.lineno(1))#, text_bounds(p, 1, 3))
def p_atom_3(p):
    'atom : LPAR testlist_gexp RPAR'
    p[0] = p[2]
    # Changes the existing values; I prefer including the []s
    if not BACKWARDS_COMPATIBLE:
        locate(p[0], p.lineno(1))#, text_bounds(p, 1, 3))
def p_atom_4(p):
    'atom : LSQB RSQB'
    p[0] = ast.List(())
    locate(p[0], p.lineno(1))#, text_bounds(p, 1, 2))
def p_atom_5(p):
    'atom : LSQB listmaker RSQB'
    p[0] = p[2]
    # Changes the existing values
    if not BACKWARDS_COMPATIBLE:
        locate(p[0], p.lineno(1))#, text_bounds(p, 1, 3))
def p_atom_6(p):
    'atom : LBRACE RBRACE'
    p[0] = ast.Dict(())
    locate(p[0], p.lineno(1))#, text_bounds(p, 1, 2))
def p_atom_7(p):
    'atom : LBRACE dictmaker RBRACE'
    p[0] = ast.Dict(p[2])
    if BACKWARDS_COMPATIBLE:
        # Python uses the first term in the dict assignment
        locate(p[0], p[2][0][0].lineno)#, text_bounds(p, 1, 3))
    else:
        # I prefer the "{"
        locate(p[0], p.lineno(1))#, text_bounds(p, 1, 3))

def p_atom_8(p):
    'atom : BACKQUOTE testlist1 BACKQUOTE'
    p[0] = ast.Backquote(p[2])
    locate(p[0], p.lineno(1))#, text_bounds(p, 1, 3))
def p_atom_9(p):
    'atom : NAME'
    p[0] = ast.Name(p[1])
    locate(p[0], p.lineno(1))#, text_bounds(p, 1))
def p_atom_10(p):
    'atom : NUMBER'
    value, orig_text = p[1]
    p[0] = ast.Const(value)
    locate(p[0], p.lineno(1))#, (p.lexpos(1), p.lexpos(1) + len(orig_text)))
def p_atom_11(p):
    'atom : atom_plus'
    # get the STRING (atom_plus does the string concatenation)
    s, lineno, span = p[1]
    p[0] = ast.Const(s)
    locate(p[0], lineno)#, span)

# This does the string concatenation of succesive STRING tokens
#  (for example, "pan" "cakes" -> "pancakes")
# Returns:  (the string, the line number where the string starts,
#            the span for the entire string)
#  XXX does this include the r'quotes'?
def p_atom_plus_1(p):
    'atom_plus : STRING'
    # First string
    lineno = p.lineno(1)
    if BACKWARDS_COMPATIBLE:
        # Python's compiler reports the last line of the string
        # and not the first.  Yet joins of multiple strings uses
        # the last line of the first string.  Blah.
        lineno += p[1].count("\n")
    p[0] = (p[1], lineno, text_bounds(p, 1))
def p_atom_plus_2(p):
    'atom_plus : atom_plus STRING'
    # Concatenate with successive strings
    left_s, lineno, left_span = p[1]
    total_span = bounds(left_span, text_bounds(p, 2))
    p[0] = (left_s + p[2], lineno, total_span)

# listmaker: test ( list_for | (',' test)* [','] )
def p_listmaker_1(p):
    'listmaker : test list_for'  # list comprehension
    # Example: [1 for c in s]
    if_terms, for_terms = p[2]
    assert not if_terms
    p[0] = ast.ListComp(p[1], for_terms)
    locate(p[0], p[1].lineno)#, bounds(p[1], for_terms[0]))

def p_listmaker_2(p):
    'listmaker : test'
    # First element in a list: the "1" in "[1]"
    p[0] = ast.List([p[1]])
    locate(p[0], p[1].lineno)#, p[1].span)
def p_listmaker_3(p):
    'listmaker : test COMMA'
    # First element in a list: the "1," in "[1,]"
    p[0] = ast.List([p[1]])
    locate(p[0], p[1].lineno)#, bounds(p[1], text_bounds(p, 2)))
def p_listmaker_4(p):
    'listmaker : test listmaker_star'
    # Get the 2nd and onwards terms
    p[0] = ast.List( [p[1]] + p[2] )
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))
def p_listmaker_5(p):
    'listmaker : test listmaker_star COMMA'
    # Get the 2nd and onwards terms; ends with a ","
    p[0] = ast.List( [p[1]] + p[2] )
    locate(p[0], p[1].lineno)#, bounds(p[1], text_bounds(p, 3)))

# the 2nd and onwards terms in a list
#   the ",2" and ",3" in:  [1, 2, 3]
def p_listmaker_star_1(p):
    'listmaker_star : COMMA test'
    # The 2nd term in a list, like the ", 2" in "[1, 2, 3]"
    p[0] = [p[2]]
def p_listmaker_star_2(p):
    'listmaker_star : listmaker_star COMMA test'
    # After the 2nd term in a list
    p[0] = p[1] + [p[3]]

#    "(c for c in s)"
# GenExpr(GenExprInner(Name('c'),
#         [GenExprFor(AssName('c', 'OP_ASSIGN'), Name('s'), [])]))
#
#    "(c for c in s if c)"
# GenExpr(GenExprInner(Name('c'),
#         [GenExprFor(AssName('c', 'OP_ASSIGN'), Name('s'),
#                      [GenExprIf(Name('c'))])]))

#  "(c for c in s if c for d in t if d !=1 if d != 2)")
# GenExpr(GenExprInner(Name('c'), 
#    [GenExprFor(AssName('c', 'OP_ASSIGN'), Name('s'),
#                    [GenExprIf(Name('c'))]),
#     GenExprFor(AssName('d', 'OP_ASSIGN'), Name('t'), 
#                    [GenExprIf(Compare(Name('d'), [('!=', Const(1))])),
#                     GenExprIf(Compare(Name('d'), [('!=', Const(2))]))])
#    ]
# ))



# testlist_gexp: test ( gen_for | (',' test)* [','] )
# These occur inside of an '(' expression with parenthesis ')'
# This can be (1, 2, 3)
# or a generator expresion like (1 for ...)
def p_testlist_gexp_1(p):
    'testlist_gexp : test gen_for'
    # Generator comprehension
    if_terms, gen_terms = p[2]
    gen_terms[0].is_outmost = True # XXX undocumented
    assert not if_terms
    inner = ast.GenExprInner(p[1], gen_terms)
    locate(inner, p[1].lineno)#, bounds(p[1], gen_terms[-1]))
    p[0] = ast.GenExpr(inner)
    locate(p[0], inner.lineno)#, inner.span)
def p_testlist_gexp_2(p):
    'testlist_gexp : test'
    # Called for the "a" in "x = (a)"
    p[0] = p[1]
    

def p_testlist_gexp_3(p):
    'testlist_gexp : test COMMA'
    # Called for the "a," in "x = (a,)"
    p[0] = ast.Tuple([p[1]])
    # Assign now, but they will be replaced by: LPAR testlist_gexp RPAR
    locate(p[0], p[1].lineno)#, p[1].span)
def p_testlist_gexp_4(p):
    '''testlist_gexp : test testlist_gexp_star
                     | test testlist_gexp_star COMMA'''
    # Remember, this is for terms inside the ()s
    # x = (a, b)
    # x = (a, b, c, d,)
    tests = [p[1]] + p[2]
    p[0] = ast.Tuple(tests)
    # Assign now, but they will be replaced by: LPAR testlist_gexp RPAR
    locate(p[0], tests[0].lineno)#, bounds(tests[0], tests[-1]))

# Implement (COMMA test)+
def p_testlist_gexp_star_1(p):
    'testlist_gexp_star : COMMA test'
    p[0] = [p[2]]
def p_testlist_gexp_star_2(p):
    'testlist_gexp_star : testlist_gexp_star COMMA test'
    p[0] = p[1] + [p[3]]

# lambdef: 'lambda' [varargslist] ':' test
def p_lambdef_1(p):
    'lambdef : LAMBDA COLON test'
    p[0] = ast.Lambda((), (), 0, p[3])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[3]))
def p_lambdef_2(p):
    'lambdef : LAMBDA varargslist COLON test'
    p[0] = ast.Lambda(p[2].argnames, p[2].defaults, p[2].flags, p[4])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))


# a "trailer" is the things that go after an "atom".  There can be
# many of these, and the list of these is a "power_star" (it comes
# from "trailer*" in the power definition).
# In the following
#   a()(1)[2].c
# there are four trailers:
#   () -- this is p_trailer1
#   (1) -- this is p_trailer2
#   [2] -- this is p_trailer3
#   .c -- this is p_trailer4

# trailer: '(' [arglist] ')' | '[' subscriptlist ']' | '.' NAME
def p_trailer_1(p):
    'trailer : LPAR RPAR'
    # function call with no parameters
    x = Arglist()
    p[0] = ('()', x, p.lineno(1), text_bounds(p, 1,2))
def p_trailer_2(p):
    'trailer : LPAR arglist RPAR'
    # function call, like a()
    p[0] = ('()', p[2], p.lineno(1), text_bounds(p, 1, 3))
def p_trailer_3(p):
    'trailer : LSQB subscriptlist RSQB'  # a[2]  a[i:j]  a[i:j:k]
    p[0] = ('[]', p[2], p.lineno(1), text_bounds(p, 1, 3))
def p_trailer_4(p):
    'trailer : DOT NAME'  # need to assign a lineno and span
    # attribute lookup: a.c
    p[0] = ('.', p[2], p.lineno(1), text_bounds(p, 1, 2))


# Python distinguishes between slices and subscripts.  Compare:
# >>> parse("d[:]")
# Module(None, Stmt([Discard(Slice(Name('d'), 'OP_APPLY', None, None))]))
# >>> parse("d[::]")
# Module(None, Stmt([Discard(Subscript(Name('d'), 'OP_APPLY',
#                    [Sliceobj([Const(None), Const(None), Const(None)])]))]))
# >>> parse("a[1:2,]")
# Module(None, Stmt([Discard(Subscript(Name('a'), 'OP_APPLY',
#                    [Sliceobj([Const(None), Const(None)])]))]))
#
# Ellipses always use Subscript



# subscriptlist: subscript (',' subscript)* [',']
# This returns:
#   - a SliceInfo (only in cases like a[1:2])
#   - a list of SlicInfo, ast.Sliceobj and ast.Ellipsis objects
def p_subscriptlist_1(p):
    'subscriptlist : subscript'
    # Single subscript: a[:]
    #    Slice(Name('a'), 'OP_APPLY', Const(1), Const(2))
    # This is the only path which generates a Slice, and only if there
    # is one and only in ":" in the subscript.
    # Everything else generates a Sliceobj
    if isinstance(p[1], SliceInfo):
        p[0] = p[1]
    else:
        p[0] = [p[1]]
def p_subscriptlist_2(p):
    'subscriptlist : subscript COMMA'
    # Single subscript: a[:,]
    # Subscript(Name('a'), 'OP_APPLY', [Sliceobj([Const(None), Const(None)])])
    p[0] = [p[1]]
def p_subscriptlist_3(p):
    'subscriptlist : subscript subscriptlist_star'
    # Two or more subscripts
    # a[1,2,3]
    # Subscript(Name('a'), 'OP_APPLY', [Const(1), Const(2), Const(3)])
    p[0] = [p[1]] + p[2]
def p_subscriptlist_4(p):
    'subscriptlist : subscript subscriptlist_star COMMA'
    # Two or more subscripts, ending with a comma
    # This is identical to the previous action.  I'm using another
    # action to make sure I have full test coverage.
    # a[1,2,3,]
    # Subscript(Name('a'), 'OP_APPLY', [Const(1), Const(2), Const(3)])
    p[0] = [p[1]] + p[2]


def p_subscriptlist_star_1(p):
    'subscriptlist_star : COMMA subscript'
    p[0] = [p[2]]
def p_subscriptlist_star_2(p):
    'subscriptlist_star : subscriptlist_star COMMA subscript'
    p[0] = p[1] + [p[3]]

# subscript: '.' '.' '.' | test | [test] ':' [test] [sliceop]

# This temporary 'SliceInfo' object stores information for subscripts with
# a single ':' (like 'x[1:]', 'x[:-1]' and 'x[1:-1]) until they be
# turned into an ast.Slice or ast.Sliceobj later on.

class SliceInfo(object):
    def __init__(self, left, right):
        self.left = left
        self.right = right

def p_subscript_1(p):
    'subscript : DOT DOT DOT'
    # a[...]
    p[0] = ast.Ellipsis()
    locate(p[0], p.lineno(1))#, text_bounds(p, 1, 3))

def p_subscript_2(p):
    'subscript : test'
    # a[x]
    p[0] = p[1]
def p_subscript_3(p):
    'subscript : COLON'
    # a[:]
    p[0] = SliceInfo(None, None)
    locate(p[0], p.lineno(1))#, text_bounds(p, 1))
def p_subscript_4(p):
    'subscript : COLON sliceop'
    # a[::]
    # a[::2]
    sliceop_obj, sliceop_span = p[2]
    none_obj1 = ast.Const(None)
    locate(none_obj1, p.lineno(1))#, text_bounds(p, 1)) # fake! XXX
    none_obj2 = ast.Const(None)
    locate(none_obj2, p.lineno(1))#, text_bounds(p, 1)) # fake! XXX
    p[0] = ast.Sliceobj([none_obj1, none_obj2, sliceop_obj])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), sliceop_span))
def p_subscript_5(p):
    'subscript : COLON test'
    # a[:9]
    p[0] = SliceInfo(None, p[2])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
def p_subscript_6(p):
    'subscript : COLON test sliceop'
    # a[:9:11]
    sliceop_obj, sliceop_span = p[3]
    none_obj = ast.Const(None)
    locate(none_obj, p.lineno(1))#, text_bounds(p, 1))  # fake! XXX
    p[0] = ast.Sliceobj([none_obj, p[2], sliceop_obj])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), sliceop_span))
def p_subscript_7(p):
    'subscript : test COLON'
    # a[-4:]
    p[0] = SliceInfo(p[1], None)
    locate(p[0], p[1].lineno)#, bounds(p[1], text_bounds(p, 2)))
def p_subscript_8(p):
    'subscript : test COLON sliceop'
    # a[1::]
    # a[1::2]
    sliceop_obj, sliceop_span = p[3]
    none_obj = ast.Const(None)
    # this is a fake location XXX
    locate(none_obj, p.lineno(2))#, text_bounds(p, 2))
    p[0] = ast.Sliceobj([p[1], none_obj, sliceop_obj])
    locate(p[0], p[1].lineno)#, bounds(p[1], sliceop_span))
def p_subscript_9(p):
    'subscript : test COLON test'
    # a[77:66]
    p[0] = SliceInfo(p[1], p[3])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[3]))
def p_subscript_10(p):
    'subscript : test COLON test sliceop'
    # a[62:168:3]
    # a[x:y:]
    sliceop_obj, sliceop_span = p[4]
    p[0] = ast.Sliceobj([p[1], p[3], sliceop_obj])
    locate(p[0], p[1].lineno)#, bounds(p[1], sliceop_span))
    
# sliceop: ':' [test]
# This is at and after the 2nd ':' in the []
# Example: the ":skip" in "data[start:stop:skip]"
# This returns a 2-tuple of (object, span) XX why?
def p_sliceop_1(p):
    'sliceop : COLON'
    # The ":2" in "x[::2]"
    none_obj = ast.Const(None)
    locate(none_obj, p.lineno(1))#, text_bounds(p, 1))
    p[0] = (none_obj, (0,0))#text_bounds(p, 1))
def p_sliceop_2(p):
    'sliceop : COLON test'
    # The 2nd ":" in "x[::]"
    p[0] = (p[2], (0,0))#bounds(text_bounds(p, 1), p[2]))

# exprlist: expr (',' expr)* [',']
# Used by 'for' and 'del' statements
# Interesting - this is legal Python: for x, in ( (1,), ): print x
def p_exprlist_1(p):
    'exprlist : expr'
    p[0] = p[1]
def p_exprlist_2(p):
    'exprlist : expr COMMA'
    p[0] = ast.Tuple([p[1]])
    locate(p[0], p[1].lineno)#, p[1].span)

def p_exprlist_3(p):
    '''exprlist : expr exprlist_star
                | expr exprlist_star COMMA'''
    nodes = [p[1]] + p[2]
    p[0] = ast.Tuple(nodes)
    locate(p[0], p[1].lineno)#, bounds(nodes[0], nodes[-1]))


# The 2nd and successive terms in:
#   del a, b, c
#   for a, b, c in x:
def p_exprlist_star_1(p):
    'exprlist_star : COMMA expr'
    p[0] = [p[2]]
def p_exprlist_star_2(p):
    'exprlist_star : exprlist_star COMMA expr'
    # Need at least 3 terms before hitting this one
    p[0] = p[1] + [p[3]]

# This is used pretty much everywhere
#  a,b = x    -- the "a,b" and the "x"
#  return a,  -- the "a,"
#  for a in [1,2,3]: -- the "[1,2,3]" (but not the "a"; that's an exprlist)
#  z += 2     -- both the "z" and the "2"
#  yield 1,   -- the "1,"
## grrr - there's also a "testlist1"
# testlist: test (',' test)* [',']
def p_testlist_1(p):
    'testlist : test'
    p[0] = p[1]
def p_testlist_2(p):
    'testlist : test COMMA'
    # a = 1,
    p[0] = ast.Tuple([p[1]])
    locate(p[0], p[1].lineno)#, bounds(p[1], text_bounds(p, 2)))
def p_testlist_3(p):
    'testlist : test testlist_star'  # a,b
    p[0] = ast.Tuple([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))
def p_testlist_4(p):
    'testlist : test testlist_star COMMA'  # a,b,
    p[0] = ast.Tuple([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], text_bounds(p, 3)))

# Returns a list of test elements
def p_testlist_star_1(p):
    'testlist_star : COMMA test'
    # the ",b" in "a,b"
    p[0] = [p[2]]
def p_testlist_star_2(p):
    'testlist_star : testlist_star COMMA test'
    # the ",b" and ",c" in "a,b,c"
    p[0] = p[1] + [p[3]]

# dictmaker: test ':' test (',' test ':' test)* [',']

# a "dictmaker" is anything inside of {}s, except the empty {}
# This returns a list of 2-element tuples of the form (key, value)
# For example: {1:2, 3:4} returns
#  [(Const(1), Const(2)), (Const(3), Const(4))]
def p_dictmaker_1(p):
    'dictmaker : test COLON test'
    # A single element dictionary:  "{1:2}"
    p[0] = [ (p[1], p[3]) ]
def p_dictmaker_2(p):
    'dictmaker : test COLON test COMMA'
    # A single element dictionary followed by a comma:  "{1:2},"
    # Using a seperate action to ensure coverage XXX
    p[0] = [ (p[1], p[3]) ]

def p_dictmaker_3(p):
    'dictmaker : test COLON test dictmaker_star'
    # 2 or more items inside of {}s:  "{1:2, 3:4}"
    p[0] = [ (p[1], p[3]) ] + p[4]
def p_dictmaker_4(p):
    'dictmaker : test COLON test dictmaker_star COMMA'
    # 2 or more items inside of {}s, ending with comma:  "{1:2, 3:4,}"
    p[0] = [ (p[1], p[3]) ] + p[4]

# a "dictmarker_star" returns a list of 2-element tuples of the form
#   (key, value)
def p_dictmaker_star_1(p):
    'dictmaker_star : COMMA test COLON test'
    # the 2nd term of a dictionary
    # Example, the ", 3:4" in "{1:2, 3:4, 5:6, 7:8}"
    p[0] = [ (p[2], p[4]) ]

def p_dictmaker_star_2(p):
    'dictmaker_star : dictmaker_star COMMA test COLON test'
    # the 3nd and onwards terms of a dictionary
    # Example, the ", 5:6" and ", 7:8" in "{1:2, 3:4, 5:6, 7:8}"
    p[0] = p[1] + [ (p[3], p[5]) ]

# classdef: 'class' NAME ['(' [testlist] ')'] ':' suite
def p_classdef_1(p):
    'classdef : CLASS NAME COLON suite'
    docstring, stmt = extract_docstring(p[4])
    node = ast.Class(p[2], [], docstring, stmt)
    locate(node, p.lineno(2))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = [node]
def p_classdef_2(p):
    'classdef : CLASS NAME LPAR RPAR COLON suite'
    docstring, stmt = extract_docstring(p[6])
    node = ast.Class(p[2], [], docstring, stmt)
    locate(node, p.lineno(2))#, bounds(text_bounds(p, 1), p[6]))
    p[0] = [node]
def p_classdef_3(p):
    'classdef : CLASS NAME LPAR testlist RPAR COLON suite'
    # The testlist in "class A(B): pass" is Name('A')
    # The testlist is "class A(B,): pass" is a Tuple([Name('A')])
    bases = p[4]
    if isinstance(bases, ast.Tuple):
        bases = bases.nodes
    else:
        bases = [bases]
    docstring, stmt = extract_docstring(p[7])
    node = ast.Class(p[2], bases, docstring, stmt)
    locate(node, p.lineno(2))#, bounds(text_bounds(p, 1), p[7]))
    p[0] = [node]

# Used when making a function call.
# arglist: (argument ',')* (argument [',']| '*' test [',' '**' test] | '**' test)

# This does not keep track of positions because it doesn't know where
# the start and end parenthesis are located.  However, "lineno" and
# "span" are assigned later on.

def p_arglist_1(p):
    'arglist : argument'
    p[0] = Arglist(args=[p[1]])
def p_arglist_2(p):
    'arglist : argument COMMA'
    p[0] = Arglist(args=[p[1]])
def p_arglist_3(p):
    'arglist : STAR test'
    p[0] = Arglist(star_args=p[2])
def p_arglist_4(p):
    'arglist : STAR test COMMA DOUBLESTAR test'
    p[0] = Arglist(star_args=p[2], dstar_args=p[5])
def p_arglist_5(p):
    'arglist : DOUBLESTAR test'
    p[0] = Arglist(dstar_args=p[2])
def p_arglist_6(p):
    'arglist : arglist_star argument'
    p[0] = p[1].add_arg(p[2])
def p_arglist_7(p):
    'arglist : arglist_star argument COMMA'
    p[0] = p[1].add_arg(p[2])
def p_arglist_8(p):
    'arglist : arglist_star STAR test'
    p[0] = p[1].add_star_arg(p[3], None)
def p_arglist_9(p):
    'arglist : arglist_star STAR test COMMA DOUBLESTAR test'
    p[0] = p[1].add_star_arg(p[3], p[6])
def p_arglist_10(p):
    'arglist : arglist_star DOUBLESTAR test'
    p[0] = p[1].add_star_arg(None, p[3])

def p_arglist_star_1(p):
    'arglist_star : argument COMMA'
    p[0] = Arglist(args=[p[1]])
def p_arglist_star_2(p):
    'arglist_star : arglist_star argument COMMA'
    p[0] = p[1].add_arg(p[2])

# These are arguments in a function call.  There three possibilities:
#   f(x)  -- a simple "test"
#   f((x+2)**3)  -- a more complex "test"
#   f(c for c in s)  -- a generator expression
#   f(x=2)  -- a keyword argument
#
# argument: test [gen_for] | test '=' test  # Really [keyword '='] test
def p_argument_1(p):
    'argument : test'
    p[0] = p[1]
def p_argument_2(p):
    'argument : test gen_for'  # genexp arg in a function call: f(genexp)
    if_terms, for_terms = p[2]
    assert not if_terms
    for_terms[0].is_outmost = True # XXX undocumented
    inner = ast.GenExprInner(p[1], for_terms)
    locate(inner, p[1].lineno)#, bounds(p[1], for_terms[-1]))
    p[0] = ast.GenExpr(inner)
    locate(p[0], inner.lineno)#, inner.span)
def p_argument_3(p):
    'argument : test EQUAL test'  # keyword arg: f(a=b)
    p[0] = ast.Keyword(expr_to_arg_name(p[1]), p[3])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[3]))

def expr_to_arg_name(expr):
    if isinstance(expr, ast.Name):
        if expr.name == "None":
            raise_syntax_error("assignment to None", expr)
        return expr.name
    else:
        # Need location XXX
        raise_syntax_error("keyword can't be an expression", expr)


# This is legal:
#  [(c,c2) for c in s1 if c != "n" for c2 in s2 if c2 != "a" if c2 != "e"]

# >>> parse("[c for c in s]")
# Module(None, Stmt([Discard(
#   ListComp(Name('c'), 
#     [ListCompFor(AssName('c', 'OP_ASSIGN'), Name('s'),
#                  [])
#     ])
# )]))

# >>> parse("[c for c in s if c == 1 if c == 2]")
# Module(None, Stmt([Discard(
#  ListComp(Name('c'),
#    [ListCompFor(AssName('c', 'OP_ASSIGN'), Name('s'),
#           [ListCompIf(Compare(Name('c'), [('==', Const(1))])),
#            ListCompIf(Compare(Name('c'), [('==', Const(2))]))
#           ])])
# )]))

# >>> parse("[c for c in s if c == 1 if c == 2 for d in t if d]") 
# Module(None, Stmt([Discard(
#   ListComp(Name('c'),
#      [ListCompFor(AssName('c', 'OP_ASSIGN'), Name('s'),
#               [ListCompIf(Compare(Name('c'), [('==', Const(1))])),
#                ListCompIf(Compare(Name('c'), [('==', Const(2))]))]),
#       ListCompFor(AssName('d', 'OP_ASSIGN'), Name('t'),
#               [ListCompIf(Name('d'))])
#      ])
# )]))



# list_iter: list_for | list_if
def p_list_iter(p):
    '''list_iter : list_for
                 | list_if'''
    p[0] = p[1]

# list_for: 'for' exprlist 'in' testlist_safe [list_iter]
def p_list_for_1(p):
    'list_for : FOR exprlist IN testlist_safe'
    # [x for (x,y) in points]
    node = ast.ListCompFor(expr_to_assign(p[2]), p[4], [])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = ([], [node])
def p_list_for_2(p):
    'list_for : FOR exprlist IN testlist_safe list_iter'
    # list_iter contains:
    #   - a list of ast.ListCompIf that apply to this 'for' term
    #   - a list of successive ast.ListCompFor terms
    if_terms, for_terms = p[5]
    node = ast.ListCompFor(expr_to_assign(p[2]), p[4], if_terms)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    # No terms (they became part of this new for term)
    p[0] = ([], [node] + for_terms)

# list_if: 'if' old_test [list_iter]
# Returns a 2-ple of:
#   [0] == list of ListCompIf terms
#   [1] == list of ListCompFor terms
def p_list_if_1(p):
    'list_if : IF old_test'
    # the "if x" in "[x for x in s if x]"
    node = ast.ListCompIf(p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = ([node], [])
def p_list_if_2(p):
    'list_if : IF old_test list_iter'
    # the "if x if y" in "[x for x in s if x if y]"
    # the "if x for y in z" in "[x for x in s if x for y in z]"
    node = ast.ListCompIf(p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    if_terms, for_terms = p[3]
    p[0] = ([node] + if_terms, for_terms)

# gen_iter: gen_for | gen_if
def p_gen_iter(p):
    '''gen_iter : gen_for
                | gen_if'''
    p[0] = p[1]

# gen_for: 'for' exprlist 'in' or_test [gen_iter]
def p_gen_for_1(p):
    'gen_for : FOR exprlist IN or_test'
    for_node = ast.GenExprFor(expr_to_assign(p[2]), p[4], [])
    locate(for_node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = ([], [for_node])

def p_gen_for_2(p):
    'gen_for : FOR exprlist IN or_test gen_iter'
    # gen_iter contains:
    #   - a list of ast.GenExprIf that apply to this 'for' term
    #   - a list of successive ast.GenExprFor terms
    if_terms, gen_terms = p[5]
    node = ast.GenExprFor(expr_to_assign(p[2]), p[4], if_terms)
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[4]))
    p[0] = ([], [node] + gen_terms)

# gen_if: 'if' old_test [gen_iter]

def p_gen_if_1(p):
    'gen_if : IF old_test'
    node = ast.GenExprIf(p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    p[0] = ([node], [])

def p_gen_if_2(p):
    'gen_if : IF old_test gen_iter'
    node = ast.GenExprIf(p[2])
    locate(node, p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))
    if_terms, gen_terms = p[3]
    p[0] = ([node] + if_terms, gen_terms)

# Inside backquotes: `s` and `1,2`
# testlist1: test (',' test)*
def p_testlist1_1(p):
    'testlist1 : test'
    # Simple object, like `s`
    p[0] = p[1]
def p_testlist1_2(p):
    'testlist1 : test testlist1_star'
    # comma-separated list of terms: `1,a,3`
    p[0] = ast.Tuple([p[1]] + p[2])
    locate(p[0], p[1].lineno)#, bounds(p[1], p[2][-1]))

# These match the 2nd and onwards terms in the `backquote` tuple list
def p_testlist1_star_1(p):
    'testlist1_star : COMMA test'
    p[0] = [p[2]]
def p_testlist1_star_2(p):
    'testlist1_star : testlist1_star COMMA test'
    p[0] = p[1] + [p[3]]

# encoding_decl: NAME
def p_encoding_decl(p):
    'encoding_decl : NAME'
    # This is here because Python's Grammar defines it.
    # The encoding declaration is handled before feeding text to the lexer.
    # It should never get here.
    raise NotImplementedError(
        "encoding declaration should not get to the parser")

# yield_expr: 'yield' [testlist]
def p_yield_expr_1(p):
    'yield_expr : YIELD'
    const = ast.Const(None)
    locate(const, p.lineno(1))#, text_bounds(p, 1)) # XXX wrong
    p[0] = ast.Yield(const)
    locate(p[0], p.lineno(1))#, text_bounds(p, 1))
def p_yield_expr_2(p):
    'yield_expr : YIELD testlist'
    p[0] = ast.Yield(p[2])
    locate(p[0], p.lineno(1))#, bounds(text_bounds(p, 1), p[2]))

def p_error(t):
    # The parser returns the original lex token, so I reach
    # into the lexer to raise the fully located exception
    python_lex.raise_syntax_error("invalid syntax", t)


parser = yacc.yacc()

def parse(source, filename="<string>"):
    # There is a bug in PLY 2.3; it doesn't like the empty string.
    # Bug reported and will be fixed for 2.4.
    # http://groups.google.com/group/ply-hack/msg/cbbfc50837818a29
    if not source:
        source = "\n"
    lexer = python_lex.lexer
    try:
        parse_tree = parser.parse(source, lexer=lexer)
    except SyntaxError, err:
        # Insert the missing data and reraise
        assert hasattr(err, "lineno"), "SytaxError is missing lineno"
        geek_lineno = err.lineno - 1
        start_of_line = lexer.lexer.line_offsets[geek_lineno]
        end_of_line = lexer.lexer.line_offsets[geek_lineno+1]-1
        text = source[start_of_line:end_of_line]
        err.filename = filename
        err.text = text
        raise
    misc.set_filename(filename, parse_tree)
    syntax.check(parse_tree)
    return parse_tree


if __name__ == "__main__":
    import sys
    if len(sys.argv) != 2:
        usage = "python_yacc.py: filename to parse and exec; or --self-test"
        print >>sys.stderr, usage
        print >>sys.stderr, " (you probably want to use ./compile.py)"
        sys.exit(1)
    
    filename = sys.argv[1]  # compile and exec the given file
    if filename == "--self-test":
        validate.verify_docstrings(globals())
        validate.cross_check()
    else:
        tree = parse(open(filename).read(), filename)
        gen = pycodegen.ModuleCodeGenerator(tree)
        code = gen.getCode()
        exec code

