"""
This file holds a series of transformations of the lexer tokens.
"""

from ply import lex
import re
from error import raise_indentation_error, raise_syntax_error
from tokens import MISSING_PARENTHESIS, CASTS


def build_token(_type, value, t):
    t2 = lex.LexToken()
    t2.type = _type
    t2.value = value
    t2.lineno = t.lineno
    t2.lexpos = -1
    t2.lexer = t.lexer
    return t2


def t_error(t):
    "Error token."
    raise_syntax_error("invalid syntax", t)

##### Keep track of indentation state

# I implemented INDENT / DEDENT generation as a post-processing filter

# The original lex token stream contains WS and NEWLINE characters.
# WS will only occur before any other tokens on a line.

# I have three filters.  One tags tokens by adding two attributes.
# "must_indent" is True if the token must be indented from the
# previous code.  The other is "at_line_start" which is True for WS
# and the first non-WS/non-NEWLINE on a line.  It flags the check so
# see if the new line has changed indication level.

# Python's syntax has three INDENT states
#  0) no colon hence no need to indent
#  1) "if 1: go()" - simple statements have a COLON but no need for an indent
#  2) "if 1:\n  go()" - complex statements have a COLON NEWLINE and must indent
NO_INDENT = 0
MAY_INDENT = 1
MUST_INDENT = 2


def trim_beginning_newlines(token_stream):
    still_trim = True
    for t in token_stream:
        if still_trim and t.type == 'NEWLINE':
            continue
        else:
            still_trim = False

        yield t


def inject_case_tokens(token_stream):
    inside_switch = False
    case_indent = 0
    for t in token_stream:
        yield t

        if inside_switch:
            if t.type == 'NEWLINE':
                t2 = token_stream.next()
                yield t2

                if t2.type == 'WS':
                    indent = len(t2.value)
                    if case_indent == 0:
                        case_indent = indent
                        yield build_token('CASE', 'case', t2)
                    else:
                        if indent == case_indent:
                            yield build_token('CASE', 'case', t2)
                        elif indent < case_indent:
                            inside_switch = False
                            case_indent = 0
                elif t2.type == "SWITCH":
                    case_indent = 0
                else:
                    inside_switch = False
                    case_indent = 0

        if t.type == "SWITCH":
            inside_switch = True
            case_indent = 0

INDENT_ERROR = "Dedention matches no previous level."


def inject_indent_tokens(lexer, token_stream):
    levels = [0]
    try:
        for t in token_stream:
            lexer.at_line_start = False
            if t.type == "NEWLINE":
                yield t
                lexer.at_line_start = True

                t2 = token_stream.next()
                level = len(t2.value) if t2.type == 'WS' else 0

                if level > levels[-1]:
                    levels.append(level)
                    yield build_token('INDENT', '', t2)
                elif level < levels[-1]:
                    if level not in levels:
                        raise_indentation_error(INDENT_ERROR, t2)
                    while levels.pop() > level:
                        yield build_token('DEDENT', '', t2)
                    levels.append(level)
                    if levels == []:
                        levels = [0]
                if t2.type != 'WS':
                    yield t2

            elif t.type == "WS":
                continue
            else:
                yield t
    except StopIteration:
        for level in range(0, len(levels) - 1):
            yield build_token('DEDENT', '', t)


def add_endmarker(token_stream):
    for t in token_stream:
        yield t
    yield build_token("ENDMARKER", None, t)
_add_endmarker = add_endmarker


def remove_empty_concats(token_stream):
    for t in token_stream:
        if t.type == "STRING_WITH_CONCAT" and t.value == "":
            continue

        if t.type == "PERCENT":
            try:
                t2 = token_stream.next()
            except StopIteration:
                yield t
                raise StopIteration
            if not(t2.type == "STRING" and t2.value == ""):
                yield t
                yield t2
        else:
            yield t


def nuke_newlines_around_indent(token_stream):
    for t in token_stream:
        if t.type == 'NEWLINE':
            try:
                t2 = token_stream.next()
            except StopIteration:
                yield t
                raise StopIteration

            if t2.type in ('INDENT', 'PASS'):
                yield t2
            else:
                yield t
                yield t2
        elif t.type in ('INDENT', 'PASS'):
            try:
                t2 = token_stream.next()
            except StopIteration:
                yield t
                raise StopIteration
            if t2.type == 'NEWLINE':
                yield t
            else:
                yield t
                yield t2
        else:
            yield t


