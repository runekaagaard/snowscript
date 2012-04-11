--TEST--
Semicolon style namespaces
--FILE--
<?php
namespace Foo\Bar;
foo;
namespace Bar;
bar;
--EXPECT--
<?php
namespace Foo\Bar;
foo;
namespace Bar;
bar;