--TEST--
Semicolon style namespaces
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
namespace Foo\Bar
foo
namespace Bar
bar
', 0);
--EXPECT--
<?php
namespace Foo\Bar;
foo;
namespace Bar;
bar;