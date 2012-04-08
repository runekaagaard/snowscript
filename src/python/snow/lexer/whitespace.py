def t_WS(t):
    r" [ \t\f]+ "
    value = t.value

    # A formfeed character may be present at the start of the
    # line; it will be ignored for the indentation calculations
    # above. Formfeed characters occurring elsewhere in the
    # leading whitespace have an undefined effect (for instance,
    # they may reset the space count to zero).
    value = value.rsplit("\f", 1)[-1]

    # First, tabs are replaced (from left to right) by one to eight
    # spaces such that the total number of characters up to and
    # including the replacement is a multiple of eight (this is
    # intended to be the same rule as used by Unix). The total number
    # of spaces preceding the first non-blank character then
    # determines the line's indentation. Indentation cannot be split
    # over multiple physical lines using backslashes; the whitespace
    # up to the first backslash determines the indentation.
    pos = 0
    while 1:
        pos = value.find("\t")
        if pos == -1:
            break
        n = 8 - (pos % 8)
        value = value[:pos] + " "*n + value[pos+1:]

    if t.lexer.at_line_start and t.lexer.paren_count == 0:
        return t

# string continuation - ignored beyond the tokenizer level
def t_escaped_newline(t):
    r"\\\n"
    t.type = "STRING_CONTINUE"
    # Raw strings don't escape the newline
    assert not t.lexer.is_raw, "only occurs outside of quoted strings"
    t.lexer.lineno += 1

# Don't return newlines while I'm inside of ()s
def t_newline(t):
    r"\n+"
    t.lexer.lineno += len(t.value)
    t.type = "NEWLINE"
    if t.lexer.paren_count == 0:
        return t
