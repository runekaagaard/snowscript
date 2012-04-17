--TEST--
Type hints
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
fn afunc(boo, array cool, callable erland, Flix fuzzy)
    <- 42
', 0);
--EXPECT--
<?php
function afunc($boo, array $cool, callable $erland, Flix $fuzzy)
{
    return 42;
}