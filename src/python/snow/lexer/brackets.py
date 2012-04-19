from ply import lex
from standard import t_LPAR, t_RPAR, t_LBRACE, t_RBRACE, t_LSQB, t_RSQB

@lex.TOKEN(t_LPAR)
def t_LPAR(t):
    t.lexer.bracket_level += 1
    return t

@lex.TOKEN(t_RPAR)
def t_RPAR(t):
    t.lexer.bracket_level -= 1
    return t

@lex.TOKEN(t_LBRACE)
def t_LBRACE(t):
    t.lexer.bracket_level += 1
    return t

@lex.TOKEN(t_RBRACE)
def t_RBRACE(t):
    t.lexer.bracket_level -= 1
    return t

@lex.TOKEN(t_LSQB)
def t_LSQB(t):
    t.lexer.bracket_level += 1
    return t

@lex.TOKEN(t_RSQB)
def t_RSQB(t):
    t.lexer.bracket_level -= 1
    return t
