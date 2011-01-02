from ply import yacc
import python_lex
tokens = python_lex.tokens

# file_input: (NEWLINE | stmt)* ENDMARKER
def p_file_input(p):
  '''
  file_input : ENDMARKER
    | file_input_star ENDMARKER
  '''
  p[0] = p[1:]

def p_file_input_star(p):
  '''
  file_input_star : NEWLINE
    | stmt
    | file_input_star NEWLINE
    | file_input_star stmt
  '''
  p[0] = p[1:]

# decorator: '@' dotted_name [ '(' [arglist] ')' ] NEWLINE
def p_decorator(p):
  '''
  decorator : AT dotted_name NEWLINE
    | AT dotted_name LPAR RPAR NEWLINE
    | AT dotted_name LPAR arglist RPAR NEWLINE
  '''
  p[0] = p[1:]

# decorators: decorator+
def p_decorators(p):
  'decorators : decorators_plus'
  p[0] = p[1:]

def p_decorators_plus(p):
  '''
  decorators_plus : decorator
    | decorators_plus decorator
  '''
  p[0] = p[1:]

# funcdef: [decorators] 'def' NAME parameters ':' suite
def p_funcdef(p):
  '''
  funcdef : DEF NAME parameters COLON suite
    | decorators DEF NAME parameters COLON suite
  '''
  p[0] = p[1:]

# parameters: '(' [varargslist] ')'
def p_parameters(p):
  '''
  parameters : LPAR RPAR
    | LPAR varargslist RPAR
  '''
  p[0] = p[1:]

# varargslist: ((fpdef ['=' test] ',')*
#               ('*' NAME [',' '**' NAME] | '**' NAME) |
#               fpdef ['=' test] (',' fpdef ['=' test])* [','])
def p_varargslist(p):
  '''
  varargslist : fpdef COMMA STAR NAME
    | fpdef COMMA STAR NAME COMMA DOUBLESTAR NAME
    | fpdef COMMA DOUBLESTAR NAME
    | fpdef
    | fpdef COMMA
    | fpdef varargslist_star COMMA STAR NAME
    | fpdef varargslist_star COMMA STAR NAME COMMA DOUBLESTAR NAME
    | fpdef varargslist_star COMMA DOUBLESTAR NAME
    | fpdef varargslist_star
    | fpdef varargslist_star COMMA
    | fpdef EQUAL test COMMA STAR NAME
    | fpdef EQUAL test COMMA STAR NAME COMMA DOUBLESTAR NAME
    | fpdef EQUAL test COMMA DOUBLESTAR NAME
    | fpdef EQUAL test
    | fpdef EQUAL test COMMA
    | fpdef EQUAL test varargslist_star COMMA STAR NAME
    | fpdef EQUAL test varargslist_star COMMA STAR NAME COMMA DOUBLESTAR NAME
    | fpdef EQUAL test varargslist_star COMMA DOUBLESTAR NAME
    | fpdef EQUAL test varargslist_star
    | fpdef EQUAL test varargslist_star COMMA
    | STAR NAME
    | STAR NAME COMMA DOUBLESTAR NAME
    | DOUBLESTAR NAME
  '''
  p[0] = p[1:]

def p_varargslist_star(p):
  '''
  varargslist_star : COMMA fpdef
    | COMMA fpdef EQUAL test
    | varargslist_star COMMA fpdef
    | varargslist_star COMMA fpdef EQUAL test
  '''
  p[0] = p[1:]

# fpdef: NAME | '(' fplist ')'
def p_fpdef(p):
  '''
  fpdef : NAME
    | LPAR fplist RPAR
  '''
  p[0] = p[1:]

# fplist: fpdef (',' fpdef)* [',']
def p_fplist(p):
  '''
  fplist : fpdef
    | fpdef COMMA
    | fpdef fplist_star
    | fpdef fplist_star COMMA
  '''
  p[0] = p[1:]

def p_fplist_star(p):
  '''
  fplist_star : COMMA fpdef
    | fplist_star COMMA fpdef
  '''
  p[0] = p[1:]

