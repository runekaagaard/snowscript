import sys
import os
from glob import glob
from difflib import unified_diff
from termcolor import colored

import string
from standard import tokens, SYMBOLIC

from lexer import SnowLexer

# Debug
from sys import exit as e

def prettyprint(code, with_colors=False):
    def nocolor(s, *args, **kwargs):
        return s
    
    if not with_colors:
        colored = nocolor
        
    def get_tokens(code):
        lexer = SnowLexer()
        lexer.input(code, '')
        tokens = [t for t in lexer]
        tokens_with_code = [t for t in tokens if t.lexpos != -1]
        return tokens, tokens_with_code
    
    def add_code_to_tokens(tokens):
        i = lexpos = 0
        for t in tokens:
            try:
                nextlexpos = tokens[i+1].lexpos
            except IndexError:
                nextlexpos = len(code)         
            t.code = code[t.lexpos:nextlexpos]
            lexpos = t.lexpos+1
            i += 1
        
    def check_code_on_tokens(tokens):
        codefromtokens = "".join(t.code for t in tokens)
        if code != codefromtokens:
            print "\n".join(l for l in unified_diff(code.split("\n"), 
                                                    codefromtokens.split("\n")))
    
    def get_value(t):
        if type(t.value) == type(tuple()):
            return t.value[0]
        else:
            if 'STRING' not in t.type and t.value == t.type.lower():
                return None
            else:
                return t.value
            
    def get_indent(indention):
        return " " * (indention * 4)
    
    tokens, tokens_with_code = get_tokens(code)
    add_code_to_tokens(tokens_with_code)
    check_code_on_tokens(tokens_with_code)
    
    indention = 0
    i = 0
    res = ""
    for t in tokens:
        try: t_next = tokens[i+1]
        except IndexError: t_next = None
        if t.type == 'ENDMARKER':
            continue
        elif t.type == 'NEWLINE':
            if t_next.type not in ('INDENT', 'DEDENT'):
                res += t.value + get_indent(indention)  
            else:
                res += t.value
        elif t.type == 'INDENT':
            indention += 1
            res += get_indent(indention)
        elif t.type == 'DEDENT':
            indention -= 1
            res += get_indent(indention)
        elif t.type in SYMBOLIC:
            res += colored(t.value, 'blue') + " "
        else:
            value = get_value(t)
            if value is not None:
                res += "%s<%s>" % (t.type, colored(value, 'green')) + " "
            else:
                res += t.type + " "
        i += 1
        
    return res