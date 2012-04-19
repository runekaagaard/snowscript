--TEST--
Different integer syntaxes
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

// TODO: Check up on 0787 and 0b111000111000. Make conversion better.
snowscript_to_php('
0
1
0xFFF
0xfff
0XfFf
0777
', 0);
--EXPECT--
<?php
0;
1;
4095;
4095;
4095;
511;