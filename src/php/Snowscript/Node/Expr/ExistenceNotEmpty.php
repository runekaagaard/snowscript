<?php

class Snowscript_Node_Expr_ExistenceNotEmpty extends PHPParser_Node_Expr {
	public function __construct(PHPParser_Node $test_for, $default, $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'test_for' => $test_for,
                'default' => $default,
            ),
            $line, $docComment
        );
    }
}