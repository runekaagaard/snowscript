--TEST--
Constant string syntaxes
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
\'\'
""
b\'\'
b""
\'Hi\'
b\'Hi\'
"Hi"
b"Hi"
\'!\\'!\\!\a!\'
"!\"!\\!\!\n!\r!\t!\f!\v!\e!\a"
"!\xFF!\377!\400!\0!"
', 0);
--EXPECT--
<?php
'';
"";
b'';
b"";
'Hi';
b'Hi';
"Hi";
b"Hi";
'!\'!\\!\a!';
"!\"!\\!\$!\n!\r!\t!\f!\v!\e!\a";
"!\xFF!\377!\400!\0!";