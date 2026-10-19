import test from 'node:test';
import fs from 'node:fs';
const capturedCases=[];
process.on('exit',()=>fs.writeFileSync(new URL('./rules-cases.json',import.meta.url),JSON.stringify(capturedCases)));
import assert from 'node:assert/strict';
import {RulesModel} from './rules-model.mjs';
import {initial,event,writes,update,apply,expense,rulesPath,profile,ledgers} from './fixtures.mjs';
const rules=new RulesModel(rulesPath);
const check=(before,writes,uid,allowed=true)=>{
 capturedCases.push({before:structuredClone(before),writes:structuredClone(writes),uid,allowed});
 const result=rules.authorize(before,writes,uid);
 assert.equal(result.allowed,allowed,JSON.stringify(result.reports,null,2));
 for (const r of result.reports) if (allowed) { assert.ok(r.expressions < 1000, `Local expression estimate: ${r.expressions}`); assert.ok(r.accesses <= 10, `Rule accesses: ${r.accesses}`); }
 return result;
};
const base=()=>({...initial(),...writes(event())});
const spent=()=>apply(base(),update(base(),'a','addExpense',expense));
const reported=()=>{const e=spent();return apply(e,update(e,'b','reportPayment',{paymentId:'p1',fromId:'b',toId:'a',amountMinor:33,revision:1}))};

test('events and expenses need no emoji and any member can add their own expense',()=>{
 const created=event();delete created.emoji;
 check(initial(),writes(created),'a');
 const docs={...initial(),...writes(created)};
 const w=update(docs,'b','addExpense',expense);
 delete w['events/e'].expenses.expense1.emoji;
 check(docs,w,'b');check(docs,w,'outsider',false);
 assert.equal(w['events/e'].expenses.expense1.createdBy,'b');
 const forged=structuredClone(w);forged['events/e'].expenses.expense1.createdBy='a';check(docs,forged,'b',false);
 const saved=apply(docs,w),edited=update(saved,'a','updateExpense',{...expense,title:'Cambio del organizador'});
 check(saved,edited,'a');assert.equal(edited['events/e'].expenses.expense1.createdBy,'b');
});

test('legacy emoji metadata does not prevent updates to existing events or expenses',()=>{
 const docs=base();docs['events/e'].emoji='legacy text';
 const w=update(docs,'b','addExpense',expense);w['events/e'].expenses.expense1.emoji='legacy text';check(docs,w,'b');
});

