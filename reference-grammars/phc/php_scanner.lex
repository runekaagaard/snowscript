/*
 * phc -- the open source PHP compiler
 * See doc/license/README.license for licensing information
 *
 * Lexical analyser
 *
 * NOTE: 
 * In PHP, the constant "01090" is taken to be an octal number; everything
 * from the 9 onwards is silently ignored (so, the number evaluates to 8
 * decimal). phc will give an error message instead.
*/

%{
	#include <cassert>
	#include <cstring>
	#include "AST.h"
	using namespace AST;
	#include "lib/Integer.h"
	#include "php_parser.tab.hpp"
	#include "cmdline.h"
	#include "parsing/PHP_context.h"
	#include "keywords.h"

	extern struct gengetopt_args_info args_info;
	
	#define YY_SKIP_YYWRAP

	/*
	 * Macros to return a token
	 * If dump_tokens_flag is set, also print the token to stdout
	 */

	#define RETURN(x) {															\
		if(false)	            												\
			printf("%ld: " #x "\n", yyextra->source_line);				\
		yyextra->after_arrow = (x) == O_SINGLEARROW;						\
		yyextra->starts_line = false;											\
		return x; }
	#define RETURN_OP(t, s) {													\
		if(false)	            												\
			printf("%ld: SIMPLE_OP %s\n", yyextra->source_line, s);	\
		yylval->token_op = new OP(new String(s)); 				\
		copy_state(yylval->token_op, yyextra);								\
		yyextra->after_arrow = false;											\
		yyextra->starts_line = false;											\
		return t; } 

	#define RETURN_ALL(state)					\
		yyextra->mt_final_state = state;		\
		yyextra->mt_index = 1;					\
		BEGIN(RET_MULTI);							\
		*yylval = yyextra->mt_lval[0];		\
		RETURN(yyextra->mt_type[0]);

	#define RETURN_CAST(x)													\
		String str(yytext);													\
		int start = str.find_first_not_of ("\t (");					\
		int end = str.find_last_not_of ("\t )");						\
		assert (start < end);												\
		yylval->string = new String (str, start, end-start+1);	\
		RETURN(x);

	#define YY_EXTRA_TYPE PHP_context*

	// Defined in php_parser.ypp
	Node* copy_state(Node* node, PHP_context* context);

	#define YY_INPUT(buf,result,max_size)		\
	{														\
		std::istream& stream = yyextra->stream;		\
		stream.read (buf, max_size);				\
		result = stream.gcount ();					\
		if (result == 0 && stream.eofbit)		\
			result = YY_NULL;							\
	}
%}

%option reentrant
%option bison-bridge
%option prefix="PHP_"
%option noyywrap
%option stack
%option nounput
%option noyy_top_state

	/* Define lexical states */

%x PHP
%x SQ_STR		
%x SQ_ESC
%x BT_STR
%x DQ_STR		
%x HD_STR
%x HD_NL
%x HD_MAIN
%x HD_END
%x ESCAPE
%x ML_COMM
%x SL_COMM

%x COMPLEX1
%x COMPLEX2

%x RET_MULTI

	/* Define a few tokens referenced in the grammar, below */

NL					\r?\n?
WS					[ \t\n\r]
ANY				[\x00-\xff]	

