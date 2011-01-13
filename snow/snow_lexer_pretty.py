from glob import glob
from snow_lexer import SnowLexer
from difflib import unified_diff
from termcolor import colored
from lexer.tokens.standard import tokens
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
    code_lines = code.split("\n")
    last_printed_pos = 0
    tokens_as_string = ''
    indent = 0
    has_newline = False
    no_prefix_next_time = False
    for t in lexer:
        if t.type == 'INDENT':
            indent += 1
        elif t.type == 'DEDENT':
            indent -= 1
        elif t.type == 'NEWLINE':
            tokens_as_string += t.value
            tokens_as_string += colored(code[last_printed_pos:t.lexpos], 'yellow') + "\n"
            last_printed_pos = t.lexpos + 1
            has_newline = True
        elif t.type == 'ENDMARKER':
            nl = "" if has_newline else "\n"
            tokens_as_string +=  "\n" + colored(code[last_printed_pos:], 'yellow')
            tokens_as_string += "%s%s" % (nl, t.type)
        else:
            is_special = t.type in tokens and t.type not in ('STRING_WITH_CONCAT', )
            prefix = " " if not has_newline else ""
            if no_prefix_next_time:
                prefix = ''
                no_prefix_next_time = False
            if t.value in "({[" and len(t.value) == 1:
                no_prefix_next_time = True
            if t.value in "({[)}]" and len(t.value) == 1:
                prefix = ''
            if t.value == ':':
                prefix = ''
            indention = (" " if has_newline else "") * indent * 4
            has_newline = False
            token = t.value if is_special else "%s{'%s'}" % (t.type, t.value)
            tokens_as_string += "%s%s%s" % (
                prefix, indention, token
            )
        continue
        """if type(t.value) == type(tuple()):
            value = t.value[1]
        else:
            value = t.value
        if value:
            value = value.replace('\n', r'\n')
        if t.type == 'STRING':
            value = value.replace(' ', '.')
        value = ": " + str(value)
        tokens_as_string += "%s%s\n" % (t.type, value)"""
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
    print colored("Running test: %s" % file, 'cyan')
    code, tokens_expected = [_.strip() for _ in open(file).read().split('----')]
    tokens_actual = lex_snow(code)
    try:
        tokens_actual = lex_snow(code)
    except Exception as e:
        tokens_actual = "%s: %s (%s)" % (e.__class__, e.message, e.args)
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