test('participant changes reject malformed or unrelated operation IDs',()=>{
 const docs=base();
 for(const id of [42,[],{},'unrelated-expense']) {
  const w=update(docs,'a','updateParticipants',{participantIds:['a','b']});
  w['events/e'].lastChange.id=id;
  check(docs,w,'a',false);
 }
});
test('profile, settings and unique code created atomically',()=>{
 const p=profile('a');check({}, {'profiles/a':p,['friendCodes/'+p.friendCode]:{uid:'a'},'users/a':{currency:'GTQ',theme:'SYSTEM',friendIds:[]}},'a');
});
test('cannot reserve a code that does not belong to the authenticated profile',()=>{
 const p=profile('a');check({}, {'profiles/a':p},'a',false);
 check(initial(),{'friendCodes/PG-9999':{uid:'b'}},'a',false);
});
test('only own name, color and preferences can change',()=>{
 const docs=initial();check(docs,{'profiles/a':{...docs['profiles/a'],colorIndex:7}},'a');
 check(docs,{'profiles/b':{...docs['profiles/b'],colorIndex:7}},'a',false);
 check(docs,{'profiles/a':{...docs['profiles/a'],name:'Changed'}},'a');
 check(docs,{'profiles/b':{...docs['profiles/b'],name:'Changed'}},'a',false);
 check(docs,{'profiles/a':{...docs['profiles/a'],name:''}},'a',false);
 check(docs,{'profiles/a':{...docs['profiles/a'],name:'x'.repeat(61)}},'a',false);
 check(docs,{'profiles/a':{...docs['profiles/a'],friendCode:'PG-9999'}},'a',false);
 check(docs,{'profiles/a':{...docs['profiles/a'],uid:'b'}},'a',false);
 check(docs,{'users/a':{...docs['users/a'],currency:'USD'}},'a');
 check(docs,{'users/b':{...docs['users/b'],theme:'DARK'}},'a',false);
});
test('unilateral friendship additions are no longer allowed',()=>{
 const docs=initial();docs['users/a'].friendIds=[];
 check(docs,{'users/a':{...docs['users/a'],friendIds:['b']}},'a',false);
 check(docs,{'users/a':{...docs['users/a'],friendIds:['unknown']}},'a',false);
});
test('creates event with ten consistent ledgers and rejects missing ledger',()=>{
 check(initial(),writes(event()),'a');const w=writes(event());delete w['events/e/ledger/5'];check(initial(),w,'a',false);
});
test('cannot add strangers or overwrite event ownership',()=>{
 check(initial(),writes(event(['a','unknown'])),'a',false);
 const docs=base(),w=update(docs,'a','addExpense',expense);w['events/e'].ownerId='b';check(docs,w,'a',false);
});
test('expense addition verifies exact shares and independent balances',()=>{
 const docs=base();check(docs,update(docs,'a','addExpense',expense),'a');
 const w=update(docs,'a','addExpense',expense);w['events/e/ledger/0'].cells[0].balance+=1;check(docs,w,'a',false);
});
test('forged rounding boundary cannot change the total cents',()=>{
 const docs=base(),w=update(docs,'a','addExpense',expense);w['events/e'].expenses.expense1.extraThrough='c';check(docs,w,'a',false);
});
test('cannot modify event while omitting ledger update',()=>{
 const docs=base(),w=update(docs,'a','addExpense',expense);delete w['events/e/ledger/0'];check(docs,w,'a',false);
});
test('expense edit/delete permitted to author or organizer only',()=>{
 const docs=spent();check(docs,update(docs,'a','updateExpense',{...expense,amountMinor:200}),'a');
 const w=update(docs,'a','deleteExpense',{expenseId:'expense1'});check(docs,w,'a');check(docs,w,'b',false);
});
test('empty event can be deleted by organizer only',()=>{
 const docs=base(),w=update(docs,'a','deleteEvent');check(docs,w,'a');check(docs,w,'b',false);
 const spentDocs=spent();check(spentDocs,w,'a',false);
});
test('participants without expenses may be removed, others cannot',()=>{
 const docs=base();check(docs,update(docs,'a','updateParticipants',{participantIds:['a','b']}),'a');
 const e=spent(),next={...e['events/e'],participantIds:['a','b'],memberSlots:['a','b',...Array(28).fill('')],revision:2,lastChange:{kind:'updateParticipants',id:''}};
 const w=writes(next);check(e,w,'a',false);
});
test('change expense participation then remove participant',()=>{
 const docs=spent(),w=update(docs,'a','updateExpense',{...expense,includedIds:['a','b']});check(docs,w,'a');
 const next=apply(docs,w);check(next,update(next,'a','updateParticipants',{participantIds:['a','b']}),'a');
});
test('reporting payment changes no balances; only debtor can report',()=>{
 const docs=spent(),w=update(docs,'b','reportPayment',{paymentId:'p1',fromId:'b',toId:'a',amountMinor:33,revision:1});check(docs,w,'b');check(docs,w,'a',false);
 assert.equal(w['events/e/ledger/0'].cells[1].balance,-33);
});
test('forged payment amount is rejected even with recalculated ledgers',()=>{
 const docs=spent(),w=update(docs,'b','reportPayment',{paymentId:'p1',fromId:'b',toId:'a',amountMinor:33,revision:1});
 w['events/e'].pendingPayment.amountMinor=1;Object.assign(w,ledgers(w['events/e']));check(docs,w,'b',false);
});
test('only receiver can confirm or reject payment',()=>{
 const docs=reported();for(const kind of ['confirmPayment','rejectPayment']){
 const w=update(docs,'a',kind,{paymentId:'p1'});check(docs,w,'a');check(docs,w,'b',false);}
});
test('confirmed payment history cannot be rewritten',()=>{
 const docs=reported(),w=update(docs,'a','confirmPayment',{paymentId:'p1'});w['events/e'].payments.p1.amountMinor=50;Object.assign(w,ledgers(w['events/e']));check(docs,w,'a',false);
});
test('editing an expense preserves the reported payment until receiver resolution',()=>{
 const docs=reported(),w=update(docs,'a','updateExpense',{...expense,amountMinor:300});check(docs,w,'a');assert.deepEqual(w['events/e'].pendingPayment,docs['events/e'].pendingPayment);
});
test('concurrent stale revision rejected',()=>{
 const docs=spent(),w=update(docs,'a','updateExpense',{...expense,amountMinor:300});w['events/e'].revision=1;check(docs,w,'a',false);
});
test('30 participants: create, split, edit and payment span ledger shards',()=>{
 const people=Array.from({length:30},(_,i)=>'u'+String(i).padStart(2,'0')),owner=people[0];
 const initialDocs=initial(people),created=writes(event(people));check(initialDocs,created,owner);
 let docs=apply(initialDocs,created);const input={...expense,payerId:owner,includedIds:people,amountMinor:100};
 let w=update(docs,owner,'addExpense',input);const r=check(docs,w,owner);
 console.log('30-person local model maximum expressions per write:',Math.max(...r.reports.map(x=>x.expressions)));
 docs=apply(docs,w);w=update(docs,owner,'updateExpense',{...input,amountMinor:600});check(docs,w,owner);
 docs=apply(docs,w);w=update(docs,people[29],'reportPayment',{paymentId:'p30',fromId:people[29],toId:owner,amountMinor:20,revision:2});check(docs,w,people[29]);
 docs=apply(docs,w);check(docs,update(docs,owner,'confirmPayment',{paymentId:'p30'}),owner);
});
test('cannot break the atomic ledger ring at any shard',()=>{
 const docs=base();for(let i=0;i<10;i++){const w=update(docs,'a','addExpense',expense);delete w['events/e/ledger/'+i];check(docs,w,'a',false);}
});
test('forged balances and forged prefixes are both rejected',()=>{
 const docs=base(),w=update(docs,'a','addExpense',expense);w['events/e/ledger/9'].balancePrefix=5;check(docs,w,'a',false);
});
test('every small remainder preserves the sum with thirty members',()=>{
 const people=Array.from({length:30},(_,i)=>'u'+String(i).padStart(2,'0')),owner=people[0],docs={...initial(people),...writes(event(people))};
 for(let amount=1;amount<=31;amount++)check(docs,update(docs,owner,'addExpense',{...expense,payerId:owner,includedIds:people,amountMinor:amount}),owner);
});
test('deletion removes private settings and code but preserves an anonymous identity',()=>{
 const docs=initial(),p=docs['profiles/a'];check(docs,{'profiles/a':{...p,name:'Usuario eliminado',colorIndex:0,deleted:true},'users/a':null,['friendCodes/'+p.friendCode]:null},'a');
 check(docs,{'profiles/b':{...docs['profiles/b'],name:'Usuario eliminado',colorIndex:0,deleted:true}},'a',false);
});



