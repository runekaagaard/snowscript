"""
Helper functions for raising errors.
"""


def _raise_error(message, t, klass):
    """
    Raise a given lexing or parsing error.
    """
    lineno, lexpos, lexer = t.lineno, t.lexpos, t.lexer
    filename = lexer.filename
    # Switch from 1-based 0-based linenumber.
    geek_lineno = lineno - 1
    start_of_line = lexer.line_offsets[geek_lineno]
    end_of_line = lexer.line_offsets[geek_lineno + 1] - 1
    text = lexer.lexdata[start_of_line:end_of_line]
    offset = lexpos - start_of_line
    # Use offset+1 because the exception is 1-based.
    raise klass(message, (filename, lineno, offset + 1, text))


def raise_syntax_error(message, t):
    _raise_error(message, t, SyntaxError)


def raise_indentation_error(message, t):
    _raise_error(message, t, IndentationError)
