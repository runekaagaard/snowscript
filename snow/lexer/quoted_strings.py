from lexer.error import raise_syntax_error

error_message = {
    "STRING_START_TRIPLE": "EOF while scanning triple-quoted string",
    "STRING_START_SINGLE": "EOL while scanning single-quoted string",
}
    
def create_strings(lexer, token_stream):
    def collected_strings(start_tok, string_toks):
        start_tok.type = "STRING"
        start_tok.value = "".join(tok.value for tok in string_toks).decode("string_escape")
        return start_tok if start_tok.value else None
        
    for tok in token_stream:
        print tok
        if not tok.type in ('STRING_START_SINGLE',):
            yield tok
            continue
            
        # This is a string start; process until string end
        start_tok = tok
        string_toks = []
        state = 'string'
        for tok in token_stream:
            print tok
            if tok.type == "STRING_END":
                if string_toks:
                    yield collected_strings(start_tok, string_toks)
                    string_toks = []
                break
            elif state == 'snow' or tok.type == "BRACKET_BEGIN_IN_STRING":
                state = 'snow'
                if string_toks:
                    yield collected_strings(start_tok, string_toks)
                    string_toks = []
                if (tok.type.startswith('STRING_') or 
                tok.type == 'META_STRING_IN_STRING_Q2'):
                    tok.type = 'STRING'
                yield tok
            elif state == 'string' or tok.type == "BRACKET_END_IN_STRING":
                state == 'string'
                string_toks.append(tok)
            else:
                raise_syntax_error('TODO: Write this message.')
        else:
            # Reached end of input without string or "}" termination
            raise_syntax_error(error_message[start_tok.type], start_tok)
