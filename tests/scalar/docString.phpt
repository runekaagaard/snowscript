--TEST--
Nowdoc and heredoc strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
<<<\'EOS\'
EOS
<<<EOS
EOS
<<<\'EOS\'
Test \'" a \n
EOS
<<<EOS
Test \'" \a \n
EOS
<<<EOS
Test a
EOS
<<<EOS
Test a and b->c test
EOS

', 0);
--EXPECT--
<?php
<<<'EOS'
EOS;
<<<EOS
EOS;
<<<'EOS'
Test '" $a \n
EOS;
<<<EOS
Test '" \$a \n
EOS;
<<<EOS
Test $a
EOS;
<<<EOS
Test $a and $b->c test
EOS;
