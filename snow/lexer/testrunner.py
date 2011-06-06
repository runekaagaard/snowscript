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
from string import strip
from collections import defaultdict

@plac.annotations(
    accept=('accept matched tests as correct', 'flag', 'a'),
    files=('file1, file2, ... fileN'),
)
def run(accept=False, *files):
    """Snow token testrunner."""
    
    def get_matched_files(files):
        matched_files = []
        for f in files:
            matched_files.extend(glob(f))
        return matched_files
    
    def get_error_diff(a, b):
        return "\n".join(l.strip() for l in unified_diff(a.split("\n"), 
                                                         b.split("\n")))
        
    def test_file(f, stats):
        stats['total'] += 1
        file_failed_count = 0
        error_diff = ''
        content = open(f).read()
        sections = content.split('++++')
        for section in sections:
            code, tokens_target = map(strip, section.split('----'))
            tokens_actual = prettyprint(code).strip()
            if tokens_actual != tokens_target:
                file_failed_count = 1
                error_diff += get_error_diff(tokens_target, tokens_actual)
        
        stats['failed'] += file_failed_count
        return not bool(file_failed_count), error_diff
    
    def run_tests(files):
        stats = defaultdict(lambda: 0)
        for f in files:
            print colored('Testing file %s' % f, 'cyan'),
            test_passed, error_diff = test_file(f, stats)
            print colored('OK', 'green') if test_passed else colored('FAIL', 
                                                                     'red')
            if error_diff:
                print error_diff
                print
            
        if stats['failed'] == 0:
            print colored('All %d tests passed' % stats['total'], 'white', 
                          'on_green')
        else:
            print colored('%d test(s) out of %d failed' % (stats['total'], 
                                            stats['failed']), 'white', 'on_red')
    
    def accept_tests(files):
        corrected_content = ''
        for f in files:
            content = open(f).read()
            sections = content.split('++++')
            i = 0
            for section in sections:
                if i>0:
                    corrected_content += "\n++++\n"
                code, _unused = map(strip, section.split('----'))
                tokens_actual = prettyprint(code).strip()
                corrected_content += "%s\n----\n%s" % (code, tokens_actual)
                i += 1
                
                open(f, 'w').write(corrected_content)
    
    matched_files = get_matched_files(files)
    run_tests(matched_files) if not accept else accept_tests(matched_files)
    
if __name__ == '__main__':
    import plac
    plac.call(run)