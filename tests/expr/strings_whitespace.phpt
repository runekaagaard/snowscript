--TEST--
Advanced strings
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'EOD'
fn foo()
    echo """
        This is base\

        And I like this!
        """
EOD
, 0);
--EXPECT--
<?php
function foo()
{
    echo "
This is base
And I like this!
";
}