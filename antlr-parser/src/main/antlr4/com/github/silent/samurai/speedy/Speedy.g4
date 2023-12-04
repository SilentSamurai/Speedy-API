
grammar Speedy;


request : resource SLSH? speedyQueryImpl? frag? CRLF? ;

resource: SLSH IDENTIFIER filters? ;

frag: HASH (identifier | DIGITS);

speedyQueryImpl: QM search ;
search: searchParameter (AND_OP searchParameter)* ;
searchParameter : identifier (EQ (searchSV | searchMV))?  ;
searchSV: constValue;
searchMV: constList;

filters: PNTH_OP (arguments | keywords) PNTH_CL;

keywords: keywordsParams ( cndoptr keywordsParams ) * ;
keywordsParams : identifier assoIdenfier? ( paramSV | paramMV ) ;
paramSV: svoptr constValue;
paramMV: mvoptr constList;

arguments: argument (COMMA_OP argument )* ;
argument: (DIGITS | VALSTRING);

svoptr: (EQ | EEQ | NEQ | LT | GT | LTE | GTE );
mvoptr: (NOT_IN | IN );
cndoptr: ( OR_OP | COMMA_OP | AND_OP );


assoIdenfier: DOT identifier;
identifier: IDENTIFIER;
constList: BRC_OP constValue (COMMA_OP constValue)*  BRC_CL;
constValue: (DIGITS | VALSTRING);
//valString: VALSTRING ;

//string: STRING ;


AND_OP :'&';
COMMA_OP: ',';
OR_OP: '|';
CRLF : '\r' ? '\n' | '\r';

NEQ: '!=';
EQ: '=';
EEQ: '==';
LT: '<';
GT: '>';
LTE: '<=';
GTE: '>=';
IN: '<>';
NOT_IN : '<!>' ;

DIGITS : NUMBER+ ;
VALSTRING: QUOTES EXCHAR+ QUOTES;
IDENTIFIER: [a-zA-Z$] CHAR*;
STRING : CHAR+ ;
QUOTES : (SINGLEQUOTE | DOUBLEQUOTE) ;

QM: '?';
HASH: '#';
DOT: '.';


PNTH_OP: '(';
PNTH_CL: ')';
BRC_OP: '[';
BRC_CL: ']';
SLSH: '/';
fragment SINGLEQUOTE: '\'';
fragment DOUBLEQUOTE: '"';
fragment NUMBER : [0-9];
fragment CHAR: [a-zA-Z_0-9$];
fragment EXCHAR: [a-zA-Z_0-9+-.*$%?& :];
WS: [ \n\t\r]+ -> skip;