# stmt: simple_stmt | compound_stmt
def p_stmt(p):
  '''
  stmt : simple_stmt
    | compound_stmt
  '''
  p[0] = p[1:]

# simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
def p_simple_stmt(p):
  '''
  simple_stmt : small_stmt NEWLINE
    | small_stmt SEMI NEWLINE
    | small_stmt simple_stmt_star NEWLINE
    | small_stmt simple_stmt_star SEMI NEWLINE
  '''
  p[0] = p[1:]

def p_simple_stmt_star(p):
  '''
  simple_stmt_star : SEMI small_stmt
    | simple_stmt_star SEMI small_stmt
  '''
  p[0] = p[1:]

# small_stmt: (expr_stmt | print_stmt  | del_stmt | pass_stmt | flow_stmt |
#              import_stmt | global_stmt | exec_stmt | assert_stmt)
def p_small_stmt(p):
  '''
  small_stmt : expr_stmt
    | print_stmt
    | del_stmt
    | pass_stmt
    | flow_stmt
    | import_stmt
    | global_stmt
    | exec_stmt
    | assert_stmt
  '''
  p[0] = p[1:]

# expr_stmt: testlist (augassign (yield_expr|testlist) |
#                      ('=' (yield_expr|testlist))*)
def p_expr_stmt(p):
  '''
  expr_stmt : testlist augassign yield_expr
    | testlist augassign testlist
    | testlist
    | testlist expr_stmt_star
  '''
  p[0] = p[1:]

def p_expr_stmt_star(p):
  '''
  expr_stmt_star : EQUAL yield_expr
    | EQUAL testlist
    | expr_stmt_star EQUAL yield_expr
    | expr_stmt_star EQUAL testlist
  '''
  p[0] = p[1:]

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
  p[0] = p[1:]

# print_stmt: 'print' ( [ test (',' test)* [','] ] |
#                       '>>' test [ (',' test)+ [','] ] )
def p_print_stmt(p):
  '''
  print_stmt : PRINT
    | PRINT test
    | PRINT test COMMA
    | PRINT test print_stmt_plus
    | PRINT test print_stmt_plus COMMA
    | PRINT RIGHTSHIFT test
    | PRINT RIGHTSHIFT test print_stmt_plus
    | PRINT RIGHTSHIFT test print_stmt_plus COMMA
  '''
  p[0] = p[1:]

def p_print_stmt_plus(p):
  '''
  print_stmt_plus : COMMA test
    | print_stmt_plus COMMA test
  '''
  p[0] = p[1:]

# del_stmt: 'del' exprlist
def p_del_stmt(p):
  'del_stmt : DEL exprlist'
  p[0] = p[1:]

# pass_stmt: 'pass'
def p_pass_stmt(p):
  'pass_stmt : PASS'
  p[0] = p[1:]

# flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
def p_flow_stmt(p):
  '''
  flow_stmt : break_stmt
    | continue_stmt
    | return_stmt
    | raise_stmt
    | yield_stmt
  '''
  p[0] = p[1:]

# break_stmt: 'break'
def p_break_stmt(p):
  'break_stmt : BREAK'
  p[0] = p[1:]

# continue_stmt: 'continue'
def p_continue_stmt(p):
  'continue_stmt : CONTINUE'
  p[0] = p[1:]

# return_stmt: 'return' [testlist]
def p_return_stmt(p):
  '''
  return_stmt : RETURN
    | RETURN testlist
  '''
  p[0] = p[1:]

# yield_stmt: yield_expr
def p_yield_stmt(p):
  'yield_stmt : yield_expr'
  p[0] = p[1:]

# raise_stmt: 'raise' [test [',' test [',' test]]]
def p_raise_stmt(p):
  '''
  raise_stmt : RAISE
    | RAISE test
    | RAISE test COMMA test
    | RAISE test COMMA test COMMA test
  '''
  p[0] = p[1:]

# import_stmt: import_name | import_from
def p_import_stmt(p):
  '''
  import_stmt : import_name
    | import_from
  '''
  p[0] = p[1:]

