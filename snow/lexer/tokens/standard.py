import re
# Debug
from sys import exit as e
from pprint import pprint as p

# List of all Snow tokens.
tokens = ['ELIF', 'END', 'ISA', 'FN', 'NEXT', 'ABSTRACT', 'AND', 'CASE', 'CATCH', 'CLASS', 'CLONE', 'DECLARE', 'DEFAULT', 'DO', 'ELSE', 'EXTENDS', 'FINAL', 'FOR', 'IF', 'IMPLEMENTS', 'INTERFACE', 'NAMESPACE', 'NEW', 'OR', 'SWITCH', 'THROW', 'TRY', 'USE', 'WHILE', 'XOR', 'GLO', 'PRI', 'PRO', 'PUB', 'STA', 'CON', 'WHEN', 'FALLTHRU', 'IN', 'TO', 'DOWNTO', '_AND_', '_OR_', 'MOD', 'BAND', 'BOX', 'BXOR', 'BLEFT', 'BRIGHT', 'ECHO', 'EMPTY', 'EXIT', 'INCLUDE', 'INCLUDE_ONCE', 'ISSET', 'LIST', 'REQUIRE', 'REQUIRE_ONCE', 'PRINT', 'UNSET', 'INC', 'DEC', 'IS_IDENTICAL', 'IS_NOT_IDENTICAL', 'IS_EQUAL', 'IS_NOT_EQUAL', 'IS_SMALLER_OR_EQUAL', 'IS_GREATER_OR_EQUAL', 'PLUS_EQUAL', 'MINUS_EQUAL', 'MUL_EQUAL', 'DIV_EQUAL', 'CONCAT_EQUAL', 'MOD_EQUAL', 'SL_EQUAL', 'SR_EQUAL', 'AND_EQUAL', 'OR_EQUAL', 'XOR_EQUAL', 'SL', 'SR', 'POW', 'RETURN', 'INNER_RETURN', 'RECEIVER', 'DOUBLE_COLON', 'COLON', 'COMMA', 'SEMI', 'PLUS', 'MINUS', 'STAR', 'SLASH', 'PIPE', 'AMPER', 'LESS', 'GREATER', 'EQUAL', 'DOT', 'PERCENT', 'BACKQUOTE', 'CIRCUMFLEX', 'TILDE', 'AT', 'LPAR', 'RPAR', 'LBRACE', 'RBRACE', 'LSQB', 'RSQB', 'PASS', 'COMMENT', 'INSIDE_COMMENT']

# Dict of all reserved keywords.
RESERVED = {'and': 'AND', 'elif': 'ELIF', 'include_once': 'INCLUDE_ONCE', 'when': 'WHEN', 'abstract': 'ABSTRACT', 'pri': 'PRI', 'box': 'BOX', 'pro': 'PRO', 'echo': 'ECHO', 'catch': 'CATCH', 'bright': 'BRIGHT', 'exit': 'EXIT', 'in': 'IN', 'if': 'IF', 'fallthru': 'FALLTHRU', 'use': 'USE', 'end': 'END', 'for': 'FOR', 'downto': 'DOWNTO', 'namespace': 'NAMESPACE', 'while': 'WHILE', 'isset': 'ISSET', 'next': 'NEXT', 'to': 'TO', 'extends': 'EXTENDS', 'implements': 'IMPLEMENTS', 'print': 'PRINT', 'new': 'NEW', 'include': 'INCLUDE', 'final': 'FINAL', 'empty': 'EMPTY', 'require_once': 'REQUIRE_ONCE', 'do': 'DO', 'clone': 'CLONE', 'pub': 'PUB', 'else': 'ELSE', 'band': 'BAND', 'interface': 'INTERFACE', 'xor': 'XOR', 'class': 'CLASS', 'fn': 'FN', 'mod': 'MOD', 'case': 'CASE', 'throw': 'THROW', '_and_': '_AND_', 'sta': 'STA', 'glo': 'GLO', 'default': 'DEFAULT', 'bxor': 'BXOR', 'require': 'REQUIRE', 'list': 'LIST', 'or': 'OR', 'try': 'TRY', 'switch': 'SWITCH', 'bleft': 'BLEFT', '_or_': '_OR_', 'isa': 'ISA', 'declare': 'DECLARE', 'unset': 'UNSET', 'con': 'CON', 'pass': 'PASS'}

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

NEXT_LINE = re.compile(r'\n([ ]*)(.*)') 
def t_COMMENT(t):
    r"[ ]*\043[^\n]*"  # \043 is '#' ; otherwise PLY thinks it's an re comment
    def next_line_indents_in(t):
        """
        Checks if the next line has a greater indention than current.
        """
        ms = NEXT_LINE.search(t.lexer.lexdata, t.lexer.lexpos)
        return False if not ms else len(ms.group(1)) > t.lexer.comment_indent
        
    def comment_indent(t):
        """
        Traverses back until it reaches a new line or start of file and sets
        the variable t.lexer.comment.indent that tells how far the current
        line is indented.
        """
        pos = t.lexpos
        while pos > -1:
            if t.lexer.lexdata[pos] == "\n":
                t.lexer.comment_indent = t.lexpos - pos - 1
                break
            pos -= 1
        else:
            t.lexer.comment_indent = 0    
            
    comment_indent(t)
    
    # If the next line has a greater indention it's a comment so we change
    # the state. Then the t_COMMENT_INSIDE_COMMENT token uses the value set in
    # t.lexer.comment_indent to collect all lines that are indented deeper than
    # this one.
    if next_line_indents_in(t):
        t.lexer.begin('COMMENT')
    
    return t

def t_COMMENT_error(t):
    raise_syntax_error("invalid syntax", t)
    
def t_COMMENT_INSIDE_COMMENT(t):
    r"\n([ ]*).*"
    # Looks a head and changes the state back to INITIAL if the next line is not
    # indented further than the first comment line.
    lexer = t.lexer.clone()
    next = lexer.token()
    if next:
        next_line_indent = len(next.lexer.lexmatch.group(2))
        if next_line_indent <= t.lexer.comment_indent:
            t.lexer.begin('INITIAL')
    return t

def t_NAME(t):
    r"[a-zA-Z_][a-zA-Z0-9_]*"
    t.type = RESERVED.get(t.value, "NAME")
    return t
    
def t_error(t):
    raise_syntax_error("invalid syntax", t)