test('two members settle 23.45: receipt clears pending and both ledgers reach zero',()=>{
 const before={...initial(['a','b']),...writes(event(['a','b']))};
 const add=update(before,'a','addExpense',{...expense,amountMinor:2345,includedIds:['a','b']});
 check(before,add,'a');
 const spent=apply(before,add);
 const report=update(spent,'b','reportPayment',{paymentId:'paid',fromId:'b',toId:'a',amountMinor:1172,revision:1});
 check(spent,report,'b');
 const pending=apply(spent,report);
 const confirmation=update(pending,'a','confirmPayment',{paymentId:'paid'});
 check(pending,confirmation,'a');
 const result=confirmation['events/e'];
 assert.equal(result.pendingPayment,null);
 assert.equal(result.payments.paid.status,'confirmed');
 assert.equal(Object.keys(result.expenses).length,1);
 for(const [key,doc]of Object.entries(confirmation))if(key.includes('/ledger/'))for(const cell of doc.cells)assert.equal(cell.balance,0);
 check(pending,confirmation,'b',false);
 // A repeat cannot turn a confirmed payment into another payment.
 const settled=apply(pending,confirmation);
 assert.throws(()=>update(settled,'a','confirmPayment',{paymentId:'paid'}));
});

const friendBase=()=>{const d=initial();d['users/a'].friendIds=[];d['users/b'].friendIds=[];return d;};
const invitation=(sender='a',recipient='b')=>({senderId:sender,recipientId:recipient,members:[sender,recipient],status:'pending',createdAt:1000,updatedAt:1000});
const invited=()=>({...friendBase(),'friendRequests/a~b':invitation()});
const acceptance=d=>({'friendRequests/a~b':{...d['friendRequests/a~b'],status:'accepted'},
 'users/a':{...d['users/a'],friendIds:[...new Set([...d['users/a'].friendIds,'b'])]},
 'users/b':{...d['users/b'],friendIds:[...new Set([...d['users/b'].friendIds,'a'])]}});
