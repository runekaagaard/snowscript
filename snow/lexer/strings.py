from lexer.error import raise_syntax_error

def add_to_string(t, value):
    """Adds a value to the lexers string_content variable.""" 
    t.lexer.string_content += value

def set_been_concat(t, type):
    """
    Set the string_been_concat global which is used to decide if we want to
    allow empty strings to return token. In general we want to do that, but not
    if the string comes after a STRING_WITH_CONCAT type.
    """
    if type == 'STRING_WITH_CONCAT':
        t.lexer.string_been_concat = True
    elif t.lexer.current_state() == 'INITIAL':
        t.lexer.string_been_concat = False
        
def get_string_token(t, type='STRING'):
    """Returns a string token with the value of the lexers 
    string_content variable which is then reset to ''."""
    set_been_concat(t, type)
    t.value = t.lexer.string_content
    t.lexer.string_content = ''
    t.type = type
    return t

def snow_begin(t):
    # Going into Snow mode. Return collected strings if any.
    t.lexer.push_state('SNOWINANYDOUBLEQUOTEDSTRING')
    if t.lexer.string_content:
        return get_string_token(t, type='STRING_WITH_CONCAT')

def string_begin(t, to_state):
    t.lexer.string_content = ''
    t.lexer.push_state(to_state)
    t.lexer.starting_string_token = t
    
def string_end(t, from_state):
    t.lexpos = t.lexer.starting_string_token.lexpos
    t.lineno = t.lexer.starting_string_token.lineno
    allow_empty = t.lexer.current_state() == from_state \
                  and not t.lexer.string_been_concat
    t.lexer.pop_state()
    if allow_empty or t.lexer.string_content:
        return get_string_token(t)

def add_escape(t):
    add_to_string(t, t.value[1] if len(t.value) > 1 else t.value[0])
    
## Tripple doublequoted string

def t_TRIPPLE_string_string_begin(t):
    r'"""'
    string_begin(t, 'INTRIPPLEDOUBLEQUOTEDSTRING')

def t_INTRIPPLEDOUBLEQUOTEDSTRING_ESCAPE(t):
    r'(\\")|(\\{)|(\\)'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_escape(t)
    
def t_INTRIPPLEDOUBLEQUOTEDSTRING_STRING(t): 
    r'[^{"\\]+'
    # All that are normal string chars.
    add_to_string(t, t.value)
 
def t_INTRIPPLEDOUBLEQUOTEDSTRING_STRING_END(t): 
        r'"""'
        return string_end(t, 'INTRIPPLEDOUBLEQUOTEDSTRING')
            
def t_INTRIPPLEDOUBLEQUOTEDSTRING_SINGLEQUOTE(t):
    r'"'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value)
        
def t_INTRIPPLEDOUBLEQUOTEDSTRING_SNOW_BEGIN(t): 
    r"{"
    return snow_begin(t)
        
def t_INTRIPPLEDOUBLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)
    
## Single doublequoted string    
def t_string_string_begin(t):
    r'"'
    string_begin(t, 'INDOUBLEQUOTEDSTRING')

def t_INDOUBLEQUOTEDSTRING_ESCAPE(t):
    r'(\\")|(\\{)|(\\)'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_escape(t)
    
def t_INDOUBLEQUOTEDSTRING_STRING(t): 
        r'[^{"\\]+'
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INDOUBLEQUOTEDSTRING_STRING_END(t): 
        r'"'
        return string_end(t, 'INDOUBLEQUOTEDSTRING')
        
def t_INDOUBLEQUOTEDSTRING_SNOW_BEGIN(t): 
        r"{"
        return snow_begin(t)
        
def t_INDOUBLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)

## Tripple Single singlequoted string    

def t_TRIPPLE_SINGLEQUOTED_STRING_BEGIN(t):
    r"'''"
    string_begin(t, 'INTRIPPLESINGLEQUOTEDSTRING')

def t_INTRIPPLESINGLEQUOTEDSTRING_ESCAPE(t):
    r"(\\')|(\\)"
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_escape(t)
    
def t_INTRIPPLESINGLEQUOTEDSTRING_STRING(t): 
        r"[^'\\]+"
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INTRIPPLESINGLEQUOTEDSTRING_STRING_END(t): 
        r"'''"
        return string_end(t, 'INTRIPPLESINGLEQUOTEDSTRING')

def t_INTRIPPLESINGLEQUOTEDSTRING_SINGLEQUOTE(t):
    r"'"
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value)
        
def t_INTRIPPLESINGLEQUOTEDSTRING_error(t):
    raise_syntax_error("invalid syntax", t)
    
## Single singlequoted string    

def t_SINGLEQUOTED_STRING_BEGIN(t):
    r"'"
    string_begin(t, 'INSINGLEQUOTEDSTRING')

def t_INSINGLEQUOTEDSTRING_ESCAPE(t):
    r"(\\')|(\\)"
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_escape(t)
    
def t_INSINGLEQUOTEDSTRING_STRING(t): 
        r"[^'\\]+"
        # All that are normal string chars.
        add_to_string(t, t.value)
 
def t_INSINGLEQUOTEDSTRING_STRING_END(t): 
        r"'"
        return string_end(t, 'INSINGLEQUOTEDSTRING')
        
def t_INSINGLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)

## Snow end

def t_SNOWINANYDOUBLEQUOTEDSTRING_SNOW_END(t): 
        r"}"
        t.lexer.starting_string_token = t
        # Pop state back to INDOUBLEQUOTEDSTRING.
        t.lexer.pop_state()