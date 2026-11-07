package com.oddfactory.renewatheater;

final class DisplayLease {
 private boolean connected;
 private Object owner;
 void connect(){connected=true;owner=null;}
 boolean ready(){return connected;}
 boolean claim(Object next){if(!connected||next==null||owner!=null&&owner!=next)return false;owner=next;return true;}
 boolean allows(Object candidate){return connected&&candidate!=null&&owner==candidate;}
 void release(Object candidate){if(owner==candidate)owner=null;}
 Object disconnect(){Object previous=owner;connected=false;owner=null;return previous;}
}
