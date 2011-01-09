from lexer.error import raise_syntax_error
from sys import exit as e

def add_to_string(t, value):
    """Adds a value to the lexers doublequoted_string variable.""" 
    try: t.lexer.doublequoted_string += value
    except AttributeError: t.lexer.doublequoted_string = value
    
def get_string_token(t):
    """Returns a string token with the value of the lexers 
    doublequoted_string variable which is then reset to ''."""
    try: t.value = t.lexer.doublequoted_string
    except AttributeError: t.value = ''
    t.type = 'STRING'
    t.lexer.doublequoted_string = ''
    return t

## Tripple doublequoted string

def t_TRIPPLE_DOUBLEQUOTED_STRING_BEGIN(t):
    r'"""'
    t.lexer.doublequoted_string = ''
    t.lexer.push_state('INTRIPPLEDOUBLEQUOTEDSTRING')

def t_INTRIPPLEDOUBLEQUOTEDSTRING_ESCAPE(t):
    r'(\\")|(\\{)|(\\)'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value[1] if len(t.value) > 1 else t.value[0])
    
def t_INTRIPPLEDOUBLEQUOTEDSTRING_STRING(t): 
        r'[^{"\\]+'
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INTRIPPLEDOUBLEQUOTEDSTRING_STRING_END(t): 
        r'"""'
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        if t.lexer.doublequoted_string:
            return get_string_token(t)
            
def t_INTRIPPLEDOUBLEQUOTEDSTRING_SINGLEQUOTE(t):
    r'"'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value)
        
def t_INTRIPPLEDOUBLEQUOTEDSTRING_SNOW_BEGIN(t): 
        r"{"
        # Going into Snow mode. Return collected strings if any.
        t.lexer.push_state('SNOWINDOUBLEQUOTEDSTRING')
        if t.lexer.doublequoted_string:
            t = get_string_token(t)
            t.type = 'STRING_WITH_CONCAT'
            return t

def t_INTRIPPLEDOUBLEQUOTEDSTRING_SNOW_END(t): 
        r"}"
        # Pop state back to INDOUBLEQUOTEDSTRING.
        t.lexer.pop_state()
        
def t_INTRIPPLEDOUBLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)
    
## Single doublequoted string    
def t_DOUBLEQUOTED_STRING_BEGIN(t):
    r'"'
    t.lexer.doublequoted_string = ''
    t.lexer.push_state('INDOUBLEQUOTEDSTRING')

def t_INDOUBLEQUOTEDSTRING_ESCAPE(t):
    r'(\\")|(\\{)|(\\)'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value[1] if len(t.value) > 1 else t.value[0])
    
def t_INDOUBLEQUOTEDSTRING_STRING(t): 
        r'[^{"\\]+'
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INDOUBLEQUOTEDSTRING_STRING_END(t): 
        r'"'
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        if t.lexer.doublequoted_string:
            return get_string_token(t)
        
def t_INDOUBLEQUOTEDSTRING_SNOW_BEGIN(t): 
        r"{"
        # Going into Snow mode. Return collected strings if any.
        t.lexer.push_state('SNOWINDOUBLEQUOTEDSTRING')
        if t.lexer.doublequoted_string:
            t = get_string_token(t)
            t.type = 'STRING_WITH_CONCAT'
            return t

def t_SNOWINDOUBLEQUOTEDSTRING_SNOW_END(t): 
        r"}"
        # Pop state back to INDOUBLEQUOTEDSTRING.
        t.lexer.pop_state()
        
def t_INDOUBLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)

## Tripple Single singlequoted string    

def t_TRIPPLE_SINGLEQUOTED_STRING_BEGIN(t):
    r"'''"
    t.lexer.doublequoted_string = ''
    t.lexer.push_state('INTRIPPLESINGLEQUOTEDSTRING')

def t_INTRIPPLESINGLEQUOTEDSTRING_ESCAPE(t):
    r"(\\')|(\\)"
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value[1] if len(t.value) > 1 else t.value[0])
    
def t_INTRIPPLESINGLEQUOTEDSTRING_STRING(t): 
        r"[^'\\]+"
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INTRIPPLESINGLEQUOTEDSTRING_STRING_END(t): 
        r"'''"
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        if t.lexer.doublequoted_string:
            return get_string_token(t)

def t_INTRIPPLESINGLEQUOTEDSTRING_SINGLEQUOTE(t):
    r"'"
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value)
        
def t_INTRIPPLESINGLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)
    
## Single singlequoted string    

def t_SINGLEQUOTED_STRING_BEGIN(t):
    r"'"
    t.lexer.doublequoted_string = ''
    t.lexer.push_state('INSINGLEQUOTEDSTRING')

def t_INSINGLEQUOTEDSTRING_ESCAPE(t):
    r"(\\')|(\\)"
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value[1] if len(t.value) > 1 else t.value[0])
    
def t_INSINGLEQUOTEDSTRING_STRING(t): 
        r"[^'\\]+"
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INSINGLEQUOTEDSTRING_STRING_END(t): 
        r"'"
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        if t.lexer.doublequoted_string:
            return get_string_token(t)
        
def t_INSINGLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)
