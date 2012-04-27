import sys
import json
from glob import glob
from lexer import SnowLexer


def get_tokens(code, with_colors=False):
    def lex_code(code):
        lexer = SnowLexer()
        lexer.input(code, '')
        return [t for t in lexer]

    return [{'type': t.type, 'value': t.value, } for t in lex_code(code)]

if __name__ == '__main__':
    matched_files = []
    for f in sys.argv[1:]:
        matched_files.extend(glob(f))
    for f in matched_files:
        output = get_tokens(open(f).read())
        print json.dumps(output)
