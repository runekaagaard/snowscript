"""
Lexes a snow file given as first argument and returns its lexed tokens as json.
"""

import sys
import json
from lexer import SnowLexer
from tokens import token_groups
import tokens

tokens.MODE = tokens.MODES['HTML']

class SnowLexerHtml(SnowLexer):
    at_line_start = False
    def set_token_stream(self):
        self.token_stream = token_stream = iter(self.lexer.token, None)

if __name__ == '__main__':
    code = open(sys.argv[1]).read()
    lexer = SnowLexerHtml()
    lexer.input(code, '')
    token_types = {}
    for group_name,token_names in token_groups.iteritems():
        for token_name in token_names:
            token_types[token_name] = group_name
    result = ""
    for t in lexer:
        group = token_types[t.type]
        if t.type == 'NUMBER':
            value = t.value[1]
        else:
            value = t.value 
        result += "<span class='%s'>%s</span>" % (group, value)
    print result
        
