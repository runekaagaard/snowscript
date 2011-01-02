"""This program converts Python's 'Grammar' file into a PLY grammar

The Grammar file is pretty simple but not designed for LALR(1) and
similar parsers.  This program tweaks the grammar slightly and
flattens the results to a more usable form.  It might prove useful for
other parsers.

"""

# Written by Andrew Dalke
# Copyright (c) 2008 by Dalke Scientific, AB
# 
# See LICENSE for details.


import itertools

from ply import lex, yacc

# Used to convert 'strings' into token names
import python_tokens

filename = "/Users/dalke/cvses/python-svn/Grammar/Grammar"

duplicates = {
    "varargslist_star2": "varargslist_star",
    "print_stmt_star": "print_stmt_plus",
    "import_from_star": "import_from_plus",
}

tokens = ("LEXER_NAME", "PARSER_NAME", "STRING",
          "NL", "LPAR", "RPAR", "COLON")

def t_comment(t):
    r"\#.*"
    pass

t_ignore = " \t"

def t_NL(t):
    r"\n"
    t.value = t.lexer.lineno
    t.lexer.lineno += 1
    if getattr(t.lexer, "paren_depth", 0) == 0:
        return t

def t_word(t):
    r"[a-zA-Z_0-9]+"
    if t.value == t.value.upper():
        t.type = "LEXER_NAME"
        return t
    if t.value == t.value.lower():
        t.type = "PARSER_NAME"
        return t
    raise AssertionError("Unknown word: %r" % t.value)

t_STRING = r"'[^']+'"

def t_LPAR(t):
    r"\("
    t.lexer.paren_depth = getattr(t.lexer, "paren_depth", 0)+1
    return t

def t_RPAR(t):
    r"\)"
    t.lexer.paren_depth = getattr(t.lexer, "paren_depth", 0)-1
    assert t.lexer.paren_depth >= 0
    return t

def t_COLON(t):
    r":"
    t.value = t.lexer.lineno
    return t

literals = ('[', ']', '|', '+', '*')

def t_error(t):
    raise AssertionError(t)

lexer = lex.lex()

if 0:
    lexer.input("ANDREW")
    while 1:
        x = lexer.token()
        if not x:
            break
        print x

class Definition(object):
    def __init__(self, name, expr, first_line, last_line):
        self.name = name
        self.expr = expr
        self.first_line = first_line
        self.last_line = last_line
    def __repr__(self):
        return "Definition(%r, %r, %r, %r)" % (
            self.name, self.expr, self.first_line, self.last_line)

class Star(object):
    def __init__(self, child):
        self.child = child
    def __repr__(self):
        return "Star(%r)" % (self.child,)

class Plus(object):
    def __init__(self, child):
        self.child = child
    def __repr__(self):
        return "Plus(%r)" % (self.child,)

class Opt(object):
    def __init__(self, child):
        self.child = child
    def __repr__(self):
        return "Opt(%r)" % (self.child,)


class Or(object):
    def __init__(self, left, right):
        self.left = left
        self.right = right
    def __repr__(self):
        return "Or(%r, %r)" % (self.left, self.right)

class Seq(object):
    def __init__(self, first, next):
        self.first = first
        self.next = next
    def __repr__(self):
        return "Seq(%r, %r)" % (self.first, self.next)

def p_datafile1(p):
    """datafile : definition
                | datafile definition"""
    if len(p) == 2:
        p[0] = [p[1]]
    else:
        p[0] = p[1] + [p[2]]

def p_datafile(p):
    """datafile : NL
                | datafile NL"""
    if len(p) == 3:
        p[0] = p[1]
    else:
        p[0] = []
            


def p_definition(p):
    """definition : PARSER_NAME COLON expr NL"""
    p[0] = Definition(p[1], p[3], p[2], p[4])

def p_expr(p):
    """expr : sequential_terms
            | expr '|' sequential_terms"""
    if len(p) == 2:
        p[0] = p[1]
    else:
        p[0] = Or(p[1], p[3])

def p_sequential_terms(p):
    """sequential_terms : term
                        | sequential_terms term"""
    if len(p) == 2:
        p[0] = p[1]
    else:
        p[0] = Seq(p[1], p[2])