# import_name: 'import' dotted_as_names
def p_import_name(p):
  'import_name : IMPORT dotted_as_names'
  p[0] = p[1:]

# import_from: ('from' ('.'* dotted_name | '.'+)
#               'import' ('*' | '(' import_as_names ')' | import_as_names))
def p_import_from(p):
  '''
  import_from : FROM dotted_name IMPORT STAR
    | FROM dotted_name IMPORT LPAR import_as_names RPAR
    | FROM dotted_name IMPORT import_as_names
    | FROM import_from_plus dotted_name IMPORT STAR
    | FROM import_from_plus dotted_name IMPORT LPAR import_as_names RPAR
    | FROM import_from_plus dotted_name IMPORT import_as_names
    | FROM import_from_plus IMPORT STAR
    | FROM import_from_plus IMPORT LPAR import_as_names RPAR
    | FROM import_from_plus IMPORT import_as_names
  '''
  p[0] = p[1:]

def p_import_from_plus(p):
  '''
  import_from_plus : DOT
    | import_from_plus DOT
  '''
  p[0] = p[1:]

# import_as_name: NAME ['as' NAME]
def p_import_as_name(p):
  '''
  import_as_name : NAME
    | NAME AS NAME
  '''
  p[0] = p[1:]

# dotted_as_name: dotted_name ['as' NAME]
def p_dotted_as_name(p):
  '''
  dotted_as_name : dotted_name
    | dotted_name AS NAME
  '''
  p[0] = p[1:]

# import_as_names: import_as_name (',' import_as_name)* [',']
def p_import_as_names(p):
  '''
  import_as_names : import_as_name
    | import_as_name COMMA
    | import_as_name import_as_names_star
    | import_as_name import_as_names_star COMMA
  '''
  p[0] = p[1:]

def p_import_as_names_star(p):
  '''
  import_as_names_star : COMMA import_as_name
    | import_as_names_star COMMA import_as_name
  '''
  p[0] = p[1:]

# dotted_as_names: dotted_as_name (',' dotted_as_name)*
def p_dotted_as_names(p):
  '''
  dotted_as_names : dotted_as_name
    | dotted_as_name dotted_as_names_star
  '''
  p[0] = p[1:]

def p_dotted_as_names_star(p):
  '''
  dotted_as_names_star : COMMA dotted_as_name
    | dotted_as_names_star COMMA dotted_as_name
  '''
  p[0] = p[1:]

# dotted_name: NAME ('.' NAME)*
def p_dotted_name(p):
  '''
  dotted_name : NAME
    | NAME dotted_name_star
  '''
  p[0] = p[1:]

def p_dotted_name_star(p):
  '''
  dotted_name_star : DOT NAME
    | dotted_name_star DOT NAME
  '''
  p[0] = p[1:]

# global_stmt: 'global' NAME (',' NAME)*
def p_global_stmt(p):
  '''
  global_stmt : GLOBAL NAME
    | GLOBAL NAME global_stmt_star
  '''
  p[0] = p[1:]

def p_global_stmt_star(p):
  '''
  global_stmt_star : COMMA NAME
    | global_stmt_star COMMA NAME
  '''
  p[0] = p[1:]

# exec_stmt: 'exec' expr ['in' test [',' test]]
def p_exec_stmt(p):
  '''
  exec_stmt : EXEC expr
    | EXEC expr IN test
    | EXEC expr IN test COMMA test
  '''
  p[0] = p[1:]

# assert_stmt: 'assert' test [',' test]
def p_assert_stmt(p):
  '''
  assert_stmt : ASSERT test
    | ASSERT test COMMA test
  '''
  p[0] = p[1:]

# compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | with_stmt | funcdef | classdef
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
  p[0] = p[1:]

# if_stmt: 'if' test ':' suite ('elif' test ':' suite)* ['else' ':' suite]
def p_if_stmt(p):
  '''
  if_stmt : IF test COLON suite
    | IF test COLON suite ELSE COLON suite
    | IF test COLON suite if_stmt_star
    | IF test COLON suite if_stmt_star ELSE COLON suite
  '''
  p[0] = p[1:]

