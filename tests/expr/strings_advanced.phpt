--TEST--
Advanced strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<EOT
mystring = "I am {A_CONSTANT} with {a_string} and {func_tion("foo")}"
'fo \ \{ \' x'
'''fo \ \{ \' x'''
"fo \ \{ \""
"foo {bar("baz")}"
"""fo \ \{ \""""
"""foo {bar("baz")}"""
'fo \ \{ \' x'
a = "aaa 'bbb' {foo(bar)} ddd"
a = "foo {bar("ufel")} sup"
"\n"
'\n'
EOT
, 0);
--EXPECT--
<?php
$mystring = (((('I am ' . A_CONSTANT) . ' with ') . $a_string) . ' and ') . func_tion('foo');
'fo \\ \\{ \' x';
'fo \\ \\{ \' x';
'fo \\ { "';
'foo ' . bar('baz');
'fo \\ { "';
'foo ' . bar('baz');
'fo \\ \\{ \' x';
$a = ('aaa \'bbb\' ' . foo($bar)) . ' ddd';
$a = ('foo ' . bar('ufel')) . ' sup';
"\n";
'\n';