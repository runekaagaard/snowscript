from lexer.error import raise_syntax_error

error_message = {
    "STRING_START_TRIPLE": "EOF while scanning triple-quoted string",
    "STRING_START_SINGLE": "EOL while scanning single-quoted string",
}

def _parse_quoted_string(start_tok, string_toks):
    """Pythonic strings like r"" are not supported in Snow."""
    s = "".join(tok.value for tok in string_toks)
    quote_type = start_tok.value.lower()
    if quote_type == "":
        return s.decode("string_escape")
    else:
        raise AssertionError("Unknown string quote type: %r" % (quote_type,))

def create_strings(lexer, token_stream):
    for tok in token_stream:
        if not tok.type.startswith("STRING_START_"):
            yield tok
            continue
        # This is a string start; process until string end
        start_tok = tok
        string_toks = []
        for tok in token_stream:
            #print " Merge string", tok
            if tok.type == "STRING_END":
                break
            else:
                assert tok.type == "STRING_CONTINUE", tok.type
                string_toks.append(tok)
        else:
            # Reached end of input without string termination
            # This reports the start of the line causing the problem.
            # Python reports the end.  I like mine better.
            raise_syntax_error(error_message[start_tok.type], start_tok)
        # Reached the end of the string
        if "SINGLE" in start_tok.type:
            # The compiler module uses the end of the single quoted
            # string to determine the strings line number.  I prefer
            # the start of the string.
            start_tok.lineno = tok.lineno
        start_tok.type = "STRING"
        start_tok.value = _parse_quoted_string(start_tok, string_toks)
        yield start_tok