PHP				[pP][hH][pP]
PHP_START		"<?"{PHP}?
SCRIPT_START	"<script"{WS}+"language"{WS}*"="{WS}*['"]{WS}*{PHP}{WS}*['"]{WS}*">"
START				{PHP_START}|{SCRIPT_START}
START_ECHO		"<?="
PHP_STOP			"?>"
SCRIPT_STOP		"</script"{WS}*">"
STOP				({PHP_STOP}|{SCRIPT_STOP}){NL}?

IDENT				[a-zA-Z_\x7F-\xFF][a-zA-Z0-9_\x7F-\xFF]*

DEC				([1-9][0-9]*)|0
HEX				0[xX][0-9a-fA-F]+
OCT				0[0-7]+
INT				({DEC}|{HEX}|{OCT})

LNUM				[0-9]+
DNUM				([0-9]*[\.]{LNUM})|({LNUM}[\.][0-9]*)
EXPONENT_DNUM	(({LNUM}|{DNUM})[eE][+-]?{LNUM})
REAL				{DNUM}|{EXPONENT_DNUM}

BRACKET			[(){}[\]]
ARITHMETIC		[+\-/*%^]
BITWISE			[&|~]
RELATIONAL		[=><]
OTHER_OP			[.!,?:$@]
SIMPLE_OP		{BRACKET}|{ARITHMETIC}|{BITWISE}|{RELATIONAL}|{OTHER_OP}

CAST_WS			[\t ]
CS					"("{CAST_WS}*
CE					{CAST_WS}*")"

C_INTEGER		[iI][nN][tT][eE][gG][eE][rR]
C_INT				[iI][nN][tT]
INT_CAST			{CS}({C_INTEGER}|{C_INT}){CE}

C_FLOAT			[fF][lL][oO][aA][tT]
C_REAL			[rR][eE][aA][lL]
C_DOUBLE			[dD][oO][uU][bB][lL][eE]
REAL_CAST		{CS}({C_FLOAT}|{C_REAL}|{C_DOUBLE}){CE}

C_STRING			[sS][tT][rR][iI][nN][gG]
STRING_CAST		{CS}{C_STRING}{CE} 

C_ARRAY			[aA][rR][rR][aA][yY]
ARRAY_CAST		{CS}{C_ARRAY}{CE}

C_OBJECT			[oO][bB][jJ][eE][cC][tT]
OBJECT_CAST		{CS}{C_OBJECT}{CE}

C_BOOL			[bB][oO][oO][lL]
C_BOOLEAN		[bB][oO][oO][lL][eE][aA][nN]
BOOL_CAST		{CS}({C_BOOL}|{C_BOOLEAN}){CE}

C_UNSET			[uU][nN][sS][eE][tT]
UNSET_CAST		{CS}{C_UNSET}{CE}

%%

	/* Update source_line */

<*>{NL}					{
								if(YY_START != SL_COMM && YY_START != ML_COMM)
									yyextra->attach_to_previous = 0;

								yyextra->source_line++;
								yyextra->starts_line = true;
								REJECT;
							}

	/* Casts */

<PHP>{INT_CAST}		{ RETURN_CAST (CAST_INT);  }
<PHP>{REAL_CAST}		{ RETURN_CAST (CAST_REAL);  }
<PHP>{STRING_CAST}	{ RETURN_CAST (CAST_STRING);  }
<PHP>{ARRAY_CAST}		{ RETURN_CAST (CAST_ARRAY);  }
<PHP>{OBJECT_CAST}	{ RETURN_CAST (CAST_OBJECT);  }
<PHP>{BOOL_CAST}		{ RETURN_CAST (CAST_BOOL);  }
<PHP>{UNSET_CAST}		{ RETURN_CAST (CAST_UNSET);  }

	/* Operators */

<PHP>"=="				{ RETURN_OP(O_EQEQ, "=="); }
<PHP>"==="				{ RETURN_OP(O_EQEQEQ, "==="); }
<PHP>"!="				{ RETURN_OP(O_NOTEQ, "!="); }
<PHP>"<>"				{ RETURN_OP(O_NOTEQ, "<>"); }
<PHP>"!=="				{ RETURN_OP(O_NOTEQEQ, "!=="); }
<PHP>"<="				{ RETURN_OP(O_LE, "<="); }
<PHP>">="				{ RETURN_OP(O_GE, ">="); }

<PHP>"++"				{ RETURN(O_INC); }
<PHP>"--"				{ RETURN(O_DEC); }
<PHP>"=>"				{ RETURN(O_DOUBLEARROW); }
<PHP>"->"				{ RETURN(O_SINGLEARROW); }

<PHP>"<<"				{ RETURN_OP(O_SL, "<<"); }
<PHP>">>"				{ RETURN_OP(O_SR, ">>"); }

<PHP>"+="				{ RETURN(O_PLUSEQ); }
<PHP>"-="				{ RETURN(O_MINUSEQ); }
<PHP>"*="				{ RETURN(O_MULEQ); }
<PHP>"/="				{ RETURN(O_DIVEQ); }
<PHP>".="				{ RETURN(O_CONCATEQ); }
<PHP>"%="				{ RETURN(O_MODEQ); }
<PHP>"&="				{ RETURN(O_ANDEQ); }
<PHP>"|="				{ RETURN(O_OREQ); }
<PHP>"^="				{ RETURN(O_XOREQ); }
<PHP>"<<="				{ RETURN(O_SLEQ); }
<PHP>">>="				{ RETURN(O_SREQ); }
<PHP>"::"				{ RETURN(O_COLONCOLON); }

<PHP>"&&"				{ RETURN_OP(O_LOGICAND, "&&"); }
<PHP>"||"				{ RETURN_OP(O_LOGICOR, "||"); }

<PHP>{SIMPLE_OP}		{ RETURN_OP(*yytext, yytext); }
<PHP>";"					{ 
								yyextra->attach_to_previous = true; 
								RETURN_OP(*yytext, yytext); 
							} 

	/* Tokens */

<PHP>${IDENT}			{
								// variable names do not contain $
								yylval->string = new String(yytext+1); 
								RETURN(VARIABLE);
							}
<PHP>{IDENT}			%{
							{	// Can't declare local variables without scoping them
								
								// We generate a semantic value which equals the 
								// keyword so that we can reproduce it exactly the
								// same way in the unparsers, if we so desire 
								// (keywords are case insensitive)
								yylval->string = new String(yytext);
							
								// Check if the ident is in fact a keyword
								const struct keyword* keyword;
								keyword = in_word_set(yytext, yyleng);
								if(keyword != 0 && !yyextra->after_arrow)
								{
									switch(keyword->token)
									{
										case K_AND:
										case K_OR:
										case K_XOR:
											yylval->token_op = new OP(
												new String(yytext));
											copy_state(yylval->token_op, yyextra);
											break;
										case C_FALSE:
											yylval->token_bool = new BOOL(false, 
												new String(yytext));
											copy_state(yylval->token_bool, yyextra);
											break;
										case C_TRUE:
											yylval->token_bool = new BOOL(true, 
												new String(yytext));
											copy_state(yylval->token_bool, yyextra);
											break;
										case C_NULL:
											yylval->token_null = new NIL( 
												new String(yytext));
											copy_state(yylval->token_null, yyextra);
											break;
										case K___LINE__:
											yylval->token_int = new INT(
												yyextra->source_line,
												new String(yytext));
											copy_state(yylval->token_int, yyextra);
											break;
										case K___FILE__:
											yylval->token_string = new STRING(
												yyextra->filename,
												new String("__FILE__"));
											copy_state(yylval->token_string, yyextra);
											break;
										case K___CLASS__:
											yylval->token_string = new STRING(
												yyextra->current_class,
												new String("__CLASS__"));
											copy_state(yylval->token_string, yyextra);
											break;
										case K___METHOD__:
										{
											String* method_name = new String;
											if (yyextra->current_class->size () > 0)
											{
												method_name->append (*yyextra->current_class);
												method_name->append ("::");
											}
											method_name->append (*yyextra->current_method);

											yylval->token_string = new STRING(
												method_name,
												new String("__METHOD__"));
											copy_state(yylval->token_string, yyextra);
											break;
										}
										case K___FUNCTION__:
											yylval->token_string = new STRING(
												yyextra->current_method,
												new String("__FUNCTION__"));
											copy_state(yylval->token_string, yyextra);
											break;
										case K_CLASS:
										case K_FUNCTION:
											yyextra->attach_to_previous = 1;
											break;
									}
									
									RETURN(keyword->token);
								}
								else
								{
									RETURN(IDENT);
								}
							}
							%}
<PHP>{INT}				{ 
								INT* i = new INT(
									0, // initialized in Token_conversion
									new String(yytext));
								copy_state(i, yyextra);
								yylval->token_int = i;
								RETURN(T_INT); 
							}
<PHP>{REAL}				{ 
								REAL* r = new REAL(
									0.0, // initialized in Token_conversion
									new String(yytext));
								copy_state(r, yyextra);
								yylval->token_real = r;
								RETURN(T_REAL); 
							}
<PHP>{STOP}				{ 
								yyextra->value_buffer = ""; 
								yyextra->source_rep_buffer = "";
								BEGIN(INITIAL); 
								RETURN(';'); 
							}

	/* Strings */

<PHP>"'"					{ 
								yyextra->value_buffer = ""; 
								yyextra->source_rep_buffer = "";
								BEGIN(SQ_STR); 
							}
<PHP>"`"					{ 
								// We don't know when the actual function call will
								// be constructed, so we cannot set any attributes on
								// it. Therefore, we generate a function call to '`',
								// which will be corrected to a function call to 
								// shell_exec in the parser, which will then set an
								// attribute phc.unparser.is_backticked
								yyextra->schedule_return(IDENT, "`");
								yyextra->schedule_return('(');
								yyextra->value_buffer = ""; 
								yyextra->source_rep_buffer = "";
								RETURN_ALL(BT_STR);
							}
<PHP>"\""				{ 
								yyextra->value_buffer = ""; 
								yyextra->source_rep_buffer = "";
								BEGIN(DQ_STR); 
							}
<PHP>"<<<"" "?			{ 
								yyextra->value_buffer = ""; 
								yyextra->source_rep_buffer = "";
								BEGIN(HD_STR); 
							}

	/* Comments */

<PHP>^[ \t]*\n			{
								// Following a suggestion by Tim Van Holder on bison-help,
								// we treat blank lines as comments
								yyextra->last_comments.push_back(new String(""));
							}
<PHP>"/*"				{
								yyextra->value_buffer = yytext;	
								BEGIN(ML_COMM); 
							}
<PHP>#|"//"				{ 
								yyextra->value_buffer = yytext;	
								BEGIN(SL_COMM); 
							}

<ML_COMM>"*/"			{ 
								yyextra->value_buffer.append(yytext);
								
								if(yyextra->attach_to_previous)
									yyextra->attach_comment(new String(yyextra->value_buffer));
								else
									yyextra->last_comments.push_back(new String(yyextra->value_buffer));
								BEGIN(PHP); 
							
								yyextra->value_buffer = "";
								yyextra->source_rep_buffer = "";
							}
<ML_COMM>{ANY}			{ yyextra->value_buffer.push_back(*yytext); }

<SL_COMM>{NL}			{ 
								if(yyextra->attach_to_previous)
									yyextra->attach_comment(new String(yyextra->value_buffer));
								else
									yyextra->last_comments.push_back(new String(yyextra->value_buffer));
								yyextra->attach_to_previous = 0;
								BEGIN(PHP);

								yyextra->value_buffer = "";
								yyextra->source_rep_buffer = "";
							}
<SL_COMM>{PHP_STOP}	{
								yyextra->value_buffer = ""; 
								yyextra->source_rep_buffer = ""; 
								BEGIN(INITIAL); 
							}
<SL_COMM>.				{ yyextra->value_buffer.push_back(*yytext); }	

	/* Any other character */

<PHP>{WS}				/* Ignore */
<PHP>{ANY}				{ RETURN(INVALID_TOKEN); }

	/* Deal with singly quoted strings */

<SQ_STR>\'			{
							STRING* str = new STRING(
								new String(yyextra->value_buffer),
								new String(yyextra->source_rep_buffer));
							copy_state(str, yyextra);
							yylval->token_string = str;

							BEGIN(PHP);
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN(T_STRING);
						}
