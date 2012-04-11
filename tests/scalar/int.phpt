--TEST--
Different integer syntaxes
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
0
1
@@{ PHP_INT_MAX     }@@
@@{ PHP_INT_MAX + 1 }@@
0xFFF
0xfff
0XfFf
0777
0787
0b111000111000
', 0);
--EXPECT--
<?php
0;
1;
@@{ PHP_INT_MAX     }@@;
@@{ PHP_INT_MAX + 1 }@@;
0xFFF;
0xfff;
0XfFf;
0777;
0787;
0b111000111000;