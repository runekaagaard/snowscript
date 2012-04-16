--TEST--
Traits
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
trait A {
    public function a() {}
}
class B {
    use C
    use D {
        a as protected b
        c as d
        e as private
    }
    use E, F, G {
        E::a insteadof F, G
        E::b as protected c
        E::d as e
        E::f as private
    }
}
', 0);
--EXPECT--
<?php
trait A {
    public function a() {}
}
class B {
    use C;
    use D {
        a as protected b;
        c as d;
        e as private;
    }
    use E, F, G {
        E::a insteadof F, G;
        E::b as protected c;
        E::d as e;
        E::f as private;
    }
}