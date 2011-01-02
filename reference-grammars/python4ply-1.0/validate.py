
#####
# Make sure that the grammar in python_yacc stays valid

import python_grammar
from compiler import ast

def get_definitions(f_name, docstring):
    name = None
    for line in docstring.splitlines():
        line = line.strip()
        if not line:
            continue
        if line[:1] == "|":
            yield name, line[1:].strip()
        else:
            if name is not None:
                raise AssertionError("name %r given; change to" % (name, line))
            if ":" not in line:
                raise AssertionError("missing ':' in %r %r" % (name, line))
            name, line = line.split(":", 1)
            name = name.strip()
            line = line.strip()
            yield name, line

def verify_docstrings(yacc_module_globals):
    expected_definitions = {}
    for (f_name, f) in python_grammar.__dict__.items():
        if f_name.startswith("p_"):
            if f.__doc__ is None:
                if f_name != "p_error":
                    print "Mising reference docstring %r" % (f_name,)
                continue
            for name, pattern in get_definitions(f_name, f.__doc__):
                assert (name, pattern) not in expected_definitions
                expected_definitions[(name, pattern)] = f_name

    seen = set()
    has_errors = False
    for (f_name, f) in yacc_module_globals.items():
        if f_name.startswith("p_"):
            if f.__doc__ is None:
                if f_name != "p_error":
                    print "Mising in my docstring %r" % (f_name,)
                continue
            for name, pattern in get_definitions(f_name, f.__doc__):
                assert (name, pattern) not in seen
                seen.add( (name, pattern) )
                if (name, pattern) not in expected_definitions:
                    print "Unknown! %r %r %r" % (f_name, name, pattern)
                    has_errors = True
    for k,v in expected_definitions:
        if (k,v) not in seen:
            print "Expected to see %r %r" % (k,v)
            has_errors = True
    if has_errors:
        raise AssertionError("fix errors!")


##########

# Make sure two ASTs are the same.  Mostly.
# The Python AST doesn't always put in line numbers.

import sys
class Output(object):
    def __init__(self):
        self.output = sys.stdout
        self._indentation = 0
    def __call__(self, s):
        print " "*self._indentation + s
    def indent(self):
        self._indentation += 1
    def dedent(self):
        self._indentation -= 1
    def error(self, text):
        raise AssertionError(text)

def compare_trees(prefix, py_node, my_node, output):
    assert isinstance(py_node, ast.Node), "%r and %r" % (py_node, my_node)
    output(prefix + " = " + py_node.__class__.__name__)
    if py_node.__class__ is not my_node.__class__:
        raise AssertionError("%r and %r" % (py_node.__class__,
                                            my_node.__class__))

    all_names = set(name for name in (my_node.__dict__.keys() +
                                      py_node.__dict__.keys())
                         if not name.startswith("_") and
                            not name == "span")
    all_names = sorted(all_names)
    for name in all_names:
        if name not in py_node.__dict__:
            output.error("Python tree does not have %r" % (name,))
        elif name not in my_node.__dict__:
            output.error("My tree does not have %r" % (name,))
    output.indent()
    for name in all_names:
        py_value = getattr(py_node, name)
        my_value = getattr(my_node, name)
        if (isinstance(py_value, ast.Node) and 
            isinstance(my_value, ast.Node)):
            compare_trees(name, py_value, my_value, output)
        elif isinstance(py_value, list):
            assert len(py_value) == len(my_value), (len(py_value),
                                                    len(my_value))
            output("%r is a list of %d elements" % (name, len(py_value),))
            output.indent()
            for i, (py_v, my_v) in enumerate(zip(py_value, my_value)):
                if isinstance(py_v, ast.Node):
                    compare_trees("%s[%d]"%(name, i), py_v, my_v, output)
                elif py_v == my_v:
                    output("%s[%d]: %r == %r" % (name, i, py_v, my_v))
                elif isinstance(py_v, tuple):
                    # this happens with {dict:ionaries}
                    # and with ast.Assign
                    output("Compare %d-tuple" % (len(py_v),))
                    output.indent()
                    for j, (py_v2, my_v2) in enumerate(zip(py_v, my_v)):
                        if isinstance(py_v2, ast.Node):
                            compare_trees("%s[%d][%d]" % (name, i, j),
                                          py_v2, my_v2, output)
                        elif py_v2 == my_v2:
                            output("%s[%d][%d] %r = %r " % (
                                    name, i, j, py_v2, my_v2))
                        else:
                            output.error(
                                "Value mismatch for %s[%d][%d]: %r %r" % (
                                    name, i, j, py_v2, my_v2))
                    output.dedent()
                else:
                    output.error("%s[%d] %r != %r" % (name, i, py_v, my_v))

            output.dedent()
                
        else:
            if name == "lineno" and my_value is None:
                output.error("I don't have a line number! %r" % (my_node,))
            if name == "lineno" and py_value is None:
                output("lineno = %r (Python has None)" % (my_value,))
            elif py_value != my_value:
                output.error("Value mismatch for %r: py=%r my=%r" %
                             (name, py_value, my_value))
            else:
                output("%s %r == %r" % (name, py_value, my_value))
    output.dedent()

####
# Some test code to check that my AST agrees (mostly) with the
# compiler module.

def cross_check():
    import python_yacc
    import python_lex
    python_lex.BACKWARDS_COMPATIBLE = True
    python_yacc.BACKWARDS_COMPATIBLE = True

    text = """
a | 1 ^ 9
b ^ 2 & 8
c & 3 | 7
d << 4 >> 3
e >> 5 << -1
f+g*9
h*i**10
j/k//3
m%n+1
o//p+4

"""
    #text = open("/usr/local/lib/python2.6/decimal.py").read()
    text = open("sample.py").read()
    my_output = None
    try:
        import time
        t1 = time.time()
        my_tree = python_yacc.parse(text)
        t2 = time.time()
        print "Parse time", t2-t1
        my_output = str(my_tree)
    except NotImplementedError, x:
        import traceback
        traceback.print_exc()
    if my_output is None:
        raise SystemExit()

    import compiler
    from compiler import misc
    py_tree = compiler.parse(text)
    misc.set_filename(my_tree.filename, py_tree)
    py_output = str(py_tree)
    if my_output != py_output:
        print "*" * 60
        for i in range(len(py_output)):
            if py_output[i] != my_output[i]:
                print "Bad at", i
                print "my", repr(my_output[max(0, i-30):i]), repr(my_output[i:i+40])
                print "py", repr(py_output[max(0, i-30):i]), repr(py_output[i:i+40])
                raise SystemExit("fix it")
                break
    compare_trees("root", py_tree, my_tree, Output())
