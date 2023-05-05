%{

// calculator

%}




%token ADD MINUS TIMES DIVIDE LBRACK RBRACK NUMBER 

%start		exp

%%


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
		;



%%

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
	printf("\nResult: %d\n", root->val);
}
