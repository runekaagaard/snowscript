--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

#pp_php("array('a' => 'b');");die;
snowscript_to_php("
{
	'a': 42,
	'b': 52,
	'hej': [1,2,3],
}
", 1);
--EXPECT--
<?php
array();
array(1, 2, 3);
array('a' => 'b');
array('a', &$b, 'c' => 'd', 'e' => &$f);