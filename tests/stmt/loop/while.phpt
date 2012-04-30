--TEST--
While loop
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
while a
    pass
', 0);
--EXPECT--
<?php
while ($a) {
    
}