def insert_missing_new(token_stream):
    prev_was_new = False
    for t in token_stream:
        if t.type == 'CLASS_NAME':
            t2 = token_stream.next()
            if t2.type == 'LPAR' and not prev_was_new:
                yield build_token('NEW', 'new', t)
            yield t
            yield t2
        else:
            yield t

        prev_was_new = t.type == 'NEW'


def correct_class_accessor_names(token_stream):
    for t in token_stream:
        if t.type == 'DOT':
            t2 = token_stream.next()
            if t2.type == 'NAME':
                t2.type = 'PHP_STRING'
            yield t
            yield t2
        else:
            yield t


def correct_function_call(token_stream):
    for t in token_stream:
        if t.type in ('NAME'):
            yield t
            t2 = token_stream.next()
            if t2.type == 'LPAR':
                t.type = 'PHP_STRING'
            yield t2
        else:
            yield t


def correct_function_definition(token_stream):
    for t in token_stream:
        if t.type == 'FN':
            yield t
            t2 = token_stream.next()
            if t2.type == 'NAME':
                t2.type = 'PHP_STRING'
            yield t2
        else:
            yield t


def casts_as_functioncalls(token_stream):
    remove_at_level = None
    for t in token_stream:
        if t.type in CASTS:
            t2 = token_stream.next()
            if t2.type == 'LPAR':
                remove_at_level = t2.lexer.bracket_level - 1
                yield build_token('%s_CAST' % t.type, '(int)', t)
            else:
                yield t
                yield t2
        elif t.type == 'RPAR' and t.lexer.bracket_level == remove_at_level:
            remove_at_level = None
        else:
            yield t


def add_missing_parenthesis(token_stream):
    inside_expression = False
    for t in token_stream:
        if hasattr(t, 'lexer'):
            bracket_level = t.lexer.bracket_level
        else:
            bracket_level = 0
        if not inside_expression and t.type in MISSING_PARENTHESIS:
            start_bracket_level = 0
            inside_expression = True
            yield t
            yield build_token('LPAR', '(', t)
            continue

        if (inside_expression and t.type in ('INDENT', 'COLON')
        and bracket_level == start_bracket_level):
            inside_expression = False
            yield build_token('RPAR', ')', t)

        yield t


def add_missing_parenthesis_after_functions(token_stream):
    for t in token_stream:
        yield t
        if t.type == 'FN':
            t1 = token_stream.next()
            yield t1
            if t1.type == 'PHP_STRING':
                t2 = token_stream.next()
                if t2.type in ('INDENT', 'COLON'):
                    yield build_token('LPAR', '(', t2)
                    yield build_token('RPAR', ')', t2)
                yield t2


def debug(token_stream):
    print
    for t in token_stream:
        print t
        yield t


def make_token_stream(lexer, add_endmarker=True):
    token_stream = iter(lexer.token, None)
    token_stream = trim_beginning_newlines(token_stream)
    token_stream = inject_case_tokens(token_stream)
    token_stream = inject_indent_tokens(lexer, token_stream)
    token_stream = remove_empty_concats(token_stream)
    token_stream = nuke_newlines_around_indent(token_stream)
    token_stream = insert_missing_new(token_stream)
    token_stream = correct_class_accessor_names(token_stream)
    token_stream = correct_function_call(token_stream)
    token_stream = correct_function_definition(token_stream)
    token_stream = casts_as_functioncalls(token_stream)
    token_stream = add_missing_parenthesis(token_stream)
    token_stream = add_missing_parenthesis_after_functions(token_stream)
    # token_stream = debug(token_stream)

    if add_endmarker:
        token_stream = _add_endmarker(token_stream)
    return token_stream

_newline_pattern = re.compile(r"\n")


def get_line_offsets(text):
    offsets = [0]
    for m in _newline_pattern.finditer(text):
        offsets.append(m.end())
    # This is only really needed if the input does not end with a newline
    offsets.append(len(text))
    return offsets
