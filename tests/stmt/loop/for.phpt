--TEST--
For loop
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
for (i = 0 i < c ++i) {}
for (a,b) {}
for () {}
for ():
endfor
', 0);
--EXPECT--
<?php
for ($i = 0; $i < $c; ++$i) {}
for (;$a,$b;) {}
for (;;) {}
for (;;):
endfor;