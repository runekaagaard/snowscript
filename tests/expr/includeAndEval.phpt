--TEST--
Include and eval
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
include \'A.php\'
include_once \'A.php\'
require \'A.php\'
require_once \'A.php\'
eval(\'A\')
', 0);
--EXPECT--
<?php
include 'A.php';
include_once 'A.php';
require 'A.php';
require_once 'A.php';
eval('A');