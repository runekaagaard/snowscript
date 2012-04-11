--TEST--
Array definitions
--FILE--
<?php
array();
array('a');
array('a', );
array('a', 'b');
array('a', &b, 'c' => 'd', 'e' => &f);
[];
[1, 2, 3];
['a' => 'b'];
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