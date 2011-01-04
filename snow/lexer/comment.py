import re
# Debug
from sys import exit as e

BEGIN_WITH_HASH = re.compile(r'([ ]*)(#)')
BEGIN_NO_HASH = re.compile(r'([ ]*)([^ ].*)')

def catch_indentend_comments(code):
    prev_len = None
    lines = []
    for line in code.split("\n"):
        if prev_len != None:
            ms = BEGIN_NO_HASH.search(line)
            if ms:
                cur_len = len(ms.group(1))
                if cur_len > prev_len:
                    lines.append(" " * prev_len + "#[%d]" % cur_len + ms.group(2))
                    continue
        lines.append(line)
        ms = BEGIN_WITH_HASH.search(line)
        prev_len = None if not ms else len(ms.group(1))
    return "\n".join(lines)
