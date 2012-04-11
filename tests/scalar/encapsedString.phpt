--TEST--
Encapsed strings
--FILE--
<?php
"A";
"A->B";
"A[B]";
"A[0]";
"A[0x0]";
"A[B]";
"{A}";
"{A['B']}";
"{A}";
"{A['B']}";
"{A}";
"A B C";
b"A";
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