grammar Pattern;

pattern : node;

// (you:Person {name:"You"})
node : '(' SP? ( variable SP? )? ( nodeLabels SP? )? ( properties SP? )? ')' ;



fragment SP : ( WHITESPACE )+ ;

fragment WHITESPACE : SPACE
           | TAB
           | LF
           ;

fragment SPACE : [ ] ;
fragment TAB : [\t] ;
fragment LF : [\n] ;