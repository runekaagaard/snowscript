import re
import tokenize

# String literal from Python's Grammar/Grammar file to tokenization name
literal_to_name = {}

# List of tokens for PLY
tokens = []

# this list comes from keyword.list
# "as" is currently special, but with 2.6 it becomes a reserved keyword
#  (this is legal in pre 2.6: from UserDict import UserDict as as )
kwlist = ['and', 'as', 'assert', 'break', 'class', 'continue', 'def',
          'del', 'elif', 'else', 'except', 'exec', 'finally', 'for',
          'from', 'global', 'if', 'import', 'in', 'is', 'lambda',
          'not', 'or', 'pass', 'print', 'raise', 'return', 'try',
          'while', 'with', 'yield']

RESERVED = {}
for literal in kwlist:
    name = literal.upper()
    RESERVED[literal] = name
    literal_to_name[literal] = name
    tokens.append(name)

# These are sorted with 3-character tokens first, then 2-character then 1.
for line in """
LEFTSHIFTEQUAL  <<=
RIGHTSHIFTEQUAL  >>=
DOUBLESTAREQUAL  **=
DOUBLESLASHEQUAL  //=

EQEQUAL ==
NOTEQUAL !=
NOTEQUAL <>
LESSEQUAL <=
LEFTSHIFT <<
GREATEREQUAL >=
RIGHTSHIFT >>
PLUSEQUAL +=
MINEQUAL -=
DOUBLESTAR **
STAREQUAL *=
DOUBLESLASH //
SLASHEQUAL /=
VBAREQUAL |=
PERCENTEQUAL %=
AMPEREQUAL &=
CIRCUMFLEXEQUAL ^=

COLON :
COMMA ,
SEMI ;
PLUS +
MINUS -
STAR *
SLASH /
VBAR |
AMPER &
LESS <
GREATER >
EQUAL =
DOT .
PERCENT %
BACKQUOTE `
CIRCUMFLEX ^
TILDE ~
AT @

# The PLY parser replaces these with special functions
LPAR (
RPAR )
LBRACE {
RBRACE }
LSQB [
RSQB ]
""".splitlines():
    line = line.strip()
    if not line or line.startswith("#"):
        continue
    name, literal = line.split()
    literal_to_name[literal] = name
    if name not in tokens:
        tokens.append(name)  # N**2 operation, but N is small

    ## Used to verify that I didn't make a typo
    #if not hasattr(tokenize, name):
    #    raise AssertionError("Unknown token name %r" % (name,))

    # Define the corresponding t_ token for PLY
    # Some of these will be overridden
    t_name = "t_" + name
    if t_name in globals():
        globals()[t_name] += "|" + re.escape(literal)
    else:
        globals()[t_name] = re.escape(literal)

# Delete temporary names
del t_name, line, name, literal
