--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php("
{}
{
	hallo: world,
	'this': 42,
	'''that''': optur,
	is_good: 'hallo',
	A_KEY: 3,
}

", 0);
--EXPECT--
<?php
array();
array('hallo' => $world, 'this' => 42, 'that' => $optur, 'is_good' => 'hallo', 'A_KEY' => 3);