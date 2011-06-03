from lexer.error import raise_syntax_error
from sys import exit as e

def add_to_string(t, value):
    """Adds a value to the lexers string_content variable."""
    try: t.lexer.string_content += value
    except AttributeError: t.lexer.string_content = value
    
def get_string_token(t):
    """Returns a string token with the value of the lexers 
    string_content variable which is then reset to ''."""
    #print t.lexer.current_state()
    #if t.lexer.current_state() == 'SNOWINANYDOUBLEQUOTEDSTRING' and t.value == '':
    #    print "NNNOOOOOT"
    #    t.lexer.string_content = ''
    #    return
    #if t.lexer.string_content == '':
    #    print t.lexer.current_state()
    #    return
    #print t.value, t.lexer.current_state()
    
    if t.lexer.string_content == '' and t.lexer.current_state() == 'SNOWINANYDOUBLEQUOTEDSTRING':
        if t.lexer.string_content != '':
            t.lexer.string_is_part_of_concat = True
            return
            print "YEAH"
    if t.lexer.current_state() == 'INITIAL':
        try:
            if t.lexer.string_is_part_of_concat:
                t.lexer.string_is_part_of_concat == False
                if t.value == '':
                    return
        except AttributeError:
            t.lexer.string_is_part_of_concat = False
    try: t.value = t.lexer.string_content
    except AttributeError: t.value = ''
    t.type = 'STRING'
    t.lexer.string_content = ''
    return t

## Tripple doublequoted string

def t_TRIPPLE_string_content_BEGIN(t):
    r'"""'
    t.lexer.tdq_t_before_str = t
    t.lexer.string_content = ''
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
        # Set lexer pos and linenumber to what it was when the string began.
        t.lexpos = t.lexer.tdq_t_before_str.lexpos
        t.lineno = t.lexer.tdq_t_before_str.lineno
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        return get_string_token(t)
            
def t_INTRIPPLEDOUBLEQUOTEDSTRING_SINGLEQUOTE(t):
    r'"'
    # Matches an escaped " or { or a single \ as these should count as a normal 
    # string char. 
    add_to_string(t, t.value)
        
def t_INTRIPPLEDOUBLEQUOTEDSTRING_SNOW_BEGIN(t): 
        r"{"
        # Going into Snow mode. Return collected strings if any.
        t.lexer.push_state('SNOWINANYDOUBLEQUOTEDSTRING')
        t = get_string_token(t)
        if t is not None:
            t.type = 'STRING_WITH_CONCAT'
            return t
        
def t_INTRIPPLEDOUBLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)
    
## Single doublequoted string    
def t_string_content_BEGIN(t):
    r'"'
    t.lexer.dq_t_before_str = t
    t.lexer.string_content = ''
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
        # Set lexer pos and linenumber to what it was when the string began.
        t.lexpos = t.lexer.dq_t_before_str.lexpos
        t.lineno = t.lexer.dq_t_before_str.lineno
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        return get_string_token(t)
        
def t_INDOUBLEQUOTEDSTRING_SNOW_BEGIN(t): 
        r"{"
        # Going into Snow mode. Return collected strings if any.
        t.lexer.push_state('SNOWINANYDOUBLEQUOTEDSTRING')
        t = get_string_token(t)
        if t is not None:
            t.type = 'STRING_WITH_CONCAT'
            return t

def t_SNOWINANYDOUBLEQUOTEDSTRING_SNOW_END(t): 
        r"}"
        # Used by both double and tripple quoted strings.
        t.lexer.dq_t_before_str = t
        t.lexer.tdq_t_before_str = t
        # Pop state back to INDOUBLEQUOTEDSTRING.
        t.lexer.pop_state()
        
def t_INDOUBLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)

## Tripple Single singlequoted string    

def t_TRIPPLE_SINGLEQUOTED_STRING_BEGIN(t):
    r"'''"
    t.lexer.tsq_t_before_str = t
    t.lexer.string_content = ''
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
        # Set lexer pos and linenumber to what it was when the string began.
        t.lexpos = t.lexer.tsq_t_before_str.lexpos
        t.lineno = t.lexer.tsq_t_before_str.lineno
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        return get_string_token(t)

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
    t.lexer.sq_t_before_str = t
    t.lexer.string_content = ''
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
        # Set lexer pos and linenumber to what it was when the string began.
        t.lexpos = t.lexer.sq_t_before_str.lexpos
        t.lineno = t.lexer.sq_t_before_str.lineno
        # Pop state back to initial. Return collected strings if any.
        t.lexer.pop_state()
        return get_string_token(t)
        
def t_INSINGLEQUOTEDSTRING_error(t):
    print t
    raise_syntax_error("invalid syntax", t)
