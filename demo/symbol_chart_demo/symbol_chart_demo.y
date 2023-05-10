%{

//symbol chart

// 符号表相关
struct STRUCT_SYM_INFO{
	char* name;
	int value;
	int isAssigned;
};

#define SYM_INFO struct STRUCT_SYM_INFO

SYM_INFO* createSymbol(char* name);
SYM_INFO* check_symbol(char* name);

%}

%union {
	char* name;
	int value;
	SYM_INFO* sym;
}



%type	<name>	NAME
%type	<value>	NUMBER	factor	term exp
%type	<sym>	var


%token ADD MINUS TIMES DIVIDE LBRACK RBRACK NUMBER 
%token NAME SEMICOLON ASSIGN

%start		exps

%%

exps		: statement SEMICOLON
		| statement SEMICOLON exps
		;

statement		: var ASSIGN exp	{ 
					if($1->isAssigned){
						printf("Reassign a declared var.\n");
					}
					else{
						printf("Declare a new var.\n");
					}
					printf("assigned var: %s,  value: %d\n", $1->name, $3);
					$1->value = $3;
					$1->isAssigned = 1;
				}
				| exp
				;

var		: NAME { $$ = check_symbol($1); }
		;

exp		: exp ADD term {$$ = $1 + $3; }
		| exp MINUS term {$$ = $1 - $3; }
		| term { $$ = $1;}
		;


term		: term TIMES factor {$$ = $1 * $3;}
		| term DIVIDE factor {$$ = $1 / $3;}
		| factor {$$ = $1;}
		;


factor		: LBRACK exp RBRACK { $$ = $2; }
			| NUMBER  { $$ = $1; }
			| var {	if(!$1->isAssigned){
					printf("Use an undeclared value in line %d.\n", yylineno);
					exit(1);
				}
				$$ = $1->value;}
			;



%%

#define SYMBOL_CHART_LIMIT 1000
SYM_INFO* symbol_chart[SYMBOL_CHART_LIMIT];
int symbol_cnt = 0;

SYM_INFO* createSymbol(char* name) {
	if (symbol_cnt == SYMBOL_CHART_LIMIT) {
		printf("Error occurred in line %d, symbol chart reached limit.\n", yylineno);
	}
	SYM_INFO* sym_info = (SYM_INFO*) malloc(sizeof(SYM_INFO));
	sym_info->name = strdup(name);
	symbol_chart[symbol_cnt++] = sym_info;
	return sym_info;
}

// 定义var和使用var会用同样的产生式规约

// 检查符号是否存在在符号表里，暂不支持scope
// 存在则返回找到的引用，否则实例一个SYM_INFO
SYM_INFO* check_symbol(char* name) {
	for (int i = 0; i < symbol_cnt; ++i) {
		if (strcmp(name, symbol_chart[i]->name) == 0) {
			return symbol_chart[i];
		}
	}
	return createSymbol(name);
}

int main(int argc, char** argv)
{

	if (argc > 1)
	{
		FILE* file;
		file = fopen(argv[1], "r");
		if (file)
			yyin = file;
	}
	if (argc > 2)
	{
		FILE* file;
		file = fopen(argv[2], "w");
		if (file)
		{
			yyout = file;
			freopen(argv[2], "w", stdout);
		}
	}
	yyparse();
	// printGrammarTree(root, 0);
	outputGraphvizFile();
	// printf("\nResult: %d\n", root->val);
}
