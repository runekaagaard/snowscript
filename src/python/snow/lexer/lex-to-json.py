"""
Lexes a snow file given as first argument and returns its lexed tokens as json.
"""

import sys
import json
from lexer import SnowLexer

if __name__ == '__main__':
    code = open(sys.argv[1]).read()
    lexer = SnowLexer()
    lexer.input(code, '')
    tokens = map(lambda t: dict(type=t.type, value=t.value), list(lexer))
    print json.dumps(tokens)
