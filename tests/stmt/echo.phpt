--TEST--
Echo
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
echo \'Hallo World!\'
echo \'Hallo\', \' \', \'World\', \'!\'
', 0);
--EXPECT--
<?php
echo 'Hallo World!';
echo 'Hallo', ' ', 'World', '!';