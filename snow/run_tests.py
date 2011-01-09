from glob import glob
from snow_lexer import SnowLexer
from difflib import unified_diff
from termcolor import colored
import sys
import os
# Debug
from sys import exit as e

def lex_snow(code):
    """
    Lexes given Snow code and returns a string representing lexed tokens.
    """
    lexer = SnowLexer()
    lexer.input(code, '')
    tokens_as_string = ''
    for t in lexer:
        print t
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

# Parse args
glob_string = '*.test' if len(sys.argv) < 2 else sys.argv[1]

# Set dir
os.chdir(os.path.abspath(os.path.dirname(__file__)))

# Delete old .out files.
os.system('rm -f lexer/tests/*.out')

# Run test 'suite'
failure = succes = 0
for file in glob('lexer/tests/' + glob_string):
    code, tokens_expected = [_.strip() for _ in open(file).read().split('----')]
    tokens_actual = lex_snow(code)
    if tokens_expected != tokens_actual:
        failure += 1
        print colored("Failing test: %s" % file, 'red')
        for line in unified_diff(tokens_expected.splitlines(), 
            tokens_actual.splitlines(), fromfile='expected', tofile='actual', 
            lineterm=''):
            print line
        print
        with open(file.replace('.test', '') + '.out', 'w') as f:
            f.write(tokens_actual)
    else:
        succes +=1
        
msg = "%d total :: %d good :: %d bad" % (failure + succes, succes, failure)
print colored(msg, 'white', 'on_red' if failure else 'on_green')

