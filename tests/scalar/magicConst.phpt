--TEST--
Magic constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
__CLASS__
__DIR__
__FILE__
__FUNCTION__
__LINE__
__METHOD__
__NAMESPACE__
__TRAIT__
', 0);
--EXPECT--
<?php
__CLASS__;
__DIR__;
__FILE__;
__FUNCTION__;
__LINE__;
__METHOD__;
__NAMESPACE__;
__TRAIT__;