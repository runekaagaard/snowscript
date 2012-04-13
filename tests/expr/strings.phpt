--TEST--
Basic strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<EOT
'single1'
"single2"
'multi
   1
x'
"multi
   2
x"
'''single1'''
"""single2"""
'''multi
   1
x'''
"""multi
   2
x"""
'"'
"'"
'''""''""'''
"""''""''"""
"" 
"foo"
" foo "
"multi
line
s"
"""""" 
"""foo"""
""" foo """
"""multi
line
s"""
'foo'
' foo '
'multi
line
s'
''''''
'''foo'''
''' foo '''
'''multi
line
s'''
call_me("")
call_you("""""")
call_us('')
call_her('''''')
echo ""
a["X"]['y']
EOT
, 0);
--EXPECT--
<?php
'single1';
'single2';
'multi
   1
x';
'multi
   2
x';
'single1';
'single2';
'multi
   1
x';
'multi
   2
x';
'"';
'\'';
'""\'\'""';
'\'\'""\'\'';
'';
'foo';
' foo ';
'multi
line
s';
'';
'foo';
' foo ';
'multi
line
s';
'foo';
' foo ';
'multi
line
s';
'';
'foo';
' foo ';
'multi
line
s';
call_me('');
call_you('');
call_us('');
call_her('');
echo '';
$a['X']['y'];