test('request is created only by its sender and cannot forge accepted friendship',()=>{
 const d=friendBase(),w={'friendRequests/a~b':invitation()};check(d,w,'a');check(d,w,'b',false);check(d,w,'c',false);
 check(d,{'friendRequests/a~b':{...invitation(),status:'accepted'}},'a',false);
 check(d,{'friendRequests/b~a':invitation()},'a',false);
 check(d,{'friendRequests/a~a':invitation('a','a')},'a',false);
 check(d,{'friendRequests/a~b':{...invitation(),members:['a','b','c']}},'a',false);
 check(d,{'friendRequests/a~b':{...invitation(),createdAt:1}},'a',false);
});
test('one recipient acceptance adds BOTH friends atomically',()=>{
 const d=invited(),w=acceptance(d);check(d,w,'b');check(d,w,'a',false);check(d,w,'c',false);
 const missingA={...w};delete missingA['users/a'];check(d,missingA,'b',false);
 const missingB={...w};delete missingB['users/b'];check(d,missingB,'b',false);
 const missingRequest={...w};delete missingRequest['friendRequests/a~b'];check(d,missingRequest,'b',false);
 const next=apply(d,w);assert.deepEqual(next['users/a'].friendIds,['b']);assert.deepEqual(next['users/b'].friendIds,['a']);
});
test('friendship acceptance cannot change other private settings or add extra people',()=>{
 const d=invited(),w=acceptance(d);w['users/a'].currency='USD';check(d,w,'b',false);
 const extra=acceptance(d);extra['users/a'].friendIds.push('c');check(d,extra,'b',false);
 const remove=invited();remove['users/a'].friendIds=['c'];const r=acceptance(remove);r['users/a'].friendIds=['b'];check(remove,r,'b',false);
});
test('legacy one-sided friendships become mutual with explicit recipient acceptance',()=>{
 for(const side of ['a','b','both']){
  const d=invited();if(side==='a'||side==='both')d['users/a'].friendIds=['b'];if(side==='b'||side==='both')d['users/b'].friendIds=['a'];
  check(d,acceptance(d),'b');
 }
});
test('reject and cancel only pending own requests; no unsolicited friendship deletion',()=>{
 const d=invited();check(d,{'friendRequests/a~b':null},'a');check(d,{'friendRequests/a~b':null},'b');check(d,{'friendRequests/a~b':null},'c',false);
 const accepted=apply(d,acceptance(d));check(accepted,{'friendRequests/a~b':null},'b',false);
 check(accepted,{'users/a':{...accepted['users/a'],friendIds:[]}},'a',false);
 check(accepted,acceptance(d),'b',false);
});
test('crossed and repeated requests cannot overwrite existing relationship',()=>{
 const d=invited();check(d,{'friendRequests/b~a':invitation('b','a')},'b',false);
 check(d,{'friendRequests/a~b':invitation()},'a',false);
 const empty=friendBase();check(empty,{'friendRequests/a~b':invitation(),'friendRequests/b~a':invitation('b','a')},'a',false);
});
test('requests reject deleted accounts and respect friendship limits',()=>{
 const d=friendBase();d['profiles/b'].deleted=true;check(d,{'friendRequests/a~b':invitation()},'a',false);
 const pending=invited();pending['profiles/a'].deleted=true;check(pending,acceptance(pending),'b',false);
 check(pending,{'friendRequests/a~b':null},'b');
 const full=invited();full['users/a'].friendIds=Array.from({length:300},(_,i)=>'other'+i);check(full,acceptance(full),'b',false);
});
test('accepted friend can be invited into an event; pending request grants no such access',()=>{
 const d=invited();check(d,writes(event(['a','b'])),'a',false);
 const accepted=apply(d,acceptance(d));check(accepted,writes(event(['a','b'])),'a');
});

const removal=d=>({'users/a':{...d['users/a'],friendIds:d['users/a'].friendIds.filter(x=>x!=='b')},
 'users/b':{...d['users/b'],friendIds:d['users/b'].friendIds.filter(x=>x!=='a')},'friendRequests/a~b':null});
