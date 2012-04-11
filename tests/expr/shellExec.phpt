--TEST--
Shell execution
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
``
`test`
`test A`
`test \``
`test \"`
', 0);
--EXPECT--
<?php
``;
`test`;
`test $A`;
`test \``;
`test \"`;