def p_if_stmt_star(p):
  '''
  if_stmt_star : ELIF test COLON suite
    | if_stmt_star ELIF test COLON suite
  '''
  p[0] = p[1:]

# while_stmt: 'while' test ':' suite ['else' ':' suite]
def p_while_stmt(p):
  '''
  while_stmt : WHILE test COLON suite
    | WHILE test COLON suite ELSE COLON suite
  '''
  p[0] = p[1:]

# for_stmt: 'for' exprlist 'in' testlist ':' suite ['else' ':' suite]
def p_for_stmt(p):
  '''
  for_stmt : FOR exprlist IN testlist COLON suite
    | FOR exprlist IN testlist COLON suite ELSE COLON suite
  '''
  p[0] = p[1:]

# try_stmt: ('try' ':' suite
#            ((except_clause ':' suite)+
# 	    ['else' ':' suite]
# 	    ['finally' ':' suite] |
# 	   'finally' ':' suite))
def p_try_stmt(p):
  '''
  try_stmt : TRY COLON suite try_stmt_plus
    | TRY COLON suite try_stmt_plus FINALLY COLON suite
    | TRY COLON suite try_stmt_plus ELSE COLON suite
    | TRY COLON suite try_stmt_plus ELSE COLON suite FINALLY COLON suite
    | TRY COLON suite FINALLY COLON suite
  '''
  p[0] = p[1:]

def p_try_stmt_plus(p):
  '''
  try_stmt_plus : except_clause COLON suite
    | try_stmt_plus except_clause COLON suite
  '''
  p[0] = p[1:]

# with_stmt: 'with' test [ with_var ] ':' suite
def p_with_stmt(p):
  '''
  with_stmt : WITH test COLON suite
    | WITH test with_var COLON suite
  '''
  p[0] = p[1:]

# with_var: 'as' expr
def p_with_var(p):
  'with_var : AS expr'
  p[0] = p[1:]

# except_clause: 'except' [test [('as' | ',') test]]
def p_except_clause(p):
  '''
  except_clause : EXCEPT
    | EXCEPT test
    | EXCEPT test AS test
    | EXCEPT test COMMA test
  '''
  p[0] = p[1:]

# suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
def p_suite(p):
  '''
  suite : simple_stmt
    | NEWLINE INDENT suite_plus DEDENT
  '''
  p[0] = p[1:]

def p_suite_plus(p):
  '''
  suite_plus : stmt
    | suite_plus stmt
  '''
  p[0] = p[1:]

# testlist_safe: old_test [(',' old_test)+ [',']]
def p_testlist_safe(p):
  '''
  testlist_safe : old_test
    | old_test testlist_safe_plus
    | old_test testlist_safe_plus COMMA
  '''
  p[0] = p[1:]

def p_testlist_safe_plus(p):
  '''
  testlist_safe_plus : COMMA old_test
    | testlist_safe_plus COMMA old_test
  '''
  p[0] = p[1:]

# old_test: or_test | old_lambdef
def p_old_test(p):
  '''
  old_test : or_test
    | old_lambdef
  '''
  p[0] = p[1:]

# old_lambdef: 'lambda' [varargslist] ':' old_test
def p_old_lambdef(p):
  '''
  old_lambdef : LAMBDA COLON old_test
    | LAMBDA varargslist COLON old_test
  '''
  p[0] = p[1:]

# test: or_test ['if' or_test 'else' test] | lambdef
def p_test(p):
  '''
  test : or_test
    | or_test IF or_test ELSE test
    | lambdef
  '''
  p[0] = p[1:]

# or_test: and_test ('or' and_test)*
def p_or_test(p):
  '''
  or_test : and_test
    | and_test or_test_star
  '''
  p[0] = p[1:]

def p_or_test_star(p):
  '''
  or_test_star : OR and_test
    | or_test_star OR and_test
  '''
  p[0] = p[1:]

# and_test: not_test ('and' not_test)*
def p_and_test(p):
  '''
  and_test : not_test
    | not_test and_test_star
  '''
  p[0] = p[1:]