<SQ_STR>\\			{
							yyextra->source_rep_buffer.push_back('\\');
							BEGIN(SQ_ESC); 
						}
<SQ_STR>{ANY}		{ 
							yyextra->value_buffer.push_back(*yytext); 
							yyextra->source_rep_buffer.push_back(*yytext);
						}

<SQ_ESC>\'			{ 
							yyextra->value_buffer.push_back(*yytext); 
							yyextra->source_rep_buffer.push_back(*yytext); 
							BEGIN(SQ_STR); 
						}
<SQ_ESC>\\			{
							yyextra->value_buffer.push_back(*yytext); 
							yyextra->source_rep_buffer.push_back(*yytext); 
							BEGIN(SQ_STR); 
						}
<SQ_ESC>{ANY}		{
							yyextra->value_buffer.push_back('\\');
							yyextra->value_buffer.push_back(*yytext);
							// source_rep_buffer already has the '\\'
							yyextra->source_rep_buffer.push_back(*yytext);
							BEGIN(SQ_STR); 
						}

	/* Deal with backticked strings. */

<BT_STR>\`			{
							yyextra->schedule_return_string();
							yyextra->schedule_return(')');
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(PHP);
						}
<BT_STR>\\\`		{ 
							yyextra->value_buffer.push_back('`'); 
							yyextra->source_rep_buffer.append(yytext);
						}
