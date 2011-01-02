import re
import tokenize

# String literal from Python's Grammar/Grammar file to tokenization name
literal_to_name = {}

# List of tokens for PLY
tokens = []

kwlist = [
    # Changed Keywords.
    'elif', 'end', 'isa', 'fn', 'next',     

    # Unchanged Keywords.
    'abstract', 'and', 'case', 'catch', 'class', 'clone', 'declare', 'default', 
    'do', 'else', 'extends', 'final', 'for', 'if', 'implements', 
    'interface', 'namespace', 'new', 'or', 'switch', 'throw', 'try', 'use', 
    'while', 'xor',   
    
    # Shortened Access Modifier Keywords. 
    'glo', 'pri', 'pro', 'pub', 'sta', 'con',
    
    # New keywords.
    'when', 'fallthru', 'in', 'to', 'downto',
    
    # New operator names.
    '_and_', '_or_', 'mod', 'band', 'box', 'bxor', 'bleft', 'bright',
    
    # Language constructs.
    'echo', 'empty', 'exit', 'include', 'include_once', 'isset', 'list', 
    'require', 'require_once', 'print', 'unset',
]

RESERVED = {}
for literal in kwlist:
    name = literal.upper()
    RESERVED[literal] = name
    literal_to_name[literal] = name
    tokens.append(name)

# These are sorted with 3-character tokens first, then 2-character then 1.
for line in """
INC ++
DEC --
IS_IDENTICAL ===
IS_NOT_IDENTICAL !==
IS_EQUAL ==
IS_NOT_EQUAL !=
IS_SMALLER_OR_EQUAL <=
IS_GREATER_OR_EQUAL >=
PLUS_EQUAL +=
MINUS_EQUAL -=
MUL_EQUAL *=
DIV_EQUAL /=
CONCAT_EQUAL |=
MOD_EQUAL mod=
SL_EQUAL bleft=
SR_EQUAL bright=
AND_EQUAL band=
OR_EQUAL bor=
XOR_EQUAL bxor=
SL bleft
SR bright
POW **

RETURN <-
INNER_RETURN <--

DOUBLE_COLON ::
COLON :
COMMA ,
SEMI ;
PLUS +
MINUS -
STAR *
SLASH /
PIPE |
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

from pprint import pprint as p
p(tokens)
