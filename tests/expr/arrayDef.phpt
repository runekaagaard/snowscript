--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

#pp_php("array('a' => 'b');");die;
snowscript_to_php("
[]
[1, 2, 3]
['a': 'b']
['a', &b, 'c': 'd', 'e': &f]
", 0);
--EXPECT--
<?php
array();
array(1, 2, 3);
array('a' => 'b');
array('a', &$b, 'c' => 'd', 'e' => &$f);