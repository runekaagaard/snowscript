--TEST--
If/Elseif/Else
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
echo a ? b : c
', 0);
--EXPECT--
<?php
echo $a ? $b : $c;
