--TEST--
Braced namespaces
--FILE--
<?php
namespace Foo\Bar {
    foo;
}
namespace {
    bar;
}
--EXPECT--
<?php
namespace Foo\Bar {
    foo;
}
namespace {
    bar;
}