<BT_STR>\\			{
							yyextra->source_rep_buffer.push_back('\\');
							yy_push_state(ESCAPE, yyscanner); 
						}
<BT_STR>{ANY}		{ 
							yyextra->value_buffer.push_back(*yytext); 
							yyextra->source_rep_buffer.push_back(*yytext); 
						}

	/* Deal with in-string syntax (in DQ_STR, HD_STR, and BT_STR) */

<DQ_STR,HD_MAIN,BT_STR>"$"{IDENT} {
							yyextra->schedule_return_string();
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							yyextra->schedule_return(VARIABLE, &yytext[1]);
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");

							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(YY_START);
						}	
<DQ_STR,HD_MAIN,BT_STR>"${"{IDENT}"}" {
							yyextra->schedule_return_string();
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.delimited");
							yyextra->schedule_return(VARIABLE, &yytext[2], yyleng - 3);
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(YY_START);
						}
<DQ_STR,HD_MAIN,BT_STR>"$"{IDENT}"["{INT}"]" %{
						{
							long left, right;
							left = strchr(yytext, '[') - yytext;
							right = strchr(yytext, ']') - yytext;

							yyextra->schedule_return_string();
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							yyextra->schedule_return(VARIABLE, &yytext[1], left - 1);
							yyextra->schedule_return('[');
							yyextra->schedule_return(T_INT, &yytext[left+1], right - left - 1);
							yyextra->schedule_return(']');
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(YY_START);
						} 
						%}
