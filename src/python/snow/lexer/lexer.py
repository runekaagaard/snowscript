"""
PLY tokenizer for the Snow language.

@author Rune Kaagaard
"""

from ply import lex
from tokens import *
from transformations import get_line_offsets, make_token_stream

# Add extra internal tokens to the tokens from snow_tokens.py.
tokens = tuple(tokens) + ("NEWLINE", "NUMBER", "NAME", "WS",
    "STRING_START_TRIPLE", "STRING_START_SINGLE", "STRING_CONTINUE",
    "STRING_END", "STRING", "INDENT", "DEDENT", "ENDMARKER")

# The different states the lexer can operate in. Token names in non-initial
# states are written as "t_[STATE]_[TOKENNAME]".
states = (
    ('INSIDEARRAY', 'inclusive'),
    ('INDOUBLEQUOTEDSTRING', 'exclusive'),
    ('INSINGLEQUOTEDSTRING', 'exclusive'),
    ('INTRIPPLEDOUBLEQUOTEDSTRING', 'exclusive'),
    ('INTRIPPLESINGLEQUOTEDSTRING', 'exclusive'),
    ('SNOWINANYDOUBLEQUOTEDSTRING', 'inclusive'),
    ('MULTILINECOMMENT', 'exclusive'),
)


class SnowLexer(object):
    """
    The Snow lexer class.

    Extends the default PLY lexer by adding rules for indentation and other
    whitespace stuff.
    """
    
    def __init__(self, lexer=None):
        if lexer is None:
            lexer = lex.lex()
        self.lexer = lexer
        self._reset()
        self.lexer.filename = None
        self.token_stream = None

    def input(self, data, filename="<string>", add_endmarker=True):
        self._reset()
        self.lexer.input(data)
        self.lexer.filename = filename
        self.lexer.line_offsets = get_line_offsets(data)
        self.set_token_stream()
        
    def set_token_stream(self):
        self.token_stream = make_token_stream(self.lexer, add_endmarker=True)        

    def _reset(self):
        self.lexer.bracket_level = 0
        self.lexer.is_raw = False
        self.lexer.string_been_concat = False

    def __iter__(self):
        return self.token_stream
