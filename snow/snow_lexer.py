"""
PLY tokenizer for the Snow language

@author Rune Kaagaard
"""

import re
import tokenize
from ply import lex
from tokens.standard import *
from tokens.whitespace import *
from tokens.number import *
from tokens.brackets import *
from tokens.quote import *
from error import  raise_syntax_error, raise_indentation_error
# Debugging, should go away.
from sys import exit as e
from pprint import pprint as p

# Settings.
SHOW_TOKENS = False

# Add extra internal tokens to the tokens from snow_tokens.py.
tokens = tuple(tokens) + ("NEWLINE", "NUMBER", "NAME", "WS", 
    "STRING_START_TRIPLE", "STRING_START_SINGLE", "STRING_CONTINUE", 
    "STRING_END", "STRING", "INDENT", "DEDENT", "ENDMARKER")

# The different states the lexer can operate in. Token names in non-initial
# states are written as "t_[STATE]_TOKENNAME]".
states = (
    ("SINGLEQ1", "exclusive"),
    ("SINGLEQ2", "exclusive"),
    ("TRIPLEQ1", "exclusive"),
    ("TRIPLEQ2", "exclusive"),
)

# This must be after "from tokens.quote import *".  Otherwise r"" is seen as 
# the NAME("r").
def t_NAME(t):
    r"[a-zA-Z_][a-zA-Z0-9_]*"
    t.type = RESERVED.get(t.value, "NAME")
    return t

##### Indentation

def _new_token(type, lineno):
    tok = lex.LexToken()
    tok.type = type
    tok.value = None
    tok.lineno = lineno
    tok.lexpos = -100
    return tok

# Synthesize a DEDENT tag.
def DEDENT(lineno):
    return _new_token("DEDENT", lineno)

# Synthesize an INDENT tag.
def INDENT(lineno):
    return _new_token("INDENT", lineno)

# Error token.
def t_error(t):
    raise_syntax_error("invalid syntax", t)

_lexer = lex.lex()

##### Handle quoted strings.

def _parse_quoted_string(start_tok, string_toks):
    """Pythonic strings like r"" are not supported in Snow."""
    s = "".join(tok.value for tok in string_toks)
    quote_type = start_tok.value.lower()
    if quote_type == "":
        return s.decode("string_escape")
    else:
        raise AssertionError("Unknown string quote type: %r" % (quote_type,))

def create_strings(lexer, token_stream):
    for tok in token_stream:
        if not tok.type.startswith("STRING_START_"):
            yield tok
            continue
        # This is a string start; process until string end
        start_tok = tok
        string_toks = []
        for tok in token_stream:
            #print " Merge string", tok
            if tok.type == "STRING_END":
                break
            else:
                assert tok.type == "STRING_CONTINUE", tok.type
                string_toks.append(tok)
        else:
            # Reached end of input without string termination
            # This reports the start of the line causing the problem.
            # Python reports the end.  I like mine better.
            raise_syntax_error(error_message[start_tok.type], start_tok)
        # Reached the end of the string
        if "SINGLE" in start_tok.type:
            # The compiler module uses the end of the single quoted
            # string to determine the strings line number.  I prefer
            # the start of the string.
            start_tok.lineno = tok.lineno
        start_tok.type = "STRING"
        start_tok.value = _parse_quoted_string(start_tok, string_toks)
        yield start_tok


##### Keep track of indentation state

# I implemented INDENT / DEDENT generation as a post-processing filter

# The original lex token stream contains WS and NEWLINE characters.
# WS will only occur before any other tokens on a line.

# I have three filters.  One tags tokens by adding two attributes.
# "must_indent" is True if the token must be indented from the
# previous code.  The other is "at_line_start" which is True for WS
# and the first non-WS/non-NEWLINE on a line.  It flags the check so
# see if the new line has changed indication level.

# Python's syntax has three INDENT states
#  0) no colon hence no need to indent
#  1) "if 1: go()" - simple statements have a COLON but no need for an indent
#  2) "if 1:\n  go()" - complex statements have a COLON NEWLINE and must indent
NO_INDENT = 0
MAY_INDENT = 1
MUST_INDENT = 2