<DQ_STR,HD_MAIN,BT_STR>"$"{IDENT}"["{IDENT}"]" %{
						{
							long left, right;
							left = strchr(yytext, '[') - yytext;
							right = strchr(yytext, ']') - yytext;
							
							yyextra->schedule_return_string();
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							yyextra->schedule_return(VARIABLE, &yytext[1], left - 1);
							yyextra->schedule_return('[');
							yyextra->schedule_return(T_STRING, &yytext[left+1], right - left - 1);
							yyextra->schedule_return(']');
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(YY_START);
						} 
						%}
<DQ_STR,HD_MAIN,BT_STR>"$"{IDENT}"[$"{IDENT}"]" %{
						{
							long left, right;
							left = strchr(yytext, '[') - yytext;
							right = strchr(yytext, ']') - yytext;
							
							yyextra->schedule_return_string();
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							yyextra->schedule_return(VARIABLE, &yytext[1], left - 1);
							yyextra->schedule_return('[');
							yyextra->schedule_return(VARIABLE, &yytext[left+2], right - left - 2);
							yyextra->schedule_return(']');
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(YY_START);
						} 
						%}
<DQ_STR,HD_MAIN,BT_STR>"$"{IDENT}"->"{IDENT} %{
						{
							long arrow;
							arrow = strchr(yytext, '-') - yytext;
							
							yyextra->schedule_return_string();
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");
							yyextra->schedule_return(VARIABLE, &yytext[1], arrow - 1);
							yyextra->schedule_return(O_SINGLEARROW);
							yyextra->schedule_return(IDENT, &yytext[arrow+2]);
							yyextra->schedule_return_op(".", "phc.unparser.in_string_syntax.simple");

							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN_ALL(YY_START);
						} 
						%}

