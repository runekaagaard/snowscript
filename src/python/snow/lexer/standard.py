import re
# Debug
from sys import exit as e
from pprint import pprint as p

# List of all Snow tokens.
tokens = ['ELIF', 'END', 'ISA', 'FN', 'NEXT', 'ABSTRACT', 'AND', 'CASE', 
          'CATCH', 'CLASS', 'CLONE', 'DECLARE', 'DEFAULT', 'DO', 'ELSE', 
          'EXTENDS', 'FINAL', 'FOR', 'IF', 'IMPLEMENTS', 'INTERFACE', 
          'NAMESPACE', 'NEW', 'OR', 'SWITCH', 'THROW', 'TRY', 'USE', 
          'WHILE', 'XOR', 'GLOBAL', 'PRIVATE', 'PROTECTED', 'PUBLIC', 'STATIC', 
          'WHEN', 
          'FALLTHRU', 'IN', 'TO', 'DOWNTO', '_AND_', '_OR_', 'MOD_EQUAL', 
          'MOD', 'BAND', 
          'BOR', 'BNOT', 'BOX', 'BXOR', 'BLEFT', 'BRIGHT', 'ECHO', 'EMPTY', 
          'EXIT', 'DIE', 'INCLUDE', 'INCLUDE_ONCE', 'ISSET', 'LIST', 'REQUIRE', 
          'REQUIRE_ONCE', 'PRINT', 'UNSET', 'INC', 'DEC', 'IS_IDENTICAL', 
          'IS_NOT_IDENTICAL', 'IS_EQUAL', 'IS_NOT_EQUAL', 'IS_SMALLER_OR_EQUAL', 
          'IS_GREATER_OR_EQUAL', 'PLUS_EQUAL', 'MINUS_EQUAL', 'MUL_EQUAL', 
          'DIV_EQUAL', 'CONCAT_EQUAL', 'SL_EQUAL', 'SR_EQUAL', 
          'AND_EQUAL', 'OR_EQUAL', 'XOR_EQUAL', 'SL', 'SR', 'POW', 'RETURN', 
          'INNER_RETURN', 'RECEIVER', 'DOUBLE_COLON', 'COLON', 'COMMA', 'SEMI', 
          'PLUS', 'MINUS', 'STAR', 'SLASH', 'PIPE', 'AMPER', 'LESS', 'GREATER', 
          'EQUAL', 'DOT', 'PERCENT', 'BACKQUOTE', 'CIRCUMFLEX', 'TILDE', 'AT', 
          'LPAR', 'RPAR', 'LBRACE', 'RBRACE', 'LSQB', 'RSQB', 'PASS', 'COMMENT', 
          'INSIDE_COMMENT', 'CONST', 'INLINE_HTML', 'ESCAPE', 
          'STRING_WITH_CONCAT', 'ARRAY', 'CALLABLE', 'TRUE', 'FALSE', 'NOT', 'NULL',
          'CLASS_NAME', 'CONSTANT_NAME', 'VARIABLE_NAME', 'INT', 'BOOL', 
          'FLOAT', 'OBJECT', 'STRINGTYPE', 'TRAIT'
]

# Dict of all reserved keywords.
RESERVED = {'and': 'AND', 'elif': 'ELIF', 'include_once': 'INCLUDE_ONCE', 
            'when': 'WHEN', 'abstract': 'ABSTRACT', 'private': 'PRIVATE', 'box': 'BOX', 
            'protected': 'PROTECTED', 'echo': 'ECHO', 'catch': 'CATCH', 'bright': 'BRIGHT', 
            'bnot': 'BNOT', 'exit': 'EXIT', 'die': 'DIE', 'in': 'IN', 'if': 'IF', 'fallthru': 
            'FALLTHRU', 'use': 'USE', 'end': 'END', 'for': 'FOR', 
            'downto': 'DOWNTO', 'namespace': 'NAMESPACE', 'while': 'WHILE', 
            'isset': 'ISSET', 'next': 'NEXT', 'to': 'TO', 'extends': 'EXTENDS', 
            'implements': 'IMPLEMENTS', 'print': 'PRINT', 'new': 'NEW', 
            'include': 'INCLUDE', 'final': 'FINAL', 'empty': 'EMPTY', 
            'require_once': 'REQUIRE_ONCE', 'do': 'DO', 'clone': 'CLONE', 
            'public': 'PUBLIC', 'else': 'ELSE', 'band': 'BAND', 'bor': 'BOR', 
            'interface': 'INTERFACE', 'xor': 'XOR', 'class': 'CLASS', 
            'fn': 'FN', 'case': 'CASE', 'throw': 'THROW', 
            '_and_': '_AND_', 'static': 'STATIC', 'global': 'GLOBAL', 
            'default': 'DEFAULT', 'bxor': 'BXOR', 'require': 'REQUIRE', 
            'list': 'LIST', 'or': 'OR', 'try': 'TRY', 'switch': 'SWITCH', 
            'bleft': 'BLEFT', '_or_': '_OR_', 'isa': 'ISA', 
            'declare': 'DECLARE', 'unset': 'UNSET', 'const': 'CONST', 
            'pass': 'PASS', 'array': 'ARRAY', 'callable': 'CALLABLE',
            'true': 'TRUE', 'false': 'FALSE', 'not': 'NOT', 'null': 'NULL',
            'int': 'INT', 'bool': 'BOOL',
            'float': 'FLOAT', 'object': 'OBJECT', 'str': 'STRINGTYPE',
            'trait': 'TRAIT',
            }