def p_and_test_star(p):
  '''
  and_test_star : AND not_test
    | and_test_star AND not_test
  '''
  p[0] = p[1:]

# not_test: 'not' not_test | comparison
def p_not_test(p):
  '''
  not_test : NOT not_test
    | comparison
  '''
  p[0] = p[1:]

# comparison: expr (comp_op expr)*
def p_comparison(p):
  '''
  comparison : expr
    | expr comparison_star
  '''
  p[0] = p[1:]

def p_comparison_star(p):
  '''
  comparison_star : comp_op expr
    | comparison_star comp_op expr
  '''
  p[0] = p[1:]

# comp_op: '<'|'>'|'=='|'>='|'<='|'!='|'in'|'not' 'in'|'is'|'is' 'not'
def p_comp_op(p):
  '''
  comp_op : LESS
    | GREATER
    | EQEQUAL
    | GREATEREQUAL
    | LESSEQUAL
    | NOTEQUAL
    | IN
    | NOT IN
    | IS
    | IS NOT
  '''
  p[0] = p[1:]

# expr: xor_expr ('|' xor_expr)*
def p_expr(p):
  '''
  expr : xor_expr
    | xor_expr expr_star
  '''
  p[0] = p[1:]

def p_expr_star(p):
  '''
  expr_star : VBAR xor_expr
    | expr_star VBAR xor_expr
  '''
  p[0] = p[1:]

# xor_expr: and_expr ('^' and_expr)*
def p_xor_expr(p):
  '''
  xor_expr : and_expr
    | and_expr xor_expr_star
  '''
  p[0] = p[1:]

def p_xor_expr_star(p):
  '''
  xor_expr_star : CIRCUMFLEX and_expr
    | xor_expr_star CIRCUMFLEX and_expr
  '''
  p[0] = p[1:]

# and_expr: shift_expr ('&' shift_expr)*
def p_and_expr(p):
  '''
  and_expr : shift_expr
    | shift_expr and_expr_star
  '''
  p[0] = p[1:]

def p_and_expr_star(p):
  '''
  and_expr_star : AMPER shift_expr
    | and_expr_star AMPER shift_expr
  '''
  p[0] = p[1:]

# shift_expr: arith_expr (('<<'|'>>') arith_expr)*
def p_shift_expr(p):
  '''
  shift_expr : arith_expr
    | arith_expr shift_expr_star
  '''
  p[0] = p[1:]

def p_shift_expr_star(p):
  '''
  shift_expr_star : LEFTSHIFT arith_expr
    | RIGHTSHIFT arith_expr
    | shift_expr_star LEFTSHIFT arith_expr
    | shift_expr_star RIGHTSHIFT arith_expr
  '''
  p[0] = p[1:]

# arith_expr: term (('+'|'-') term)*
def p_arith_expr(p):
  '''
  arith_expr : term
    | term arith_expr_star
  '''
  p[0] = p[1:]

def p_arith_expr_star(p):
  '''
  arith_expr_star : PLUS term
    | MINUS term
    | arith_expr_star PLUS term
    | arith_expr_star MINUS term
  '''
  p[0] = p[1:]

# term: factor (('*'|'/'|'%'|'//') factor)*
def p_term(p):
  '''
  term : factor
    | factor term_star
  '''
  p[0] = p[1:]

def p_term_star(p):
  '''
  term_star : STAR factor
    | SLASH factor
    | PERCENT factor
    | DOUBLESLASH factor
    | term_star STAR factor
    | term_star SLASH factor
    | term_star PERCENT factor
    | term_star DOUBLESLASH factor
  '''
  p[0] = p[1:]

# factor: ('+'|'-'|'~') factor | power
def p_factor(p):
  '''
  factor : PLUS factor
    | MINUS factor
    | TILDE factor
    | power
  '''
  p[0] = p[1:]

# power: atom trailer* ['**' factor]
def p_power(p):
  '''
  power : atom
    | atom DOUBLESTAR factor
    | atom power_star
    | atom power_star DOUBLESTAR factor
  '''
  p[0] = p[1:]