<DQ_STR,HD_MAIN,BT_STR>"{$"	{
							yy_push_state(COMPLEX2, yyscanner);
							yy_push_state(COMPLEX1, yyscanner);

							STRING* str = new STRING(
								new String(yyextra->value_buffer),
								new String(yyextra->source_rep_buffer));
							copy_state(str, yyextra);
							yylval->token_string = str;

							yyless(1);
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN(T_STRING);
						}

<ESCAPE>n			{ 
							yyextra->value_buffer.push_back('\n'); 
							yyextra->source_rep_buffer.push_back('n'); 
							yy_pop_state(yyscanner); 
						}
<ESCAPE>t			{ 
							yyextra->value_buffer.push_back('\t'); 
							yyextra->source_rep_buffer.push_back('t'); 
							yy_pop_state(yyscanner); 
						}
<ESCAPE>r			{ 
							yyextra->value_buffer.push_back('\r'); 
							yyextra->source_rep_buffer.push_back('r'); 
							yy_pop_state(yyscanner); 
						}
<ESCAPE>\\			{ 
							yyextra->value_buffer.push_back('\\'); 
							yyextra->source_rep_buffer.push_back('\\'); 
							yy_pop_state(yyscanner); 
						}
<ESCAPE>\$			{ 
							yyextra->value_buffer.push_back('$');  
							yyextra->source_rep_buffer.push_back('$');  
							yy_pop_state(yyscanner); 
						}
<ESCAPE>x[0-9A-Fa-f]{1,2} %{
						{
							char c = (char) strtol(yytext + 1, 0, 16);
							yyextra->value_buffer.push_back(c);
							yyextra->source_rep_buffer.append(yytext);
							yy_pop_state(yyscanner);
						}
						%}
<ESCAPE>[0-7]{1,3} %{
						{
							char c = (char) strtol(yytext, 0, 8);
							yyextra->value_buffer.push_back(c);
							yyextra->source_rep_buffer.append(yytext);
							yy_pop_state(yyscanner);
						}
						%}
<ESCAPE>{ANY}		{ 
							yyextra->value_buffer.push_back('\\');
							yyextra->value_buffer.push_back(*yytext);
							// source_rep_buffer already has the '\\'
							yyextra->source_rep_buffer.push_back(*yytext);
							yy_pop_state(yyscanner);
						}

	/* Complex syntax */

<COMPLEX1>{ANY}	{
							yyless(0);
							BEGIN(PHP);

							OP* op = new OP(new String("."));
							copy_state(op, yyextra);
							op->attrs->set_true("phc.unparser.in_string_syntax.complex");
							yylval->token_op = op; 

							RETURN(O_MAGIC_CONCAT);
						}
<COMPLEX2>{ANY}	{
							yyless(0);
							yy_pop_state(yyscanner);
							
							OP* op = new OP(new String("."));
							copy_state(op, yyextra);
							op->attrs->set_true("phc.unparser.in_string_syntax.simple");
							yylval->token_op = op;

							RETURN('.');
						}

	/* Deal with (doubly quoted) strings. */

<DQ_STR>\"			{
							STRING* str = new STRING(
								new String(yyextra->value_buffer),
								new String(yyextra->source_rep_buffer));
							str->attrs->set_true("phc.unparser.is_doubly_quoted");
							copy_state(str, yyextra);
							yylval->token_string = str;

							BEGIN(PHP);
							yyextra->value_buffer = "";
							yyextra->source_rep_buffer = "";
							RETURN(T_STRING);
						}

<DQ_STR>\\\"		{ 
							yyextra->value_buffer.push_back('"'); 
							yyextra->source_rep_buffer.append(yytext);
						}
<DQ_STR>\\			{
							yyextra->source_rep_buffer.push_back('\\');
							yy_push_state(ESCAPE, yyscanner); 
						}
