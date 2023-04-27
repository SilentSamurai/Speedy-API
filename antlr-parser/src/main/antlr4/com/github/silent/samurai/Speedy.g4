
grammar Speedy;


request : SLSH resource filters? SLSH? query? frag? CRLF? ;

frag: HASH (identifier | DIGITS) ;

query: QM search ;
search: searchParameter (AND_OP searchParameter)* ;
searchParameter : identifier (EQ paramValue)?  ;


filters: PNTH_OP (arguments | keywords) PNTH_CL;

keywords: keywordsParams ( ( OR_OP | COMMA_OP | AND_OP ) keywordsParams ) * ;
keywordsParams : paramKey EQ paramValue  ;
paramValue: (valString | DIGITS);
paramKey: identifier ;

arguments: argument (( OR_OP | COMMA_OP | AND_OP ) argument )* ;
argument: valString;

resource: identifier ;


valString: VALSTRING ;
identifier: IDENTIFIER;
string: STRING ;
//digits: DIGITS;



AND_OP :'&';
COMMA_OP: ',';
OR_OP: '|';
CRLF : '\r' ? '\n' | '\r';

DIGITS : NUMBER+ ;
VALSTRING: QUOTES EXCHAR+ QUOTES;
IDENTIFIER: [a-zA-Z$] CHAR*;
STRING : CHAR+ ;
QUOTES : (SINGLEQUOTE | DOUBLEQUOTE) ;

QM: '?';
HASH: '#';
EQ: '=';
PNTH_OP: '(';
PNTH_CL: ')';
SLSH: '/';
fragment SINGLEQUOTE: '\'';
fragment DOUBLEQUOTE: '"';
fragment NUMBER : [0-9];
fragment CHAR: [a-zA-Z_0-9$];
fragment EXCHAR: [a-zA-Z_0-9+-.*$%?& ];
WS: [ \n\t\r]+ -> skip;