def p_term(p):
    """term : element '*'
             | element '+'
             | element
             """
    if len(p) == 3:
        if p[2] == "+":
            p[0] = Plus(p[1])
        elif p[2] == "*":
            p[0] = Star(p[1])
        else:
            raise AssertionError(p[2])
    else:
        p[0] = p[1] # no repeat


def p_element(p):
    """element : '[' expr ']'
               | LPAR expr RPAR
               | STRING
               | LEXER_NAME
               | PARSER_NAME"""
    if len(p) == 4:
        if p[1] == '[':
            p[0] = Opt(p[2])
        else:
            p[0] = p[2] # no repeat
    elif p[1].startswith("'"):
        # Quoted string; turn into a token name
        literal = p[1][1:-1]
        p[0] = python_tokens.literal_to_name[literal]
    else:
        p[0] = p[1]

def p_error(p):
    raise AssertionError(p)
             
yacc.yacc()

#s = "this: THAT\nthat: THIS\n")
s = open(filename).read()

# Both of these map to NOTEQUAL
# Easiest way to fix it is to patch the grammar
grammar_text = s.replace("'<>'|'!='", "'!='")

definition_list = yacc.parse(grammar_text)
print "Result", definition_list

varargslist = yacc.parse(
"varargslist: (fpdef ['=' test] (',' fpdef ['=' test])* "
"(',' '*' NAME [',' '**' NAME] | ',' '**' NAME | [','])) |"
"('*' NAME [',' '**' NAME]) |"
"('**' NAME)\n"
)[0]

def add_flattened_definition(name, flat_expr):
    print name, ":", flat_expr

_seen_names = set()
def new_name(name):
    if name in _seen_names:
        for i in itertools.count(2):
            name2 = name + str(i)
            if name2 not in _seen_names:
                break
        name = name2
    _seen_names.add(name)
    return name

def flatten(name, expr, need_list):
    if isinstance(expr, Seq):
        for first_terms in flatten(name, expr.first, need_list):
            for next_terms in flatten(name, expr.next, need_list):
                yield first_terms + next_terms

    elif isinstance(expr, Or):
        for left_terms in flatten(name, expr.left, need_list):
            yield left_terms
        for right_terms in flatten(name, expr.right, need_list):
            yield right_terms

    elif isinstance(expr, Star):
        yield []
        child_name = new_name(name + "_star")
        yield [child_name]
        need_list.append( (child_name, expr.child) )

    elif isinstance(expr, Plus):
        child_name = new_name(name + "_plus")
        yield [child_name]
        need_list.append( (child_name, expr.child) )
    
    elif isinstance(expr, Opt):
        yield []
        for term in flatten(name, expr.child, need_list):
            yield term

    elif isinstance(expr, str):
        yield [expr]

    else:
        raise AssertionError(expr)
        


f = open("python_grammar.py", "w")
def W(s):
    f.write(s + "\n")

W("from ply import yacc")
W("import python_lex")
W("tokens = python_lex.tokens")

def format_function(name, rules):
    W("def p_%s(p):" % name)
    if len(rules) == 1:
        W("  '%s : %s'" % (name, rules[0]))
    else:
        W("  '''")
        W("  %s : %s" % (name, rules[0]))
        for rule in rules[1:]:
            W("    | %s" % (rule,))
        W("  '''")
    W("  p[0] = p[1:]")
    
    
grammar_lines = grammar_text.splitlines()

for definition in definition_list:
    if definition.name in ("single_input", "eval_input"):
        continue
    if definition.name == "varargslist":
        definition.expr = varargslist.expr
        
    rules = []
    need_list = []
    for terms in flatten(definition.name, definition.expr, need_list):
        terms = [duplicates.get(term, term) for term in terms]
        rules.append( " ".join(terms) )

    W("\n# " + 
      "\n# ".join(grammar_lines[definition.first_line-1:definition.last_line]))
    format_function(definition.name, rules)

    while need_list:
        name, expr = need_list.pop(0)
        if name in duplicates:
            continue
        rules = []
        for terms in flatten(name, expr, need_list):
            terms = [duplicates.get(term, term) for term in terms]
            rules.append( " ".join(terms) )
        rules = rules + [name + " " + rule for rule in rules]
        W("")
        format_function(name, rules)

W("def p_error(p):")
W("  raise AssertionError(p)")

W("""
if __name __ == "__main_":
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
""")
