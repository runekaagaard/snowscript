--TEST--
Advanced strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<EOT
'fo \ \{ \' x'
'''fo \ \{ \' x'''
a = "aaa 'bbb' {foo(bar)} ddd"
a = "foo {bar("ufel")} sup"
mystring = "I am {A_CONSTANT} with {a_string} and {func_tion("foo")}"
"fo \ \{ \""
"foo {bar("baz")}"
"""fo \ \{ \""""
"""foo {bar("baz")}"""
EOT
, 0);
--EXPECT--
<?php
'fo \ \{ \' x';
'fo \ \{ \' x';
$a = ('aaa \'bbb\' ' . foo($bar)) . ' ddd';
$a = ('foo ' . bar('ufel')) . ' sup';
$mystring = (((('I am ' . A_CONSTANT) . ' with ') . $a_string) . ' and ') . func_tion('foo');
'fo \ { "';
'foo ' . $bar('baz');
'fo \ { "';
'foo ' . $bar('baz');