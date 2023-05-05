%{

#include "y.tab.h"

%}


digit			[0-9]

%%

[ \t]	break;
{digit}+	{  yylval = atoi(yytext); 
	   return NUMBER; }
\+	{return ADD;}
\-	{return MINUS;}
\/	{return DIVIDE;}
\*	{return TIMES;}
\(	{return LBRACK;}
\)	{return RBRACK;}


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