<DQ_STR>{ANY}		{ 
							yyextra->value_buffer.push_back(*yytext); 
							yyextra->source_rep_buffer.push_back(*yytext); 
						}

	/* Heredoc syntax */

<HD_STR>{IDENT}	{
							yyextra->heredoc_id = strdup(yytext);
							yyextra->heredoc_id_len = yyleng;
							yyextra->heredoc_id_ptr = 0;
							
							if(!strcmp(yytext, "INLINE_C"))
								yyextra->last_comments.push_back(new String("// phc:inline-c"));
							else
							{
								// We don't know when actual STRING will be created
								// (it could be when we see in-string syntax, for example)
								// Hence, we add it to the parser context and rely on
								// copy_state to add it to whatever STRING will be 
								// created next
								yyextra->heredoc_attr = new String(yytext);
							}

							BEGIN(HD_NL);
						}
<HD_STR>.			{ 
							yyless(0); 
							BEGIN(PHP); 
							RETURN(INVALID_TOKEN);
						}

<HD_NL>{NL}			{ BEGIN(HD_MAIN); }
<HD_NL>{ANY}		{ RETURN(INVALID_TOKEN);	}

<HD_MAIN>\\			{
							yyextra->source_rep_buffer.push_back('\\');
							yy_push_state(ESCAPE, yyscanner); 
						}
<HD_MAIN>^{ANY}	{
							yyextra->value_buffer.push_back(*yytext);
							yyextra->source_rep_buffer.push_back(*yytext);

							if(*yytext == yyextra->heredoc_id[0])
								yyextra->heredoc_id_ptr = &yyextra->heredoc_id[1];
						}
<HD_MAIN>{ANY}		%{
							yyextra->value_buffer.push_back(*yytext);
							yyextra->source_rep_buffer.push_back(*yytext);

							if(yyextra->heredoc_id_ptr && (*yyextra->heredoc_id_ptr == *yytext))
								yyextra->heredoc_id_ptr++;
							else
								yyextra->heredoc_id_ptr = 0;

							if(yyextra->heredoc_id_ptr - yyextra->heredoc_id == yyextra->heredoc_id_len)
							{	
								BEGIN(HD_END);
							}
						%}
<HD_END>{NL}|;		%{ 
							{
								// Remove heredoc_id from the buffer 
								long value_len = yyextra->value_buffer.size() - yyextra->heredoc_id_len;
								long source_rep_len = yyextra->source_rep_buffer.size() - yyextra->heredoc_id_len;

								// The linebreak of the last line of the HEREDOC
								// string should also be stripped
								if(value_len > 0 && yyextra->value_buffer[value_len - 1] == '\n')
								{
									value_len--;
									source_rep_len--;
								}
								if(value_len > 0 && yyextra->value_buffer[value_len - 1] == '\r')
								{
									// Windows file
									value_len--;
									source_rep_len--; 
								}
						
								STRING* str = new STRING(
									new String(yyextra->value_buffer.substr(0, value_len)),
									new String(yyextra->source_rep_buffer.substr(0, source_rep_len)));
								
								// Reset starts_line because we don't output the
								// string using HEREDOC syntax 
								yyextra->starts_line = false;
								copy_state(str, yyextra);
								yylval->token_string = str;
								
								if(yytext[0] == ';')
									yyless(0);
								
								BEGIN(PHP);
								yyextra->value_buffer = "";
								yyextra->source_rep_buffer = "";
								RETURN(T_STRING);
							}
						%}
<HD_END>.			%{
							yyextra->value_buffer.push_back(*yytext);
							yyextra->source_rep_buffer.push_back(*yytext);
							yyextra->heredoc_id_ptr = 0;
							BEGIN(HD_MAIN);
						%}

	/* Returning multiple tokens */

