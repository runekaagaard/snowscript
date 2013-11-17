--TEST--
Aliases (use)
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
use A\B
use C\D as E
use F\G as H, J
use \A
use \A as B
', 0);
--EXPECT--
<?php
use A\B;
use C\D as E;
use F\G as H, J;
use \A;
use \A as B;