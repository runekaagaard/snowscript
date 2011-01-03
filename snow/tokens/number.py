import tokenize
from ply import lex

# The NUMBER tokens return a 2-ple of (value, original string)

# The original string can be used to get the span of the original
# token and to provide better round-tripping.

# imaginary numbers in Python are represented with floats,
#   (1j).imag is represented the same as (1.0j).imag -- with a float
@lex.TOKEN(tokenize.Imagnumber)
def t_IMAG_NUMBER(t):
    t.type = "NUMBER"
    t.value = (float(t.value[:-1])* 1j, t.value)
    return t

# Then check for floats (must have a ".")

@lex.TOKEN(tokenize.Floatnumber)
def t_FLOAT_NUMBER(t):
    t.type = "NUMBER"
    t.value = (float(t.value), t.value)
    return t

# In the following I use 'long' to make the actual type match the
# results from the compiler module.  Otherwise there's no need for it.

# Python allows "0x", but in reading python-dev it looks like this was
# removed in 2.6/3.0.  I don't allow it.
def t_HEX_NUMBER(t):
    r"0[xX][0-9a-fA-F]+[lL]?"
    t.type = "NUMBER"
    value = t.value
    if value[-1] in "lL":
        value = value[:-1]
        f = long
    else:
        f = int
    t.value = (f(value, 16), t.value)
    return t

def t_OCT_NUMBER(t):
    r"0[0-7]*[lL]?"
    t.type = "NUMBER"
    value = t.value
    if value[-1] in "lL":
        value = value[:-1]
        f = long
    else:
        f = int
    t.value = (f(value, 8), t.value)
    return t

def t_DEC_NUMBER(t):
    r"[1-9][0-9]*[lL]?"
    t.type = "NUMBER"
    value = t.value
    if value[-1] in "lL":
        value = value[:-1]
        f = long
    else:
        f = int
    t.value = (f(value, 10), t.value)
    return t

