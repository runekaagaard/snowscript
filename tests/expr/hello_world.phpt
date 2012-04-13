--TEST--
Testing Hello World example.
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
echo "Hello, Snowscript World!"
');
--EXPECT--
<?php
echo 'Hello, Snowscript World!';