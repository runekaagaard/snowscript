--TEST--
Aliases (use)
--FILE--
<?php
use A\B;
use C\D as E;
use F\G as H, J;
use \A;
use \A as B;
--EXPECT--
<?php
use A\B;
use C\D as E;
use F\G as H, J;
use \A;
use \A as B;