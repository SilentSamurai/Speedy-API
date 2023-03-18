
grammar Speedy;


request : SLSH resource filters? SLSH? query? frag? CRLF? ;

filters: PNTH_OP (arguments | keywords) PNTH_CL;

keywords: keywordsParams (FILTER_SEP keywordsParams ) * ;
keywordsParams : paramKey EQ paramValue  ;
paramValue: valString;
paramKey: identifier ;

arguments: argument (FILTER_SEP argument )* ;
argument: valString;

resource: identifier ;

frag: HASH (string | DIGITS) ;

query: QM search ;
search: searchParameter (AND_OP searchParameter)* ;
searchParameter : identifier (EQ valString )? ;

valString: VALSTRING ;
identifier: IDENTIFIER;
string: STRING ;



FILTER_SEP: ( AND_OP | ',' | '|' );
AND_OP :'&';
CRLF : '\r' ? '\n' | '\r';

VALSTRING: QUOTES EXCHAR+ QUOTES;
IDENTIFIER: [a-zA-Z] CHAR*;
STRING : CHAR+ ;
DIGITS : NUMBER+ ;
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
fragment CHAR: [a-zA-Z_0-9];
fragment EXCHAR: [a-zA-Z_0-9+-.*$%?];