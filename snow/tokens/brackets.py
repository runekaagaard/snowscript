from ply import lex
from standard import t_LPAR, t_RPAR, t_LBRACE, t_RBRACE, t_LSQB, t_RSQB

@lex.TOKEN(t_LPAR)
def t_LPAR(t):
    t.lexer.paren_count += 1
    return t

@lex.TOKEN(t_RPAR)
def t_RPAR(t):
    t.lexer.paren_count -= 1
    return t

@lex.TOKEN(t_LBRACE)
def t_LBRACE(t):
    r"\{"
    t.lexer.paren_count += 1
    return t

@lex.TOKEN(t_RBRACE)
def t_RBRACE(t):
    r"\}"
    t.lexer.paren_count -= 1
    return t

@lex.TOKEN(t_LSQB)
def t_LSQB(t):
    t.lexer.paren_count += 1
    return t

@lex.TOKEN(t_RSQB)
def t_RSQB(t):
    t.lexer.paren_count -= 1
    return t
