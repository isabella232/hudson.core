grammar LabelExpression;

options { ASTLabelType=Label; }

@rulecatch { }

@header {
  package hudson.model.labels;
  import hudson.model.Label;
}

@parser::members {
  protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow) throws RecognitionException {
    throw new MismatchedTokenException(ttype, input);
  }
}

@lexer::header {
  package hudson.model.labels;
  import hudson.model.Label;
}

expr
returns [Label l]
  : t=term1 EOF { l = t; }
  ;

term1
returns [Label l]
  : t=term2  ( IFF r=term2 { t=t.iff(r); } )? { l=t; } 
  ;

term2
returns [Label l]
  : t=term3 ( IMPLIES r=term3 {t=t.implies(r);} )?  { l=t; }
  ;

term3
returns [Label l]
  : t=term4 ( OR r=term4 {t=t.or(r);} )?  { l=t; }
  ;

term4
returns [Label l]
  : t=term5 ( AND r=term5 {t=t.and(r);} )? { l=t; }
  ;

term5
returns [Label l]
  : t=term6 { l=t; }
  | NOT x=term6
    { l=x.not(); }
  ;

term6
returns [Label l]
  : LPAREN t=term1 RPAREN
    { l=t.paren(); }
  | a=ATOM
    { l=LabelAtom.get(a.getText()); }
  | s=STRINGLITERAL
    { l=LabelAtom.get(hudson.util.QuotedStringTokenizer.unquote(s.getText())); }
  ;

AND:    '&&';
OR:     '||';
NOT:    '!';
IMPLIES:'->';
IFF:    '<->';
LPAREN: '(';
RPAREN: ')';

fragment
IDENTIFIER_PART
    :   ~( '&' | '|' | '!' | '<' | '>' | '(' | ')' | ' ' | '\t' | '\"' | '\'' )
    ;

ATOM
/* the real check of valid identifier happens in LabelAtom.get() */
    :   (IDENTIFIER_PART)+
    ;

WS
  : (' '|'\t')+
    { $channel=HIDDEN; }
  ;

STRINGLITERAL
    :   '"'
        ( '\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\"' | '\'' | '\\' )   /* escape */
        |  ~( '\\' | '"' | '\r' | '\n' )
        )*
        '"'
    ;
