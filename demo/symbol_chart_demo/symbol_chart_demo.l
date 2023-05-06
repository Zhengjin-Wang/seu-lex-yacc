%{

#include "y.tab.h"

%}


digit			[0-9]
alpha			[a-zA-Z]

%%

[ \t\n\r]	break;
{digit}+	{  yylval.value = atoi(yytext); 
	   return NUMBER; }
{alpha}+({alpha}|{digit})*	{yylval.name = strdup(yytext);
	return NAME;}
\+	{return ADD;}
\-	{return MINUS;}
\/	{return DIVIDE;}
\*	{return TIMES;}
\(	{return LBRACK;}
\)	{return RBRACK;}
";"	{return SEMICOLON;}
"="	{return ASSIGN;}



%%

//int main(int argc, char** argv)
//{
//
//    if (argc > 1)
//    {
//        FILE* file;
//        file = fopen(argv[1], "r");
//        if (file)
//            yyin = file;
//    }
//    if (argc > 2)
//    {
//        FILE* file;
//        file = fopen(argv[2], "w");
//        if (file)
//        {
//            yyout = file;
//            freopen(argv[2], "w", stdout);
//        }
//    }
//    while (yylex() != 0);
//    yywrap();
//}


int
yywrap()
{
return 1; /* tell scanner no further files need to be processed */
}

