--TEST--
While loop
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
while (a) {}
while (a):
endwhile
', 0);
--EXPECT--
<?php
while ($a) {}
while ($a):
endwhile;