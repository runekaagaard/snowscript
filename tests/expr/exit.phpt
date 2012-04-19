--TEST--
Exit
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
exit
exit()
exit(\'Die!\')
die
die()
die(\'Exit!\')
', 0);
--EXPECT--
<?php
die;
die;
die('Die!');
die;
die;
die('Exit!');