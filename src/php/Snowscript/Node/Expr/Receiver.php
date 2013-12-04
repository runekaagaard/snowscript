<?php

class Snowscript_Node_Expr_Receiver extends PHPParser_Node_Expr {
	public function __construct(PHPParser_Node_Expr $input, PHPParser_Node_Expr_FuncCall $fn, $line = -1, $docComment = null) {
		array_unshift($fn->args, new PHPParser_Node_Arg($input));
        parent::__construct(
            array(
                'fn' => $fn,
            ),
            $line, $docComment
        );
    }
}