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
    try:
        t2.lexer = t.lexer
    except AttributeError:
        pass
    return t2


def t_error(t):
    "Error token."
    raise_syntax_error("invalid syntax", t)


def trim_beginning_newlines(token_stream):
    still_trim = True
    for t in token_stream:
        if still_trim and t.type == 'NEWLINE':
            continue
        else:
            still_trim = False

        yield t
        
def delete_multiple_newlines(token_stream):
    prev_is_nl = False
    for t in token_stream:
        is_nl = t.type == 'NEWLINE'
        
        if prev_is_nl and is_nl:
            continue
        
        prev_is_nl = is_nl
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

def mark_indentation_level(lexer, token_stream):
    lexer.indent_level = 0
    for t in token_stream:
        if t.type == 'INDENT':
            lexer.indent_level += 1
        elif t.type == 'DEDENT':
            lexer.indent_level -= 1

        yield t


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
            if not(t2.type in ("STRING_SINGLE", "STRING_DOUBLE") 
            and t2.value == ""):
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
    prev_was_class = False
    for t in token_stream:
        if t.type == 'CLASS_NAME':
            t2 = token_stream.next()
            if t2.type == 'LPAR' and not prev_was_new and not prev_was_class:
                yield build_token('NEW', 'new', t)
            yield t
            yield t2
        else:
            yield t

        prev_was_new = t.type == 'NEW'
        prev_was_class = t.type == 'CLASS'

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
            start_bracket_level = t.lexer.bracket_level
            inside_expression = True
            yield t
            yield build_token('LPAR', '(', t)

            continue

        if (inside_expression and t.type in ('INDENT', 'COLON', 'THEN')
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

def add_missing_this(token_stream):
    for t in token_stream:
        if t.type == 'DOT' and prev_t.type not in ('PHP_STRING', 'NAME', 'CLASS_NAME'):
            yield build_token("NAME", "this", t)

        yield t
        prev_t = t

def add_missing_self(token_stream):
    for t in token_stream:
        if t.type == 'DOUBLE_DOT' and prev_t.type not in (
        'PHP_STRING', 'NAME', 'CLASS_NAME') and prev_t.value != 'parent':
            yield build_token("PHP_STRING", "self", t)

        yield t
        prev_t = t

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
    token_stream = mark_indentation_level(lexer, token_stream)
    token_stream = remove_empty_concats(token_stream)
    token_stream = nuke_newlines_around_indent(token_stream)
    token_stream = insert_missing_new(token_stream)
    token_stream = correct_class_accessor_names(token_stream)
    token_stream = correct_function_call(token_stream)
    token_stream = correct_function_definition(token_stream)
    token_stream = casts_as_functioncalls(token_stream)
    token_stream = add_missing_parenthesis(token_stream)
    token_stream = add_missing_parenthesis_after_functions(token_stream)
    token_stream = delete_multiple_newlines(token_stream)
    token_stream = add_missing_this(token_stream)
    token_stream = add_missing_self(token_stream)
    #token_stream = debug(token_stream)

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
