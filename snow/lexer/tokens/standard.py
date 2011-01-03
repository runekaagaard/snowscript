# List of all Snow tokens.
tokens = ['ELIF', 'END', 'ISA', 'FN', 'NEXT', 'ABSTRACT', 'AND', 'CASE', 'CATCH', 'CLASS', 'CLONE', 'DECLARE', 'DEFAULT', 'DO', 'ELSE', 'EXTENDS', 'FINAL', 'FOR', 'IF', 'IMPLEMENTS', 'INTERFACE', 'NAMESPACE', 'NEW', 'OR', 'SWITCH', 'THROW', 'TRY', 'USE', 'WHILE', 'XOR', 'GLO', 'PRI', 'PRO', 'PUB', 'STA', 'CON', 'WHEN', 'FALLTHRU', 'IN', 'TO', 'DOWNTO', '_AND_', '_OR_', 'MOD', 'BAND', 'BOX', 'BXOR', 'BLEFT', 'BRIGHT', 'ECHO', 'EMPTY', 'EXIT', 'INCLUDE', 'INCLUDE_ONCE', 'ISSET', 'LIST', 'REQUIRE', 'REQUIRE_ONCE', 'PRINT', 'UNSET', 'INC', 'DEC', 'IS_IDENTICAL', 'IS_NOT_IDENTICAL', 'IS_EQUAL', 'IS_NOT_EQUAL', 'IS_SMALLER_OR_EQUAL', 'IS_GREATER_OR_EQUAL', 'PLUS_EQUAL', 'MINUS_EQUAL', 'MUL_EQUAL', 'DIV_EQUAL', 'CONCAT_EQUAL', 'MOD_EQUAL', 'SL_EQUAL', 'SR_EQUAL', 'AND_EQUAL', 'OR_EQUAL', 'XOR_EQUAL', 'SL', 'SR', 'POW', 'RETURN', 'INNER_RETURN', 'RECEIVER', 'DOUBLE_COLON', 'COLON', 'COMMA', 'SEMI', 'PLUS', 'MINUS', 'STAR', 'SLASH', 'PIPE', 'AMPER', 'LESS', 'GREATER', 'EQUAL', 'DOT', 'PERCENT', 'BACKQUOTE', 'CIRCUMFLEX', 'TILDE', 'AT', 'LPAR', 'RPAR', 'LBRACE', 'RBRACE', 'LSQB', 'RSQB']

# Dict of all reserved keywords.
RESERVED = {'and': 'AND', 'elif': 'ELIF', 'include_once': 'INCLUDE_ONCE', 'when': 'WHEN', 'abstract': 'ABSTRACT', 'pri': 'PRI', 'box': 'BOX', 'pro': 'PRO', 'echo': 'ECHO', 'catch': 'CATCH', 'bright': 'BRIGHT', 'exit': 'EXIT', 'in': 'IN', 'if': 'IF', 'fallthru': 'FALLTHRU', 'use': 'USE', 'end': 'END', 'for': 'FOR', 'downto': 'DOWNTO', 'namespace': 'NAMESPACE', 'while': 'WHILE', 'isset': 'ISSET', 'next': 'NEXT', 'to': 'TO', 'extends': 'EXTENDS', 'implements': 'IMPLEMENTS', 'print': 'PRINT', 'new': 'NEW', 'include': 'INCLUDE', 'final': 'FINAL', 'empty': 'EMPTY', 'require_once': 'REQUIRE_ONCE', 'do': 'DO', 'clone': 'CLONE', 'pub': 'PUB', 'else': 'ELSE', 'band': 'BAND', 'interface': 'INTERFACE', 'xor': 'XOR', 'class': 'CLASS', 'fn': 'FN', 'mod': 'MOD', 'case': 'CASE', 'throw': 'THROW', '_and_': '_AND_', 'sta': 'STA', 'glo': 'GLO', 'default': 'DEFAULT', 'bxor': 'BXOR', 'require': 'REQUIRE', 'list': 'LIST', 'or': 'OR', 'try': 'TRY', 'switch': 'SWITCH', 'bleft': 'BLEFT', '_or_': '_OR_', 'isa': 'ISA', 'declare': 'DECLARE', 'unset': 'UNSET', 'con': 'CON'}

## Token definitions ##

t_INC = r'\+\+'
t_DEC = r'\-\-'
t_IS_IDENTICAL = r'\=\=\='
t_IS_NOT_IDENTICAL = r'\!\=\='
t_IS_EQUAL = r'\=\='
t_IS_NOT_EQUAL = r'\!\='
t_IS_SMALLER_OR_EQUAL = r'\<\='
t_IS_GREATER_OR_EQUAL = r'\>\='
t_PLUS_EQUAL = r'\+\='
t_MINUS_EQUAL = r'\-\='
t_MUL_EQUAL = r'\*\='
t_DIV_EQUAL = r'\/\='
t_CONCAT_EQUAL = r'\|\='
t_MOD_EQUAL = r'mod\='
t_SL_EQUAL = r'bleft\='
t_SR_EQUAL = r'bright\='
t_AND_EQUAL = r'band\='
t_OR_EQUAL = r'bor\='
t_XOR_EQUAL = r'bxor\='
t_SL = r'bleft'
t_SR = r'bright'
t_POW = r'\*\*'
t_RETURN = r'\<\-'
t_INNER_RETURN = r'\<\-\-'
t_RECEIVER = r'\-\>'
t_DOUBLE_COLON = r'\:\:'
t_COLON = r'\:'
t_COMMA = r'\,'
t_SEMI = r'\;'
t_PLUS = r'\+'
t_MINUS = r'\-'
t_STAR = r'\*'
t_SLASH = r'\/'
t_PIPE = r'\|'
t_AMPER = r'\&'
t_LESS = r'\<'
t_GREATER = r'\>'
t_EQUAL = r'\='
t_DOT = r'\.'
t_PERCENT = r'\%'
t_BACKQUOTE = r'\`'
t_CIRCUMFLEX = r'\^'
t_TILDE = r'\~'
t_AT = r'\@'
t_LPAR = r'\('
t_RPAR = r'\)'
t_LBRACE = r'\{'
t_RBRACE = r'\}'
t_LSQB = r'\['
t_RSQB = r'\]'

# I put this before t_WS so it can consume lines with only comments in them.
# This definition does not consume the newline; needed for things like
#    if 1: #comment
def t_comment(t):
    r"[ ]*\043[^\n]*"  # \043 is '#' ; otherwise PLY thinks it's an re comment
    pass
    
def t_NAME(t):
    r"[a-zA-Z_][a-zA-Z0-9_]*"
    t.type = RESERVED.get(t.value, "NAME")
    return t
