--TEST--
Magic constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
__CLASS
__DIR
__FILE
__FUNCTION
__LINE
__METHOD
__NAMESPACE
__TRAIT
__GLOBALS
__SERVER
__GET
__POST
__FILES
__COOKIE
__SESSION
__REQUEST
__ENV

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
$GLOBALS;
$_SERVER;
$_GET;
$_POST;
$_FILES;
$_COOKIE;
$_SESSION;
$_REQUEST;
$_ENV;