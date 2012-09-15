--TEST--
Advanced strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

pp_php(<<<EOT

"yo";
'yo';

EOT);
snowscript_to_php(<<<EOT
"\n"
'\n'
EOT
, 1);
--EXPECT--
<?php
"\n"
'\n'