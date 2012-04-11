--TEST--
isset() and empty()
--FILE--
<?php
isset(a);
isset(a, b, c);
empty(a);
--EXPECT--
<?php
isset($a);
isset($a, $b, $c);
empty($a);