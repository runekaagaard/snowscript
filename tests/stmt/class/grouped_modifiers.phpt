--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("
class A
    public
        static
            final
                b = [1,2,3]
                c = 4
    private
        abstract
            d, e, f, g
            h = 52, i = [1, 2, 3]
    public
        k
    public static
        l
    protected static final 
        m
", 0);
--EXPECT--
<?php
class A
{
    public static final $b = array(1, 2, 3);
    public static final $c = 4;
    private abstract $d, $e, $f, $g;
    private abstract $h = 52, $i = array(1, 2, 3);
    public $k;
    public static $l;
    protected static final $m;
}
