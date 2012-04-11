--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
array()
array(\'a\')
array(\'a\', )
array(\'a\', \'b\')
array(\'a\', &b, \'c\' => \'d\', \'e\' => &f)
[]
[1, 2, 3]
[\'a\' => \'b\']
', 0);
--EXPECT--
<?php
array();
array('a');
array('a', );
array('a', 'b');
array('a', &$b, 'c' => 'd', 'e' => &$f);
[];
[1, 2, 3];
['a' => 'b'];