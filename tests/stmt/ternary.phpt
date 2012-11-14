--TEST--
If/Elseif/Else
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
echo if a then b else c
[if a then b else c, if d then e else f]
', 0);
--EXPECT--
<?php
echo $a ? $b : $c;
array($a ? $b : $c, $d ? $e : $f);
