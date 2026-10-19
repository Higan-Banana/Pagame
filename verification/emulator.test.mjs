// Runs against the official LOCAL Firestore emulator. Never connects to a real project.
import fs from 'node:fs';
import test from 'node:test';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, getDoc, getDocs, collection, query, where, writeBatch, serverTimestamp } from 'firebase/firestore';
const host=process.env.FIRESTORE_EMULATOR_HOST;
if(!host || !/^(localhost|127\.0\.0\.1):\d+$/.test(host))throw new Error('Run using npm run test:emulator. A local Firestore emulator is required.');
const [hostname,port]=host.split(':');
const environment=await initializeTestEnvironment({projectId:'demo-pagame-spark',firestore:{host:hostname,port:Number(port),rules:fs.readFileSync(new URL('../firestore.rules',import.meta.url),'utf8')}});
const cases=JSON.parse(fs.readFileSync(new URL('./rules-cases.json',import.meta.url),'utf8'));
try{
 await test('Official Firestore rules regression scenarios',async t=>{
  for(const [index,scenario]of cases.entries())await t.test(`scenario ${index+1}: ${scenario.allowed?'allow':'deny'}`,async()=>{
   await environment.clearFirestore();
   await environment.withSecurityRulesDisabled(async context=>{
    const db=context.firestore(),batch=writeBatch(db);for(const [path,data]of Object.entries(scenario.before))batch.set(doc(db,path),data);await batch.commit();
   });
   const db=(scenario.uid?environment.authenticatedContext(scenario.uid):environment.unauthenticatedContext()).firestore(),batch=writeBatch(db);
   for(const [path,raw]of Object.entries(scenario.writes)){
    if(raw===null){batch.delete(doc(db,path));continue}
    const value=structuredClone(raw);
    if(path.split('/').length===2&&(path.startsWith('profiles/')||path.startsWith('events/'))&&!scenario.before[path])value.createdAt=serverTimestamp();
    if(path.split('/').length===2&&path.startsWith('events/'))value.updatedAt=serverTimestamp();
    if(path.startsWith('friendRequests/')) {
     if(!scenario.before[path] && value.createdAt===1000)value.createdAt=serverTimestamp();
     if(value.updatedAt===1000)value.updatedAt=serverTimestamp();
    }
    batch.set(doc(db,path),value);
   }
   await (scenario.allowed?assertSucceeds(batch.commit()):assertFails(batch.commit()));
  });
 });
 await test('Profile queries and private information stay restricted',async()=>{
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async context=>{const db=context.firestore(),batch=writeBatch(db);batch.set(doc(db,'users/a'),{currency:'GTQ',theme:'SYSTEM',friendIds:[]});batch.set(doc(db,'events/e'),{participantIds:['a']});await batch.commit()});
  await assertFails(getDoc(doc(environment.authenticatedContext('b').firestore(),'users/a')));
  await assertFails(getDoc(doc(environment.authenticatedContext('b').firestore(),'events/e')));
  await assertFails(getDoc(doc(environment.unauthenticatedContext().firestore(),'profiles/a')));
 });
 await test('Friend requests are visible to the two participants only, including live-query shape',async()=>{
  await environment.clearFirestore();
  const request={senderId:'a',recipientId:'b',members:['a','b'],status:'pending',createdAt:1000,updatedAt:1000};
  await environment.withSecurityRulesDisabled(async context=>{
   const batch=writeBatch(context.firestore());batch.set(doc(context.firestore(),'friendRequests/a~b'),request);await batch.commit();
  });
  for(const uid of ['a','b']) {
   const db=environment.authenticatedContext(uid).firestore();
   await assertSucceeds(getDoc(doc(db,'friendRequests/a~b')));
   await assertSucceeds(getDoc(doc(db,uid==='a'?'friendRequests/a~c':'friendRequests/b~c')));
   await assertSucceeds(getDocs(query(collection(db,'friendRequests'),where('members','array-contains',uid))));
   await assertFails(getDocs(collection(db,'friendRequests')));
  }
  await assertFails(getDoc(doc(environment.authenticatedContext('c').firestore(),'friendRequests/a~b')));
  await assertFails(getDoc(doc(environment.unauthenticatedContext().firestore(),'friendRequests/a~b')));
 });
}finally{await environment.cleanup()}
