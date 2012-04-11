--TEST--
Class declaration
--FILE--
<?php
class A extends B implements C, D {
    const A = 'B', C = 'D';
    public a = 'b', c = 'd';
    protected e;
    private f;
    public function a() {}
    public static function b() {}
    public final function c() {}
    protected function d() {}
    private function e() {}
}
--EXPECT--
<?php
class A extends B implements C, D {
    const A = 'B', C = 'D';
    public $a = 'b', $c = 'd';
    protected $e;
    private $f;
    public function a() {}
    public static function b() {}
    public final function c() {}
    protected function d() {}
    private function e() {}
}