def p_power_star(p):
  '''
  power_star : trailer
    | power_star trailer
  '''
  p[0] = p[1:]

# atom: ('(' [yield_expr|testlist_gexp] ')' |
#        '[' [listmaker] ']' |
#        '{' [dictmaker] '}' |
#        '`' testlist1 '`' |
#        NAME | NUMBER | STRING+)
def p_atom(p):
  '''
  atom : LPAR RPAR
    | LPAR yield_expr RPAR
    | LPAR testlist_gexp RPAR
    | LSQB RSQB
    | LSQB listmaker RSQB
    | LBRACE RBRACE
    | LBRACE dictmaker RBRACE
    | BACKQUOTE testlist1 BACKQUOTE
    | NAME
    | NUMBER
    | atom_plus
  '''
  p[0] = p[1:]

def p_atom_plus(p):
  '''
  atom_plus : STRING
    | atom_plus STRING
  '''
  p[0] = p[1:]

# listmaker: test ( list_for | (',' test)* [','] )
def p_listmaker(p):
  '''
  listmaker : test list_for
    | test
    | test COMMA
    | test listmaker_star
    | test listmaker_star COMMA
  '''
  p[0] = p[1:]

def p_listmaker_star(p):
  '''
  listmaker_star : COMMA test
    | listmaker_star COMMA test
  '''
  p[0] = p[1:]

# testlist_gexp: test ( gen_for | (',' test)* [','] )
def p_testlist_gexp(p):
  '''
  testlist_gexp : test gen_for
    | test
    | test COMMA
    | test testlist_gexp_star
    | test testlist_gexp_star COMMA
  '''
  p[0] = p[1:]

def p_testlist_gexp_star(p):
  '''
  testlist_gexp_star : COMMA test
    | testlist_gexp_star COMMA test
  '''
  p[0] = p[1:]

# lambdef: 'lambda' [varargslist] ':' test
def p_lambdef(p):
  '''
  lambdef : LAMBDA COLON test
    | LAMBDA varargslist COLON test
  '''
  p[0] = p[1:]

# trailer: '(' [arglist] ')' | '[' subscriptlist ']' | '.' NAME
def p_trailer(p):
  '''
  trailer : LPAR RPAR
    | LPAR arglist RPAR
    | LSQB subscriptlist RSQB
    | DOT NAME
  '''
  p[0] = p[1:]

# subscriptlist: subscript (',' subscript)* [',']
def p_subscriptlist(p):
  '''
  subscriptlist : subscript
    | subscript COMMA
    | subscript subscriptlist_star
    | subscript subscriptlist_star COMMA
  '''
  p[0] = p[1:]

def p_subscriptlist_star(p):
  '''
  subscriptlist_star : COMMA subscript
    | subscriptlist_star COMMA subscript
  '''
  p[0] = p[1:]

# subscript: '.' '.' '.' | test | [test] ':' [test] [sliceop]
def p_subscript(p):
  '''
  subscript : DOT DOT DOT
    | test
    | COLON
    | COLON sliceop
    | COLON test
    | COLON test sliceop
    | test COLON
    | test COLON sliceop
    | test COLON test
    | test COLON test sliceop
  '''
  p[0] = p[1:]

# sliceop: ':' [test]
def p_sliceop(p):
  '''
  sliceop : COLON
    | COLON test
  '''
  p[0] = p[1:]

# exprlist: expr (',' expr)* [',']
def p_exprlist(p):
  '''
  exprlist : expr
    | expr COMMA
    | expr exprlist_star
    | expr exprlist_star COMMA
  '''
  p[0] = p[1:]

def p_exprlist_star(p):
  '''
  exprlist_star : COMMA expr
    | exprlist_star COMMA expr
  '''
  p[0] = p[1:]

# testlist: test (',' test)* [',']
def p_testlist(p):
  '''
  testlist : test
    | test COMMA
    | test testlist_star
    | test testlist_star COMMA
  '''
  p[0] = p[1:]