# Forces indentation.
INDENTATION_TRIGGERS = ('IF', 'ELSE', 'ELIF', 'FOR', 'SWITCH', 'CASE', 'WHILE',
                        'DEFAULT', 'FN', 'CLASS', 'INTERFACE', 'TRAIT',
                        # 'STATIC', 'PRIVATE', 'CONST', 'PUBLIC', 'PROTECTED',
                        )

CASTS = ('ARRAY', 'BOOL', 'DOUBLE', 'FLOAT', 'INT', 'OBJECT', 'STRINGTYPE', 'UNSET', 'REAL')

MISSING_PARENTHESIS = ('IF', 'ELIF', 'FOR', 'SWITCH', 'WHILE')

SYMBOLIC = ('INC', 'DEC', 'IS_IDENTICAL', 'IS_NOT_IDENTICAL', 'IS_EQUAL', 
            'IS_NOT_EQUAL', 't_IS_SMALLER_OR_EQUAL', 'IS_GREATER_OR_EQUAL', 
            'PLUS_EQUAL', 'MINUS_EQUAL', 'MUL_EQUAL', 'DIV_EQUAL', 
            'CONCAT_EQUAL', 'POW', 'RETURN', 'INNER_RETURN', 'RECEIVER', 
            'DOUBLE_COLON', 'COLON', 'COMMA', 'SEMI', 'PLUS', 'MINUS', 'STAR', 
            'SLASH', 'PIPE', 'AMPER', 'LESS', 'GREATER', 'EQUAL', 'DOT', 
            'PERCENT', 'BACKQUOTE', 'CIRCUMFLEX', 'TILDE', 'AT', 'LPAR', 'RPAR', 
            'LBRACE', 'RBRACE', 'LSQB', 'RSQB')

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
t_CONCAT_EQUAL = r'\%\='
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

def t_INLINE_HTML(t):
    r"(?sm)%>(?:.*<%|.*$)"
    # Strip leading and trailing %> / <%
    t.value = t.value[2:]
    if t.value[-2:] == '<%':
        t.value = t.value[:-2]
    return t

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
        the variable t.lexer.comment_indent that tells how far the current
        line is indented.
        """
        pos = t.lexpos
        while pos > 0:
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
    #
    # However, we collects the content of the COMMENT state from a clone of the
    # lexer and returns one master token.
    if next_line_indents_in(t):
        cloned_lexer = t.lexer.clone()
        cloned_lexer.begin('COMMENT')
        t.lexer.begin('COMMENT') # Make sure real lexer follows suit.
        for cloned_t in cloned_lexer:
            t.lexer.next() # Increment real lexer to keep it alligned.
            t.value += cloned_t.value
            if cloned_t.lexer.current_state() == 'INITIAL':
                return t
    return t

def t_COMMENT_error(t):
    raise_syntax_error("invalid syntax", t)
    
def t_COMMENT_INSIDE_COMMENT(t):
    r"\n([ ]*).*"
    # Empty lines are still comments, so we wont switch state back to INITIAL.
    if t.value.strip() == '':
        return t
    # Looks a head and changes the state back to INITIAL if the next line is not
    # indented further than the first comment line.
    lexer = t.lexer.clone()
    next = lexer.token()
    if next:
        next_line_indent = len(next.lexer.lexmatch.group(2))
        if next_line_indent <= t.lexer.comment_indent:
            t.lexer.begin('INITIAL')
    return t

def t_MOD_EQUAL(t): 
    r'mod\='; t.type = RESERVED.get(t.value, "MOD_EQUAL"); return t

def t_MOD(t): 
    r'mod'; t.type = RESERVED.get(t.value, "MOD"); return t

def t_SL_EQUAL(t): 
    r'bleft\='; t.type = RESERVED.get(t.value, "SL_EQUAL"); return t

def t_SR_EQUAL(t): 
    r'bright\='; t.type = RESERVED.get(t.value, "SR_EQUAL"); return t

def t_AND_EQUAL(t): 
    r'band\='; t.type = RESERVED.get(t.value, "AND_EQUAL"); return t

def t_OR_EQUAL(t): 
    r'bor\='; t.type = RESERVED.get(t.value, "OR_EQUAL"); return t

def t_XOR_EQUAL(t): 
    r'bxor\='; t.type = RESERVED.get(t.value, "XOR_EQUAL"); return t

def t_SL(t): 
    r'bleft\='; t.type = RESERVED.get(t.value, "SL"); return t

def t_SR(t): 
    r'bright\='; t.type = RESERVED.get(t.value, "SR"); return t

def t_constant_or_class_name(t):
    r"[A-Z][a-zA-Z0-9_]*"
    t.type = RESERVED.get(
        t.value, 
        "CONSTANT_NAME" if t.value == t.value.upper() and len(t.value) > 1 else "CLASS_NAME"
    )
    return t

def t_VARIABLE_NAME(t):
    r"[a-zA-Z_][a-zA-Z0-9_]*"
    t.type = RESERVED.get(t.value, "NAME")
    return t
    
def t_error(t):
    print t
    raise_syntax_error("invalid syntax", t)