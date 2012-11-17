--TEST--
Testing Hello World example.
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

// TODO: Support chaining and not static scalar values. 

snowscript_to_php(<<<'SNOW'
a['b']?
c ? 42
a['b']??
c ?? get_c()
SNOW
);
--EXPECT--
<?php
isset($a['b']);
(isset($c) ? $c : 42);
!empty($a['b']);
(empty($c) ? $c : get_c());