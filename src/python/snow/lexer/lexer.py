"""
PLY tokenizer for the Snow language.

@author Rune Kaagaard
"""

import re
import tokenize
from ply import lex
from standard import *
from whitespace import *
from number import *
from brackets import *
from strings import *
from indentation import get_line_offsets, make_token_stream
from error import raise_syntax_error, raise_indentation_error
import sys
# Debugging, should go away.
from sys import exit as e
from pprint import pprint as p

# Add extra internal tokens to the tokens from snow_tokens.py.
tokens = tuple(tokens) + ("NEWLINE", "NUMBER", "NAME", "WS", 
    "STRING_START_TRIPLE", "STRING_START_SINGLE", "STRING_CONTINUE", 
    "STRING_END", "STRING", "INDENT", "DEDENT", "ENDMARKER")

# The different states the lexer can operate in. Token names in non-initial
# states are written as "t_[STATE]_TOKENNAME]".
states = (
    ("COMMENT", "exclusive"),
    ('INDOUBLEQUOTEDSTRING', 'exclusive'),
    ('INSINGLEQUOTEDSTRING', 'exclusive'),
    ('INTRIPPLEDOUBLEQUOTEDSTRING', 'exclusive'),
    ('INTRIPPLESINGLEQUOTEDSTRING', 'exclusive'),
    ('SNOWINANYDOUBLEQUOTEDSTRING', 'inclusive'),
)

class SnowLexer(object):
    """
    The Snow lexer class.
    
    Extends the default PLY lexer by adding rules for indentation and other
    whitespace stuff.
    """
    def __init__(self, lexer=None):
        if lexer is None:
            lexer = lex.lex().clone()
        self.lexer = lexer
        self._reset()
        self.lexer.filename = None
        self.token_stream = None

    def input(self, data, filename="<string>", add_endmarker=True):
        self._reset()
        self.lexer.input(data)
        self.lexer.filename = filename
        self.lexer.line_offsets = get_line_offsets(data)
        self.token_stream = make_token_stream(self.lexer, add_endmarker=True)
    
    def _reset(self):
        self.lexer.paren_count = 0
        self.lexer.is_raw = False
        self.lexer.string_been_concat = False
        
    def token(self):
        try:
            t = self.token_stream.next()
            return t
        except StopIteration:
            return None

    def __iter__(self):
        return self.token_stream