def p_testlist_star(p):
  '''
  testlist_star : COMMA test
    | testlist_star COMMA test
  '''
  p[0] = p[1:]

# dictmaker: test ':' test (',' test ':' test)* [',']
def p_dictmaker(p):
  '''
  dictmaker : test COLON test
    | test COLON test COMMA
    | test COLON test dictmaker_star
    | test COLON test dictmaker_star COMMA
  '''
  p[0] = p[1:]

def p_dictmaker_star(p):
  '''
  dictmaker_star : COMMA test COLON test
    | dictmaker_star COMMA test COLON test
  '''
  p[0] = p[1:]

# classdef: 'class' NAME ['(' [testlist] ')'] ':' suite
def p_classdef(p):
  '''
  classdef : CLASS NAME COLON suite
    | CLASS NAME LPAR RPAR COLON suite
    | CLASS NAME LPAR testlist RPAR COLON suite
  '''
  p[0] = p[1:]

# arglist: (argument ',')* (argument [',']| '*' test [',' '**' test] | '**' test)
def p_arglist(p):
  '''
  arglist : argument
    | argument COMMA
    | STAR test
    | STAR test COMMA DOUBLESTAR test
    | DOUBLESTAR test
    | arglist_star argument
    | arglist_star argument COMMA
    | arglist_star STAR test
    | arglist_star STAR test COMMA DOUBLESTAR test
    | arglist_star DOUBLESTAR test
  '''
  p[0] = p[1:]

def p_arglist_star(p):
  '''
  arglist_star : argument COMMA
    | arglist_star argument COMMA
  '''
  p[0] = p[1:]

# argument: test [gen_for] | test '=' test  # Really [keyword '='] test
def p_argument(p):
  '''
  argument : test
    | test gen_for
    | test EQUAL test
  '''
  p[0] = p[1:]

# list_iter: list_for | list_if
def p_list_iter(p):
  '''
  list_iter : list_for
    | list_if
  '''
  p[0] = p[1:]

# list_for: 'for' exprlist 'in' testlist_safe [list_iter]
def p_list_for(p):
  '''
  list_for : FOR exprlist IN testlist_safe
    | FOR exprlist IN testlist_safe list_iter
  '''
  p[0] = p[1:]

# list_if: 'if' old_test [list_iter]
def p_list_if(p):
  '''
  list_if : IF old_test
    | IF old_test list_iter
  '''
  p[0] = p[1:]

# gen_iter: gen_for | gen_if
def p_gen_iter(p):
  '''
  gen_iter : gen_for
    | gen_if
  '''
  p[0] = p[1:]

# gen_for: 'for' exprlist 'in' or_test [gen_iter]
def p_gen_for(p):
  '''
  gen_for : FOR exprlist IN or_test
    | FOR exprlist IN or_test gen_iter
  '''
  p[0] = p[1:]

# gen_if: 'if' old_test [gen_iter]
def p_gen_if(p):
  '''
  gen_if : IF old_test
    | IF old_test gen_iter
  '''
  p[0] = p[1:]

# testlist1: test (',' test)*
def p_testlist1(p):
  '''
  testlist1 : test
    | test testlist1_star
  '''
  p[0] = p[1:]

def p_testlist1_star(p):
  '''
  testlist1_star : COMMA test
    | testlist1_star COMMA test
  '''
  p[0] = p[1:]

# encoding_decl: NAME
def p_encoding_decl(p):
  'encoding_decl : NAME'
  p[0] = p[1:]

# yield_expr: 'yield' [testlist]
def p_yield_expr(p):
  '''
  yield_expr : YIELD
    | YIELD testlist
  '''
  p[0] = p[1:]
def p_error(p):
  raise AssertionError(p)

if __name__ == "__main__":
  yacc.yacc()
  s = open("python_grammar.py").read()
  import glob
  for filename in glob.glob('/Library/Frameworks/Python.framework/'
                            'Versions/2.5/lib/python2.5/*.py'):
    s = open(filename).read()
    print "Process", filename
    try:
      yacc.parse(s, lexer=python_lex.lexer)
    except:
      print "Process", filename
      raise

