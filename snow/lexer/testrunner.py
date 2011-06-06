#!/usr/bin/env python

from argh import *
import argparse
from glob import glob
from lexer import SnowLexer
from difflib import unified_diff
from termcolor import colored
import sys
import os
import plac
from prettyprint import prettyprint

@plac.annotations(
    accept=('Accept matched tests as working', 'flag', 'a'),
    files=('file1, file2, ... fileN'),
)
def run(accept=False, *files):
    """Runs the tests."""
    
    def get_matched_files(files):
        matched_files = []
        for f in files:
            matched_files.extend(glob(f))
        return matched_files
    
    def test_file(f):
        content = open(f).read()
        sections = content.split('++++')
        for section in sections:
            section = section.strip()
            snowcode, tokens = [s.strip() for s in section.split('----')]
            result = prettyprint(snowcode)
            print [tokens.strip()]
            print ",,,,,,,,,"
            print [result]
        #print sections
        sections = 32
        
    matched_files = get_matched_files(files)
    for f in matched_files:
        test_file(f)
    
if __name__ == '__main__':
    import plac; plac.call(run)
"""
if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Runs lexer tests.')
    parser.add_argument('run')
    
    args = parser.parse_args()
    print args.accumulate(args)"""
"""
# Parse args
glob_string = '*.test' if len(sys.argv) < 2 else sys.argv[1]

# Set dir
os.chdir(os.path.abspath(os.path.dirname(__file__)))

# Delete old .out files.
os.system('rm -f tests/*.out')

# Run test 'suite'
failure = succes = 0
for file in glob('tests/' + glob_string):
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
"""