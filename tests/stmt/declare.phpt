--TEST--
Declare
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
declare (A=\'B\', C=\'D\') {}
declare (A=\'B\', C=\'D\'):
enddeclare
', 0);
--EXPECT--
<?php
declare (A='B', C='D') {}
declare (A='B', C='D'):
enddeclare;