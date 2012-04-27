import sys
from glob import glob
from lexer import SnowLexer


def get_tokens(code, with_colors=False):
    def lex_code(code):
        lexer = SnowLexer()
        lexer.input(code, '')
        tokens = [t for t in lexer]
        return tokens

    tokens = lex_code(code)
    output = []
    for t in tokens:
        output.append({
            'type': t.type,
            'value': t.value,
        })
    return output

if __name__ == '__main__':
    matched_files = []
    for f in sys.argv[1:]:
        matched_files.extend(glob(f))
    for f in matched_files:
        output = get_tokens(open(f).read())
        import json
        print json.dumps(output)
