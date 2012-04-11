--TEST--
Encapsed strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
"A"
"A->B"
"A[B]"
"A[0]"
"A[0x0]"
"A[B]"
"{A}"
"{A[\'B\']}"
"{A}"
"{A[\'B\']}"
"{A}"
"A B C"
b"A"
', 0);
--EXPECT--
<?php
"$A";
"$A->B";
"$A[B]";
"$A[0]";
"$A[0x0]";
"$A[$B]";
"{$A}";
"{$A['B']}";
"${A}";
"${A['B']}";
"${$A}";
"A $B C";
b"$A";