<RET_MULTI>{ANY}	{
							yyless(0);

							if(yyextra->mt_index == yyextra->mt_count - 1)
							{
								yyextra->mt_count = 0;
								BEGIN(yyextra->mt_final_state);
							}

							*yylval = yyextra->mt_lval[yyextra->mt_index];
							yyextra->mt_index++;
							RETURN(yyextra->mt_type[yyextra->mt_index - 1]);
						}

	/* Deal with HTML fragments */

^"#!".*\n			{
							if(yyextra->source_line == 1)
							{
								yyextra->hash_bang = new String(yytext);
							}
							else
							{
								yyextra->value_buffer.append(yytext);
							}
						}
{START_ECHO}		{
							// The logic that deals with returning multiple tokens
							// needs at least two tokens to work with.
							if(!yyextra->value_buffer.empty())
								yyextra->schedule_return(INLINE_HTML, yyextra->value_buffer);
							else
								yyextra->schedule_return(';');

							yyextra->schedule_return(K_ECHO);
							RETURN_ALL(PHP);
						}
{START}				%{
							BEGIN(PHP); 

							if(!yyextra->value_buffer.empty())
							{
								yylval->string = new String(yyextra->value_buffer);
								RETURN(INLINE_HTML);
							}
						%}
<<EOF>>				%{
							if(yyextra->value_buffer.empty())
							{
								yyterminate();
							} 
							else 
							{
								yylval->string = new String(yyextra->value_buffer);	
								yyextra->value_buffer = "";
								yyextra->source_rep_buffer = "";
								RETURN(INLINE_HTML);
							}
						%}
{ANY}					{ yyextra->value_buffer.push_back(*yytext); }

%%

/**
 * We need to define this here rather than in PHP_lexer.h because we
 * need access to the BEGIN and COMPLEX2 macros defined in lex.yy.cc
 */

void PHP_context::init_scanner(FILE*)
{
	yylex_init(&scanner);
	yyset_extra(this, scanner);
	yyset_in(NULL, scanner);
}

void PHP_context::destroy_scanner()
{
	yylex_destroy(scanner);
}

void PHP_context::return_to_complex_syntax()
{
	yy_pop_state(scanner);
}

// Attach a comment to the last node generated
void PHP_context::attach_comment(String *s)
{
	s->attrs->set_true("phc.unparser.comment.after");
	assert(last_commented_node);
	last_commented_node->get_comments()->push_back(s);
}	
	
void PHP_context::schedule_return(long type, const char* lval, long length)
{
	mt_type[mt_count] = type;

	if(lval)
	{
		INT* i;
		STRING* s;
		int len = length == -1 ? strlen(lval) : length;
		
		if(type < 256)
		{
			// Simple op
			OP* o = new OP(new String(lval, len));
			copy_state(o, this);
			mt_lval[mt_count].token_op = o;
		}
		else switch(type)
		{
			case T_INT:
				i = new INT(0, new String(lval, len)); // initialized in Token_conversion
				copy_state(i, this);
				mt_lval[mt_count].token_int = i;
				break;
			case T_STRING:
				s = new STRING(new String(lval, len), new String(lval, len));
				copy_state(s, this);
				mt_lval[mt_count].token_string = s;
				break;
			default:
				mt_lval[mt_count].string = new String(lval, len);
				break;
		};
	}
	else
	{
		mt_lval[mt_count].object = NULL;
	}

	mt_count++;
}

void PHP_context::schedule_return_string()
{
   STRING* token_string;
   
   token_string = new STRING(new String(value_buffer), new String(source_rep_buffer));
   copy_state(token_string, this);

	mt_type[mt_count] = T_STRING;
	mt_lval[mt_count].token_string = token_string; 
	mt_count++;
}

void PHP_context::schedule_return_op(const char* op, const char* attr)
{
   OP* token_op;
   
   token_op = new OP(new String(op));
   copy_state(token_op, this);

   if(attr != NULL)
   {
      token_op->attrs->set_true(attr);
   }

   mt_type[mt_count] = *op;
   mt_lval[mt_count].token_op = token_op;
   mt_count++;
}

void PHP_context::schedule_return(long type, string& s)
{
	schedule_return(type, s.c_str(), s.size());
}
