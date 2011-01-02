#!/usr/bin/env python
# Generate byte code.  Also has a command-line interface.

import os
import time
import imp
import struct
import marshal
import types
import sys
from compiler import pycodegen, ast
import python_yacc
from optparse import OptionParser

# Ahh, the joys of the compiler module and not-well-documented code
MAGIC = imp.get_magic()

def codegen(tree):
    gen = pycodegen.ModuleCodeGenerator(tree)
    code = gen.getCode()
    return code

def save_bytecode(code, outfile, timestamp=None):
    if isinstance(outfile, basestring):
        outfile = open(outfile, "wb")
    if timestamp is None:
        timestamp = time.time()
    outfile.write(MAGIC + struct.pack("<i", timestamp))
    marshal.dump(code, outfile)

def compile_file(source_filename, pyc_filename=None):
    if pyc_filename is None:
        pyc_filename = source_filename + "c"

    text = open(source_filename).read()
    mtime = os.path.getmtime(source_filename)
    tree = python_yacc.parse(text, source_filename)
    code = codegen(tree)
    
    save_bytecode(code, pyc_filename, mtime)

def execfile(source_filename):
    text = open(source_filename).read()
    tree = python_yacc.parse(text, source_filename)
    code = codegen(tree)
    mod = types.ModuleType("__main__", tree.doc)
    setattr(mod, "__file__", source_filename)
    sys.modules["__main__"] = mod
    exec code in mod.__dict__

opt_parser = OptionParser()
opt_parser.add_option(
    "-c", "--compile", action="store_true", dest="compile",
    help="byte-compile the list of files")
opt_parser.add_option(
    "-e", "--execfile", action="store_true", dest="execfile",
    help="exec the first name; remaining arguments put into sys.argv")

if __name__ == "__main__":
    options, args = opt_parser.parse_args()
    if not options.compile and not options.execfile:
        options.compile = True

    if options.compile:
        if options.execfile:
            opt_parser.error("options -c and -e cannot be used together")
        for filename in args:
            if os.path.splitext(filename)[1] != ".py":
                print "Skipping %r - not a Python file" % (filename,)
            else:
                print "Compiling %r" % (filename,)
                compile_file(filename)
    else:
        if not args:
            opt_parser.error("No filename specified")
        sys.argv[:] = args
        execfile(args[0])
