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

def lex_to_html(snowscript):
    """
    Lexes snowscript code and returns its lexed tokens as prettyprinted html.
    """
    lexer = SnowLexerHtml()
    lexer.input(snowscript, '')
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
    return result
