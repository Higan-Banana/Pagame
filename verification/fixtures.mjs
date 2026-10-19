import domain from './reference-domain.cjs';
export const rulesPath=new URL('../firestore.rules',import.meta.url);
export function profile(uid){return {uid,name:'Persona '+uid,friendCode:'PG-'+(1000+uid.charCodeAt(0)),colorIndex:2,createdAt:1000,deleted:false}}
export function initial(people=['a','b','c']){
 const docs={};for(const uid of people){docs['profiles/'+uid]=profile(uid);docs['users/'+uid]={currency:'GTQ',theme:'SYSTEM',friendIds:people.filter(id=>id!==uid)};docs['friendCodes/'+profile(uid).friendCode]={uid}}
 return docs;
}
export function event(people=['a','b','c']){return {schemaVersion:2,title:'Cena',ownerId:people[0],participantIds:people,
 memberSlots:[...people,...Array(30-people.length).fill('')],currency:'GTQ',emoji:'🍽️',expenses:{},payments:{},pendingPayment:null,
 revision:0,createdAt:1000,updatedAt:1000,lastChange:{kind:'createEvent',id:''}}}
export function legacy(e){return {...e,expenses:Object.values(e.expenses),payments:[...Object.values(e.payments),...(e.pendingPayment?[e.pendingPayment]:[])]}}
export function ledgers(e){
 const old=legacy(e),balance=domain.balances(old),docs={};let prefix=0;
 for(let shard=0;shard<10;shard++){prefix+=e.memberSlots.slice(shard*3,shard*3+3).reduce((sum,id)=>sum+(balance[id]??0),0);docs['events/e/ledger/'+shard]={revision:e.revision,balancePrefix:prefix,cells:e.memberSlots.slice(shard*3,shard*3+3).map(uid=>({uid,balance:balance[uid]??0,
 expenseCount:old.expenses.filter(x=>x.payerId===uid||x.includedIds.includes(uid)).length,
 paymentCount:old.payments.filter(x=>x.fromId===uid||x.toId===uid).length}))};
 }return docs;
}
export function writes(e){return {'events/e':e,...ledgers(e)}}
export function update(before,actor,kind,input={}){
 const old=before['events/e'],next=domain.changeEvent(legacy(old),actor,kind,input,1000);
 if(next===null)return Object.fromEntries(['events/e',...Array.from({length:10},(_,i)=>'events/e/ledger/'+i)].map(k=>[k,null]));
 const slots=old.memberSlots.map(id=>next.participantIds.includes(id)?id:'');
 for(const id of next.participantIds)if(!slots.includes(id))slots[slots.indexOf('')]=id;
 const payment=p=>({...p,fromSlot:slots.indexOf(p.fromId),toSlot:slots.indexOf(p.toId)});
 return writes({...old,...next,memberSlots:slots,expenses:Object.fromEntries(next.expenses.map(x=>[x.id,{...x,extraThrough:x.amountMinor%x.includedIds.length===0?'':x.includedIds[x.amountMinor%x.includedIds.length-1]}])),
 payments:Object.fromEntries(next.payments.filter(p=>p.status==='confirmed').map(p=>[p.id,payment(p)])),
 pendingPayment:next.payments.find(p=>p.status==='pending')?payment(next.payments.find(p=>p.status==='pending')):null,
 lastChange:{kind,id:input.expenseId??input.paymentId??''}});
}
export function apply(before,w){const n={...before};for(const [k,v]of Object.entries(w)){if(v===null)delete n[k];else n[k]=v}return n}
export const expense={expenseId:'expense1',title:'Cena',amountMinor:100,payerId:'a',includedIds:['a','b','c']};