# only care about whitespace at the start of a line
def annotate_indentation_state(lexer, token_stream):
    lexer.at_line_start = at_line_start = True
    indent = NO_INDENT
    saw_colon = False
    for token in token_stream:
        if SHOW_TOKENS:
            print "Got token:", token
        token.at_line_start = at_line_start

        if token.type == "COLON":
            at_line_start = False
            indent = MAY_INDENT
            token.must_indent = False
            
        elif token.type == "NEWLINE":
            at_line_start = True
            if indent == MAY_INDENT:
                indent = MUST_INDENT
            token.must_indent = False

        elif token.type == "WS":
            assert token.at_line_start == True
            at_line_start = True
            token.must_indent = False

        else:
            # A real token; only indent after COLON NEWLINE
            if indent == MUST_INDENT:
                token.must_indent = True
            else:
                token.must_indent = False
            at_line_start = False
            indent = NO_INDENT

        yield token
        lexer.at_line_start = at_line_start


# Track the indentation level and emit the right INDENT / DEDENT events.
def synthesize_indentation_tokens(token_stream):
    # A stack of indentation levels; will never pop item 0
    levels = [0]
    token = None
    depth = 0
    prev_was_ws = False
    for token in token_stream:
##        if 1:
##            print "Process", token,
##            if token.at_line_start:
##                print "at_line_start",
##            if token.must_indent:
##               print "must_indent",
##            print
                
        # WS only occurs at the start of the line
        # There may be WS followed by NEWLINE so
        # only track the depth here.  Don't indent/dedent
        # until there's something real.
        if token.type == "WS":
            assert depth == 0
            depth = len(token.value)
            prev_was_ws = True
            # WS tokens are never passed to the parser
            continue

        if token.type == "NEWLINE":
            depth = 0
            if prev_was_ws or token.at_line_start:
                # ignore blank lines
                continue
            # pass the other cases on through
            yield token
            continue

        # then it must be a real token (not WS, not NEWLINE)
        # which can affect the indentation level

        prev_was_ws = False
        if token.must_indent:
            # The current depth must be larger than the previous level
            if not (depth > levels[-1]):
                raise_indentation_error("expected an indented block", token)

            levels.append(depth)
            yield INDENT(token.lineno)

        elif token.at_line_start:
            # Must be on the same level or one of the previous levels
            if depth == levels[-1]:
                # At the same level
                pass
            elif depth > levels[-1]:
                # indentation increase but not in new block
                raise_indentation_error("unexpected indent", token)
            else:
                # Back up; but only if it matches a previous level
                try:
                    i = levels.index(depth)
                except ValueError:
                    # I report the error position at the start of the
                    # token.  Python reports it at the end.  I prefer mine.
                    raise_indentation_error(
     "unindent does not match any outer indentation level", token)
                for _ in range(i+1, len(levels)):
                    yield DEDENT(token.lineno)
                    levels.pop()

        yield token

    ### Finished processing ###

    # Must dedent any remaining levels
    if len(levels) > 1:
        assert token is not None
        for _ in range(1, len(levels)):
            yield DEDENT(token.lineno)
    

def add_endmarker(token_stream):
    tok = None
    for tok in token_stream:
        yield tok
    if tok is not None:
        lineno = tok.lineno
    else:
        lineno = 1
    yield _new_token("ENDMARKER", lineno)
_add_endmarker = add_endmarker

def make_token_stream(lexer, add_endmarker = True):
    token_stream = iter(lexer.token, None)
    token_stream = create_strings(lexer, token_stream)
    token_stream = annotate_indentation_state(lexer, token_stream)
    token_stream = synthesize_indentation_tokens(token_stream)
    if add_endmarker:
        token_stream = _add_endmarker(token_stream)
    return token_stream


_newline_pattern = re.compile(r"\n")
def get_line_offsets(text):
    offsets = [0]
    for m in _newline_pattern.finditer(text):
        offsets.append(m.end())
    # This is only really needed if the input does not end with a newline
    offsets.append(len(text))
    return offsets

class SnowLexer(object):
    def __init__(self, lexer = None):
        if lexer is None:
            lexer = _lexer.clone()
        self.lexer = lexer
        self.lexer.paren_count = 0
        self.lexer.is_raw = False
        self.lexer.filename = None
        self.token_stream = None

    def input(self, data, filename="<string>", add_endmarker=True):
        self.lexer.input(data)
        self.lexer.paren_count = 0
        self.lexer.is_raw = False
        self.lexer.filename = filename
        self.lexer.line_offsets = get_line_offsets(data)
        self.token_stream = make_token_stream(self.lexer, add_endmarker=True)

    def token(self):
        try:
            x = self.token_stream.next()
            #print "Return", x
            return x
        except StopIteration:
            return None

    def __iter__(self):
        return self.token_stream

lexer = SnowLexer()

text = open("test.snow").read()
lexer.input(text, "test.snow")
for tok in lexer:
    print tok
