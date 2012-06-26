grammar Crontab;

options {
  language = Java;
  superClass = BaseParser;
}

@rulecatch { } 

@header {
  package hudson.scheduler;
}

@lexer::header {
  package hudson.scheduler;
}

startRule [CronTab table] throws RecognitionException
  : m=expr[0] WS h=expr[1] WS d=expr[2] WS mnth=expr[3] WS dow=expr[4] EOF
  {
    table.bits[0]=m;
    table.bits[1]=h;
    table.bits[2]=d;
    table.bits[3]=mnth;
    table.dayOfWeek=(int)dow;
  }
  | ( AT
      (
        YEARLY
      {
        table.set("0 0 1 1 *");
      }
      | ANNUALLY
      {
        table.set("0 0 1 1 *");
      }
      | MONTHLY
      {
        table.set("0 0 1 * *");
      }
      | WEEKLY
      {
        table.set("0 0 * * 0");
      }
      | DAILY
      {
        table.set("0 0 * * *");
      }
      | MIDNIGHT
      {
        table.set("0 0 * * *");
      }
      | HOURLY
      {
        table.set("0 * * * *");
      }
    )
  )
  ;

expr [int field] returns [long bits=0] throws RecognitionException
  : lhs=term[field] (',' rhs=expr[field])?
  {
    bits = lhs|rhs;
  }
  ;

term [int field] returns [long bits=0] throws RecognitionException
@init {
  d=1;
}
  : (token '-')=> s=token '-' e=token ( '/' d=token )?
  {
    bits = doRange(s,e,d,field);
  }
  | t=token
  {
    rangeCheck(t,field);
    bits = 1L<<t;
  }
  | '*' ('/' d=token )?
  {
    bits = doRange(d,field);
  }
  ;

token returns [int value=0]
  : t=TOKEN
  {
    value = Integer.parseInt(t.getText());
  }
  ;

TOKEN
  : ('0'..'9')+
  ;

WS
  : (' ' | '\t')+
  ;

MINUS:  '-';
STAR: '*';
DIV:  '/';
OR:   ',';
AT:   '@';

YEARLY: 'yearly';
ANNUALLY: 'annually';
MONTHLY: 'monthly';
WEEKLY: 'weekly';
DAILY: 'daily';
MIDNIGHT: 'midnight';
HOURLY: 'hourly';
