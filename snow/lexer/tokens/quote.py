from lexer.error import raise_syntax_error

# This is a q1: '
# This is a q2: "
# These are single quoted strings:  'this' "and" r"that"
# These are triple quoted strings:  """one""" '''two''' U'''three'''

error_message = {
    "STRING_START_TRIPLE": "EOF while scanning triple-quoted string",
    "STRING_START_SINGLE": "EOL while scanning single-quoted string",
}

# Handle "\" escapes
def t_SINGLEQ1_SINGLEQ2_TRIPLEQ1_TRIPLEQ2_escaped(t):
    r"\\(.|\n)"
    t.type = "STRING_CONTINUE"
    t.lexer.lineno += t.value.count("\n")
    return t

##### Triple Q1

def t_start_triple_quoted_q1_string(t):
    r"'''"
    t.lexer.push_state("TRIPLEQ1")
    t.type = "STRING_START_TRIPLE"
    if "r" in t.value or "R" in t.value:
        t.lexer.is_raw = True
    t.value = t.value.split("'", 1)[0]
    return t

def t_TRIPLEQ1_simple(t):
    r"[^'\\]+"
    t.type = "STRING_CONTINUE"
    t.lexer.lineno += t.value.count("\n")
    return t

def t_TRIPLEQ1_q1_but_not_triple(t):
    r"'(?!'')"
    t.type = "STRING_CONTINUE"
    return t

def t_TRIPLEQ1_end(t):
    r"'''"
    t.type = "STRING_END"
    t.lexer.pop_state()
    t.lexer.is_raw = False
    return t


def t_start_triple_quoted_q2_string(t):
    r'"""'
    t.lexer.push_state("TRIPLEQ2")
    t.type = "STRING_START_TRIPLE"
    if "r" in t.value or "R" in t.value:
        t.lexer.is_raw = True
    t.value = t.value.split('"', 1)[0]
    return t

def t_TRIPLEQ2_simple(t):
    r'[^"\\]+'
    t.type = "STRING_CONTINUE"
    t.lexer.lineno += t.value.count("\n")
    return t

def t_TRIPLEQ2_q2_but_not_triple(t):
    r'"(?!"")'
    t.type = "STRING_CONTINUE"
    return t

def t_TRIPLEQ2_end(t):
    r'"""'
    t.type = "STRING_END"
    t.lexer.pop_state()
    t.lexer.is_raw = False
    return t

t_TRIPLEQ1_ignore = ""  # supress PLY warning
t_TRIPLEQ2_ignore = ""  # supress PLY warning

def t_TRIPLEQ1_error(t):
    raise_syntax_error()

def t_TRIPLEQ2_error(t):
    raise_syntax_error()

##### Single quoted strings

def t_start_single_quoted_q1_string(t):
    r"'"
    t.lexer.push_state("SINGLEQ1")
    t.type = "STRING_START_SINGLE"
    if "r" in t.value or "R" in t.value:
        t.lexer.is_raw = True
    t.value = t.value.split("'", 1)[0]
    #print "single_q1", t.value
    return t

def t_SINGLEQ1_simple(t):
    r"[^'\\]+"
    t.type = "STRING_CONTINUE"
    return t

def t_SINGLEQ1_end(t):
    r"'"
    t.type = "STRING_END"
    t.lexer.pop_state()
    t.lexer.is_raw = False
    return t

def t_start_single_quoted_q2_string(t):
    r'"'
    t.lexer.push_state("SINGLEQ2")
    t.type = "STRING_START_SINGLE"
    if "r" in t.value or "R" in t.value:
        t.lexer.is_raw = True
    t.value = t.value.split('"', 1)[0]
    #print "single_q2", repr(t.value)
    return t

def t_SINGLEQ2_BRACKET_BEGIN_IN_STRING(t):
    r'\{'
    t.lexer.push_state('BRACKETINSTRING')
    return t    

def t_BRACKETINSTRING_META_STRING_IN_STRING_Q2(t):
    r'\\"([^"]+)\\"'
    t.value = t.lexer.lexmatch.group(2)
    return t
    
def t_BRACKETINSTRING_BRACKET_END_IN_STRING(t):
    r'\}'
    t.lexer.pop_state()
    return t


    
def t_SINGLEQ2_simple(t):
    r'[^"\\\{]+'
    t.type = "STRING_CONTINUE"
    return t

def t_SINGLEQ2_end(t):
    r'"'
    t.type = "STRING_END"
    t.lexer.pop_state()
    t.lexer.is_raw = False
    return t

t_SINGLEQ1_ignore = ""  # supress PLY warning
t_SINGLEQ2_ignore = ""  # supress PLY warning

def t_SINGLEQ1_error(t):
    raise_syntax_error("EOF while scanning single quoted string", t)

def t_SINGLEQ2_error(t):
    raise_syntax_error("EOF while scanning single quoted string", t)
