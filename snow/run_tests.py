from glob import glob
from snow_lexer import SnowLexer
from difflib import unified_diff
from termcolor import colored
import sys

def lex_snow(code):
    """
    Lexes given Snow code and returns a string representing lexed tokens.
    """
    lexer = SnowLexer()
    lexer.input(code, '')
    tokens_as_string = ''
    for t in lexer:
        if type(t.value) == type(tuple()):
            value = t.value[1]
        else:
            value = t.value
        if value:
            value = value.replace('\n', r'\n')
        if t.type == 'STRING':
            value = value.replace(' ', '.')
        value = ": " + str(value)
        tokens_as_string += "%s%s\n" % (t.type, value)
    return tokens_as_string.strip()

# Run test 'suite'
failure = succes = 0
for file in glob('lexer/tests/*.test'):
    code, tokens_expected = [_.strip() for _ in open(file).read().split('----')]
    tokens_actual = lex_snow(code)
    if tokens_expected != tokens_actual:
        failure += 1
        print "Failing test: %s" % file
        for line in unified_diff(tokens_expected.splitlines(), 
            tokens_actual.splitlines(), fromfile='expected', tofile='actual', 
            lineterm=''):
            print line
        print
    else:
        succes +=1
        
msg = "%d failures and %d successes out of %d tests" % (failure, succes, 
                                                         failure + succes)
print colored(msg, 'white', 'on_red' if failure else 'on_green')
