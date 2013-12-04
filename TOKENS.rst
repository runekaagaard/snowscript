Stub.

tokens = [ ABSTRACT  AMPER  AND  AND_EQUAL  ARRAY  AT  BACKQUOTE ,
      BAND  BLEFT  BNOT  BOOL  BOR  BOX  BREAK  BRIGHT  BXOR ,
      CALLABLE  CASE  CATCH  CIRCUMFLEX  CLASS  CLASS_NAME  CLONE ,
      COLON  COMMA  COMMENT  COMMENT_MULTILINE  CONCAT_EQUAL  CONST , 
      CONSTANT_NAME ,
      DEC  DECLARE  DEFAULT  DIE  DIV_EQUAL  DOUBLE_DOT  DOT ,
      DOUBLE_COLON  DOWNTO  ECHO  ELIF  ELSE  EMPTY  END ,
      EQUAL  ESCAPE  EXIT  EXTENDS  FALLTHRU  FALSE  FINAL ,
      FLOAT  FN  FOR  GLOBAL  GREATER  IF  IMPLEMENTS  IN ,
      INC  INCLUDE  INCLUDE_ONCE  INLINE_HTML  INNER_RETURN ,
      INSIDE_COMMENT  INT  INTERFACE  ISA  ISSET  IS_EQUAL ,
      IS_GREATER_OR_EQUAL  IS_IDENTICAL  IS_NOT_EQUAL  IS_NOT_IDENTICAL ,
      IS_SMALLER_OR_EQUAL  LBRACE  LESS  LIST  LPAR  LSQB  MINUS ,
      MINUS_EQUAL  MOD  MOD_EQUAL  MUL_EQUAL  NAMESPACE  NEW ,
      NEXT  NOT  NULL  OBJECT  OR  OR_EQUAL  PASS  PERCENT ,
      PIPE  PLUS  PLUS_EQUAL  POW  PRINT  PRIVATE  PROTECTED ,
      PUBLIC  RBRACE  RECEIVER  REQUIRE  REQUIRE_ONCE  RETURN ,
      RPAR  RSQB  SEMI  SL  SLASH  SL_EQUAL  SR  SR_EQUAL ,
      STAR  STATIC  STRINGTYPE  STRING_WITH_CONCAT  SWITCH  THROW ,
      TILDE  TO  TRAIT  TRUE  TRY  UNSET  USE  VARIABLE_NAME ,
      WHEN  WHILE  XOR  XOR_EQUAL  _AND_  _OR_  STEP ,
      DOUBLE_ARROW  DOUBLE_QUESTION_MARK  QUESTION_MARK  THEN , 
      STRING_DOUBLE  STRING_SINGLE  PARENT ,
]
symbolic unchanged:
	&& -> and
	&

unchanged:
	abstract
	catch
	class
	else
	elseif -> elif
	continue -> ?
	extends
	final
	for
	function -> fn
	if
	implements
	instanceof -> isa ??
	interface
	private
	protected
	public
	return -> <-
	static
	throw
	trait
	try
	while
	xor

called_as_function:
	array
	callable
	clone
	die
	echo
	empty
	eval
	exit
	include
	include_once
	isset
	print
	require
	require_once
	unset

used_with_prefix:
	const

deleted:
	and 
 	as
 	break
 	case
 	default
 	do
 	endfor
 	endforeach
 	endif
 	endswitch
 	endwhile
 	foreach
 	goto
 	insteadof
 	list
 	namespace
 	new
 	or
 	switch
 	use
 	var
 	declare
 	enddeclare

 unsure:
 	global
 	


                                                              

$predefined_constants = array(__CLASS__ __DIR__ __FILE__ __FUNCTION__ __LINE__ __METHOD__ __NAMESPACE__ __TRAIT__);
