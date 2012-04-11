--TEST--
Nowdoc and heredoc strings
--FILE--
<?php
<<<'EOS'
EOS;
<<<EOS
EOS;
<<<'EOS'
Test '" a \n
EOS;
<<<EOS
Test '" \a \n
EOS;
<<<EOS
Test a
EOS;
<<<EOS
Test a and b->c test
EOS;

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
