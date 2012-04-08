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
    output = []
    i = 0
    for t in tokens:
        if t.type == 'NEWLINE' and tokens[i+1].type == 'INDENT':
            continue

        output.append({
            'type': t.type,
            'value': t.value,
        })
        i += 1
    return output
    
if __name__ == '__main__':
    matched_files = []
    for f in sys.argv[1:]:
        matched_files.extend(glob(f))
    for f in matched_files:
        output = prettyprint(open(f).read())
        import json
        print json.dumps(output)
        
    