test('either friend may remove the mutual relation but cannot edit preferences or unrelated friends',()=>{
 const d=apply(invited(),acceptance(invited()));const w=removal(d);check(d,w,'a');check(d,w,'b');check(d,w,'c',false);
 const bad=removal(d);bad['users/b'].currency='EUR';check(d,bad,'a',false);
 const one=removal(d);delete one['users/b'];check(d,one,'a',false);
 const keepRequest=removal(d);delete keepRequest['friendRequests/a~b'];check(d,keepRequest,'a',false);
 const alongside={...d,...writes(event(['a','b']))};check(alongside,w,'a');
 assert.deepEqual(apply(alongside,w)['events/e'],alongside['events/e']);
});
test('legacy unilateral and bilateral friendships can be removed without creating requests',()=>{
 for(const side of ['a','both']) {
  const d=friendBase();d['users/a'].friendIds=['b'];if(side==='both')d['users/b'].friendIds=['a'];
  const w=removal(d);delete w['friendRequests/a~b'];check(d,w,'a');
 }
 const deleted=friendBase();deleted['users/a'].friendIds=['b'];deleted['profiles/b'].deleted=true;delete deleted['users/b'];
 check(deleted,{'users/a':{...deleted['users/a'],friendIds:[]}},'a');
});
test('friend removal cancels legacy pending request and permits a fresh invitation later',()=>{
 const d=invited();d['users/a'].friendIds=['b'];const w=removal(d);check(d,w,'a');
 const removed=apply(d,w);check(removed,{'friendRequests/a~b':invitation()},'a');
});
test('removing and restoring expense participation never erases confirmed or reported payments',()=>{
 const d={...initial(['a','b']),...writes(event(['a','b']))};
 const bill={expenseId:'expense1',title:'Agua',amountMinor:2345,payerId:'a',includedIds:['a','b']};
 const spent=apply(d,update(d,'a','addExpense',bill));
 const pendingWrites=update(spent,'b','reportPayment',{paymentId:'p',fromId:'b',toId:'a',amountMinor:1172,revision:1});
 const pending=apply(spent,pendingWrites);
 for(const confirmFirst of [false,true]) {
  let state=pending;
  if(confirmFirst){const w=update(state,'a','confirmPayment',{paymentId:'p'});check(state,w,'a');state=apply(state,w);}
  const removed=update(state,'a','updateExpense',{...bill,includedIds:['a']});check(state,removed,'a');state=apply(state,removed);
  const restored=update(state,'a','updateExpense',bill);check(state,restored,'a');state=apply(state,restored);
  if(!confirmFirst){assert.equal(state['events/e'].pendingPayment.id,'p');const w=update(state,'a','confirmPayment',{paymentId:'p'});check(state,w,'a');state=apply(state,w);}
  assert.equal(state['events/e/ledger/0'].cells[0].balance,0);assert.equal(state['events/e/ledger/0'].cells[1].balance,0);
  assert.equal(Object.keys(state['events/e'].payments).length,1);
 }
 const forged=update(pending,'a','updateExpense',bill);forged['events/e'].pendingPayment=null;Object.assign(forged,ledgers(forged['events/e']));check(pending,forged,'a',false);
});




test('both invitation directions: either party deletes within even uncached rule access limits',()=>{
 for (const sender of ['a','b']) for (const actor of ['a','b']) {
  const receiver=sender==='a'?'b':'a',id=sender+'~'+receiver;
  const d=friendBase();d['users/a'].friendIds=['b'];d['users/b'].friendIds=['a'];
  d['friendRequests/'+id]={...invitation(sender,receiver),status:'accepted'};
  const w={'users/a':{...d['users/a'],friendIds:[]},'users/b':{...d['users/b'],friendIds:[]},['friendRequests/'+id]:null};
  // Match actual transaction write order: caller, counterpart, then request.
  const other=actor==='a'?'b':'a';
  const ordered={['users/'+actor]:w['users/'+actor],['users/'+other]:w['users/'+other],['friendRequests/'+id]:null};
  const result=check(d,ordered,actor);
  for(const report of result.reports)assert.ok(report.accessCalls<=10,JSON.stringify(report));
  const total=result.reports.reduce((sum,r)=>sum+r.accessCalls,0);
  assert.ok(total<=20,`Uncached batch calls: ${total}`);
  console.log(`request ${id}, delete by ${actor}: ${total} uncached accesses`);
 }
});
