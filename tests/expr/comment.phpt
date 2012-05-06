--TEST--
Comments
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
class Foo

    # A comment.
    public a = false
', 1);
--EXPECT--
<?php
